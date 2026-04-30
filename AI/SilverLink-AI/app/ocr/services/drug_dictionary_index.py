"""In-memory drug dictionary index for OCR candidate lookup.

The index is a fast lookup layer only. MySQL remains the source of truth and is
used to build or refresh this structure.
"""
from __future__ import annotations

import re
import threading
import time
import unicodedata
from dataclasses import dataclass, field
from typing import Any, Dict, Iterable, List, Optional, Set

from loguru import logger
from rapidfuzz import fuzz

from app.ocr.model.drug_model import DrugInfo, MatchCandidate, MatchResult


def normalize_dictionary_key(text: Optional[str]) -> str:
    """Normalize drug names and aliases for dictionary lookup."""
    if not text:
        return ""
    value = unicodedata.normalize("NFKC", str(text))
    value = value.replace("\u00b5", "\u03bc")
    value = value.lower()
    value = re.sub(r"[^\w\uac00-\ud7a3a-z0-9\s.\-/%()]", " ", value)
    value = re.sub(r"\s+", " ", value).strip()
    return value


def compact_dictionary_key(text: Optional[str]) -> str:
    return normalize_dictionary_key(text).replace(" ", "")


@dataclass(frozen=True)
class DrugSummary:
    item_seq: str
    item_name: str
    normalized_item_name: str = ""
    ingredient: Optional[str] = None
    strength: Optional[str] = None
    unit: Optional[str] = None
    dosage_form: Optional[str] = None
    manufacturer: Optional[str] = None
    source_fields: Dict[str, Any] = field(default_factory=dict)

    @classmethod
    def from_drug_info(cls, drug: DrugInfo) -> "DrugSummary":
        return cls(
            item_seq=drug.item_seq,
            item_name=drug.item_name,
            normalized_item_name=drug.item_name_normalized or drug.item_name,
            manufacturer=drug.entp_name,
            source_fields=drug.model_dump(),
        )

    def to_drug_info(self) -> DrugInfo:
        fields = dict(self.source_fields)
        fields.setdefault("item_seq", self.item_seq)
        fields.setdefault("item_name", self.item_name)
        fields.setdefault("item_name_normalized", self.normalized_item_name)
        fields.setdefault("entp_name", self.manufacturer)
        return DrugInfo(**fields)


@dataclass
class AliasSummary:
    item_seq: str
    text: str
    normalized_text: str = ""
    evidence: Dict[str, Any] = field(default_factory=dict)
    score: float = 0.98


class DrugDictionaryIndex:
    """Local memory index for exact, alias, error alias, ngram and limited fuzzy lookup.

    TODO (Phase 4-B / 향후):
        - reload_dictionary_index(): 운영 중 인덱스 갱신 (atomic swap 완료, 트리거 미구현)
        - prefix lookup: sorted list 또는 prefix_map 기반 전방 일치 (현재 ngram이 대체)
    """

    NGRAM_SIZE = 2
    DEFAULT_LIMIT = 10
    FUZZY_POOL_LIMIT = 50
    _load_lock = threading.Lock()

    def __init__(self, drug_repository=None):
        self.repo = drug_repository
        self._loaded = False
        self._load_failed = False
        self.loaded_at: Optional[float] = None

        # _current holds the fully-built index reference.
        # match() reads from _current so that a partial swap never occurs.
        self._current: Optional[DrugDictionaryIndex] = None
        self.last_reload_stats: Dict[str, Any] = {}

        self.exact_map: Dict[str, List[str]] = {}
        self.alias_map: Dict[str, List[AliasSummary]] = {}
        self.error_alias_map: Dict[str, List[AliasSummary]] = {}
        self.ngram_index: Dict[str, Set[str]] = {}
        self.drug_summary_map: Dict[str, DrugSummary] = {}

    @property
    def loaded(self) -> bool:
        return self._loaded and (self._current is not None or bool(self.drug_summary_map))

    @property
    def load_failed(self) -> bool:
        return self._load_failed

    def ensure_loaded(self) -> bool:
        if self.loaded:
            return True
        if self.repo is None:
            self._load_failed = True
            return False

        with self._load_lock:
            # Double-check after acquiring lock
            if self.loaded:
                return True

            started = time.perf_counter()
            try:
                medications = self.repo.fetch_all_medications_for_index()
                aliases = self.repo.fetch_all_aliases_for_index()
                error_aliases = self.repo.fetch_all_error_aliases_for_index()
                new_index = self.build(medications, aliases, error_aliases)
                # Atomic swap: assign fully-built index in one reference update
                self._current = new_index
                self.exact_map = new_index.exact_map
                self.alias_map = new_index.alias_map
                self.error_alias_map = new_index.error_alias_map
                self.ngram_index = new_index.ngram_index
                self.drug_summary_map = new_index.drug_summary_map
                self.loaded_at = time.time()
                self._loaded = True
                self._load_failed = False
                elapsed_ms = (time.perf_counter() - started) * 1000
                self.last_reload_stats = self._stats_from_index(
                    new_index,
                    success=True,
                    elapsed_ms=elapsed_ms,
                    message="Dictionary index loaded.",
                )
                logger.info(
                    "Drug dictionary index loaded: drugs={}, aliases={}, error_aliases={}, ngram_tokens={}, elapsed_ms={:.1f}",
                    len(new_index.drug_summary_map),
                    sum(len(v) for v in new_index.alias_map.values()),
                    sum(len(v) for v in new_index.error_alias_map.values()),
                    len(new_index.ngram_index),
                    elapsed_ms,
                )
                return self.loaded
            except Exception as exc:
                self._load_failed = True
                self.last_reload_stats = {
                    "success": False,
                    "message": f"Dictionary index load failed: {exc}",
                    "existing_index_kept": self.loaded,
                }
                logger.warning("Drug dictionary index load failed; using MySQL fallback: {}", exc)
                return False

    def reload(self) -> bool:
        """
        Force reload dictionary from DB.

        Keeps the existing index if reload fails.

        TODO:
        In multi-worker deployment, this reload only affects the current worker.
        Use rolling restart, dictionary_version polling, or pub/sub for production-wide reload.
        """
        try:
            if self.repo is None:
                logger.warning("Cannot reload: drug_repository is not configured.")
                self.last_reload_stats = {
                    "success": False,
                    "message": "drug_repository is not configured.",
                    "existing_index_kept": self.loaded,
                }
                return False

            with self._load_lock:
                started = time.perf_counter()
                medications = self.repo.fetch_all_medications_for_index()
                aliases = self.repo.fetch_all_aliases_for_index()
                error_aliases = self.repo.fetch_all_error_aliases_for_index()
                new_index = self.build(medications, aliases, error_aliases)
                
                # Atomic swap: assign fully-built index in one reference update
                self._current = new_index
                self.exact_map = new_index.exact_map
                self.alias_map = new_index.alias_map
                self.error_alias_map = new_index.error_alias_map
                self.ngram_index = new_index.ngram_index
                self.drug_summary_map = new_index.drug_summary_map
                self.loaded_at = time.time()
                self._loaded = True
                self._load_failed = False
                elapsed_ms = (time.perf_counter() - started) * 1000
                self.last_reload_stats = self._stats_from_index(
                    new_index,
                    success=True,
                    elapsed_ms=elapsed_ms,
                    message="Dictionary reload completed.",
                )
                
                logger.info(
                    "DrugDictionaryIndex reloaded successfully: drugs={}, aliases={}, error_aliases={}, ngram_tokens={}, elapsed_ms={:.1f}",
                    len(new_index.drug_summary_map),
                    sum(len(v) for v in new_index.alias_map.values()),
                    sum(len(v) for v in new_index.error_alias_map.values()),
                    len(new_index.ngram_index),
                    elapsed_ms,
                )
                return True
        except Exception as exc:
            self.last_reload_stats = {
                "success": False,
                "message": f"Dictionary reload failed: {exc}",
                "existing_index_kept": self.loaded,
                "drug_count": len(self.drug_summary_map),
                "alias_count": sum(len(v) for v in self.alias_map.values()),
                "error_alias_count": sum(len(v) for v in self.error_alias_map.values()),
                "ngram_token_count": len(self.ngram_index),
            }
            logger.exception("DrugDictionaryIndex reload failed. Existing index will be kept.")
            return False

    def reload_stats(self) -> Dict[str, Any]:
        return dict(self.last_reload_stats)

    @classmethod
    def build(
        cls,
        medications: Iterable[DrugInfo | Dict[str, Any]],
        aliases: Iterable[Dict[str, Any]] = (),
        error_aliases: Iterable[Dict[str, Any]] = (),
    ) -> "DrugDictionaryIndex":
        index = cls()

        for item in medications:
            drug = item if isinstance(item, DrugInfo) else DrugInfo(**item)
            summary = DrugSummary.from_drug_info(drug)
            if not summary.item_seq:
                continue
            index.drug_summary_map[summary.item_seq] = summary
            index._add_exact(summary)
            index._add_ngrams(summary)

        for alias in aliases:
            index._add_alias(index.alias_map, alias, score=0.98, source="local_alias")

        for alias in error_aliases:
            confidence = float(alias.get("confidence") or alias.get("error_alias_confidence") or 0.9)
            score = round(min(max(confidence, 0.5), 1.0) * 0.95, 3)
            index._add_alias(index.error_alias_map, alias, score=score, source="local_error_alias")

        index._loaded = bool(index.drug_summary_map)
        index.loaded_at = time.time() if index._loaded else None
        return index

    def match(self, query: str, threshold: float) -> MatchResult:
        if not self.ensure_loaded():
            return MatchResult(method="local_unavailable")

        # Use _current snapshot if available, to avoid partial-swap reads
        idx = self._current if self._current is not None else self

        candidates = idx.lookup_exact(query)
        if candidates and candidates[0].score >= threshold:
            return MatchResult(candidates=candidates, best_score=candidates[0].score, method="exact")

        candidates = idx.lookup_alias(query)
        alias_conflict = self._has_conflict(candidates)
        if candidates and candidates[0].score >= threshold:
            return MatchResult(
                candidates=self._deduplicate(candidates),
                best_score=candidates[0].score,
                method="alias",
                alias_conflict=alias_conflict,
            )

        candidates = idx.lookup_error_alias(query)
        error_conflict = self._has_conflict(candidates)
        alias_conflict = alias_conflict or error_conflict
        if candidates and candidates[0].score >= threshold:
            return MatchResult(
                candidates=self._deduplicate(candidates),
                best_score=candidates[0].score,
                method="error_alias",
                alias_conflict=alias_conflict,
            )

        candidate_pool = idx.lookup_ngram(query)
        fuzzy_candidates = idx.lookup_fuzzy(query, candidate_pool)

        merged = self._deduplicate([*candidate_pool, *fuzzy_candidates])
        best = merged[0] if merged else None
        method = best.method.replace("local_", "") if best else "none"
        return MatchResult(
            candidates=merged,
            best_score=best.score if best else 0.0,
            method=method,
            alias_conflict=alias_conflict,
        )

    def lookup_exact(self, query: str) -> List[MatchCandidate]:
        key = normalize_dictionary_key(query)
        compact_key = compact_dictionary_key(query)
        seqs = list(dict.fromkeys([*self.exact_map.get(key, []), *self.exact_map.get(compact_key, [])]))
        return [
            self._candidate(seq, 1.0, "local_exact", {"matched_text": key})
            for seq in seqs
            if seq in self.drug_summary_map
        ][: self.DEFAULT_LIMIT]

    def lookup_alias(self, query: str) -> List[MatchCandidate]:
        return self._lookup_alias_map(self.alias_map, query, "local_alias")

    def lookup_error_alias(self, query: str) -> List[MatchCandidate]:
        return self._lookup_alias_map(self.error_alias_map, query, "local_error_alias")

    def lookup_ngram(self, query: str) -> List[MatchCandidate]:
        key = compact_dictionary_key(query)
        grams = self._ngrams(key)
        if not grams:
            return []

        counts: Dict[str, int] = {}
        for gram in grams:
            for seq in self.ngram_index.get(gram, set()):
                counts[seq] = counts.get(seq, 0) + 1

        candidates = []
        for seq, overlap in counts.items():
            summary = self.drug_summary_map.get(seq)
            if not summary:
                continue
            target_grams = self._ngrams(compact_dictionary_key(summary.normalized_item_name or summary.item_name))
            denominator = max(len(set(grams) | set(target_grams)), 1)
            score = min((overlap / denominator) * 0.85, 0.85)
            candidates.append(
                self._candidate(
                    seq,
                    round(score, 3),
                    "local_ngram",
                    {
                        "matched_text": summary.normalized_item_name,
                        "ngram_overlap": overlap,
                    },
                )
            )
        return sorted(candidates, key=lambda c: c.score, reverse=True)[: self.FUZZY_POOL_LIMIT]

    def lookup_fuzzy(
        self,
        query: str,
        candidate_pool: Optional[List[MatchCandidate]] = None,
    ) -> List[MatchCandidate]:
        pool = candidate_pool or self.lookup_ngram(query)
        pool = pool[: self.FUZZY_POOL_LIMIT]
        if not pool:
            return []

        key = compact_dictionary_key(query)
        candidates = []
        for item in pool:
            summary = self.drug_summary_map.get(item.drug_info.item_seq)
            if not summary:
                continue
            target = compact_dictionary_key(summary.normalized_item_name or summary.item_name)
            ratio = fuzz.ratio(key, target) / 100.0
            partial = fuzz.partial_ratio(key, target) / 100.0
            score = round((ratio * 0.6 + partial * 0.4), 3)
            if score < 0.5:
                continue
            candidates.append(
                self._candidate(
                    summary.item_seq,
                    score,
                    "local_fuzzy",
                    {
                        "matched_text": summary.normalized_item_name,
                        "fuzzy_score": score,
                        "ratio": round(ratio, 3),
                        "partial_ratio": round(partial, 3),
                        "candidate_pool_size": len(pool),
                    },
                )
            )
        return sorted(candidates, key=lambda c: c.score, reverse=True)[: self.DEFAULT_LIMIT]

    def _add_exact(self, summary: DrugSummary) -> None:
        for value in (summary.item_name, summary.normalized_item_name):
            key = normalize_dictionary_key(value)
            compact_key = compact_dictionary_key(value)
            if key:
                self.exact_map.setdefault(key, []).append(summary.item_seq)
            if compact_key and compact_key != key:
                self.exact_map.setdefault(compact_key, []).append(summary.item_seq)

    def _add_ngrams(self, summary: DrugSummary) -> None:
        key = compact_dictionary_key(summary.normalized_item_name or summary.item_name)
        for gram in self._ngrams(key):
            self.ngram_index.setdefault(gram, set()).add(summary.item_seq)

    def _add_alias(
        self,
        alias_map: Dict[str, List[AliasSummary]],
        alias: Dict[str, Any],
        score: float,
        source: str,
    ) -> None:
        item_seq = str(alias.get("item_seq") or "")
        if item_seq not in self.drug_summary_map:
            return

        text = (
            alias.get("alias_name")
            or alias.get("error_text")
            or alias.get("normalized_error_text")
            or alias.get("alias_normalized")
            or ""
        )
        normalized_text = (
            alias.get("alias_normalized")
            or alias.get("normalized_error_text")
            or text
        )
        summary = AliasSummary(
            item_seq=item_seq,
            text=text,
            normalized_text=normalized_text,
            evidence={**alias, "source": source},
            score=score,
        )
        for value in (text, normalized_text):
            key = normalize_dictionary_key(value)
            compact_key = compact_dictionary_key(value)
            if key:
                alias_map.setdefault(key, []).append(summary)
            if compact_key and compact_key != key:
                alias_map.setdefault(compact_key, []).append(summary)

    def _lookup_alias_map(
        self,
        alias_map: Dict[str, List[AliasSummary]],
        query: str,
        method: str,
    ) -> List[MatchCandidate]:
        key = normalize_dictionary_key(query)
        compact_key = compact_dictionary_key(query)
        aliases = [*alias_map.get(key, []), *alias_map.get(compact_key, [])]
        candidates = []
        for alias in aliases:
            evidence = {
                "match_method": method,
                "normalized_query": key,
                "matched_text": alias.normalized_text or alias.text,
                **alias.evidence,
            }
            candidates.append(self._candidate(alias.item_seq, alias.score, method, evidence))
        return self._deduplicate(candidates)

    def _candidate(self, item_seq: str, score: float, method: str, evidence: Dict[str, Any]) -> MatchCandidate:
        summary = self.drug_summary_map[item_seq]
        source = evidence.get("source", method)
        return MatchCandidate(
            drug_info=summary.to_drug_info(),
            score=score,
            method=method.replace("local_", ""),
            evidence={
                "source": source,
                "match_method": method,
                "name_match": score,
                **evidence,
            },
        )

    @staticmethod
    def _ngrams(text: str) -> List[str]:
        if len(text) < DrugDictionaryIndex.NGRAM_SIZE:
            return []
        return [
            text[i : i + DrugDictionaryIndex.NGRAM_SIZE]
            for i in range(len(text) - DrugDictionaryIndex.NGRAM_SIZE + 1)
        ]

    @staticmethod
    def _has_conflict(candidates: List[MatchCandidate]) -> bool:
        return len({c.drug_info.item_seq for c in candidates}) >= 2

    @staticmethod
    def _deduplicate(candidates: List[MatchCandidate]) -> List[MatchCandidate]:
        best_by_seq: Dict[str, MatchCandidate] = {}
        for candidate in candidates:
            seq = candidate.drug_info.item_seq
            if seq not in best_by_seq or candidate.score > best_by_seq[seq].score:
                best_by_seq[seq] = candidate
        result = list(best_by_seq.values())
        result.sort(key=lambda c: c.score, reverse=True)
        return result

    @staticmethod
    def _stats_from_index(
        index: "DrugDictionaryIndex",
        *,
        success: bool,
        elapsed_ms: float,
        message: str,
    ) -> Dict[str, Any]:
        return {
            "success": success,
            "message": message,
            "existing_index_kept": False,
            "elapsed_ms": round(elapsed_ms, 1),
            "drug_count": len(index.drug_summary_map),
            "alias_count": sum(len(v) for v in index.alias_map.values()),
            "error_alias_count": sum(len(v) for v in index.error_alias_map.values()),
            "ngram_token_count": len(index.ngram_index),
            "loaded_at": index.loaded_at,
        }
