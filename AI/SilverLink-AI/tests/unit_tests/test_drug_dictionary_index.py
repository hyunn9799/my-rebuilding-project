from app.ocr.model.drug_model import DrugInfo
from app.ocr.services.drug_dictionary_index import DrugDictionaryIndex
from app.ocr.services.mysql_matcher import MySQLMatcher


def _drug(item_seq: str, name: str) -> DrugInfo:
    return DrugInfo(
        item_seq=item_seq,
        item_name=name,
        item_name_normalized=name,
        entp_name="테스트제약",
    )


def _index() -> DrugDictionaryIndex:
    return DrugDictionaryIndex.build(
        medications=[
            _drug("A001", "타이레놀정500mg"),
            _drug("B002", "타이레놀이알정650mg"),
            _drug("C003", "아스피린정100mg"),
        ],
        aliases=[
            {
                "item_seq": "A001",
                "alias_name": "타이레놀",
                "alias_normalized": "타이레놀",
                "alias_source": "test",
            }
        ],
        error_aliases=[
            {
                "item_seq": "A001",
                "error_text": "타이레놀정5OOmg",
                "normalized_error_text": "타이레놀정500mg",
                "correction_reason": "OCR O/0 confusion",
                "confidence": 0.9,
            }
        ],
    )


def test_local_exact_lookup_returns_candidate():
    result = _index().match("타이레놀정500mg", threshold=0.7)

    assert result.method == "exact"
    assert result.best_score == 1.0
    assert result.candidates[0].evidence["source"] == "local_exact"


def test_local_alias_lookup_returns_candidate():
    result = _index().match("타이레놀", threshold=0.7)

    assert result.method == "alias"
    assert result.candidates[0].drug_info.item_seq == "A001"
    assert result.candidates[0].evidence["source"] == "local_alias"


def test_local_alias_conflict_returns_ambiguous_signal():
    index = DrugDictionaryIndex.build(
        medications=[
            _drug("A001", "타이레놀정500mg"),
            _drug("B002", "타이레놀이알정650mg"),
        ],
        aliases=[
            {"item_seq": "A001", "alias_name": "타이레놀", "alias_normalized": "타이레놀"},
            {"item_seq": "B002", "alias_name": "타이레놀", "alias_normalized": "타이레놀"},
        ],
    )

    result = index.match("타이레놀", threshold=0.7)

    assert result.method == "alias"
    assert result.alias_conflict is True
    assert len(result.candidates) == 2


def test_local_error_alias_lookup_returns_candidate():
    result = _index().match("타이레놀정5OOmg", threshold=0.7)

    assert result.method == "error_alias"
    assert result.candidates[0].drug_info.item_seq == "A001"
    assert result.candidates[0].evidence["source"] == "local_error_alias"


def test_local_ngram_lookup_returns_reduced_candidate_pool():
    candidates = _index().lookup_ngram("타이레놀서방정")

    assert candidates
    assert len(candidates) <= DrugDictionaryIndex.FUZZY_POOL_LIMIT
    assert all(candidate.evidence["source"] == "local_ngram" for candidate in candidates)


def test_local_fuzzy_runs_only_on_reduced_candidate_pool():
    index = _index()
    pool = index.lookup_ngram("타이레놀서방정")

    result = index.lookup_fuzzy("타이레놀서방정", pool)

    assert result
    assert all(candidate.evidence["candidate_pool_size"] == len(pool) for candidate in result)
    assert len(pool) < len(index.drug_summary_map) or len(index.drug_summary_map) <= index.FUZZY_POOL_LIMIT


class FailingIndex:
    def match(self, normalized_name, threshold):
        raise RuntimeError("index unavailable")


class FallbackRepo:
    def __init__(self):
        self.calls = []

    def exact_match(self, name):
        self.calls.append("exact")
        return [(_drug("A001", "타이레놀정500mg"), 1.0)]

    def alias_match(self, name):
        self.calls.append("alias")
        return []

    def error_alias_match(self, name):
        self.calls.append("error_alias")
        return []

    def prefix_match(self, name):
        self.calls.append("prefix")
        return []

    def ngram_match(self, name):
        self.calls.append("ngram")
        return []

    def fetch_all_for_fuzzy(self):
        self.calls.append("fuzzy")
        return []


def test_index_loading_failure_uses_mysql_fallback():
    repo = FallbackRepo()
    matcher = MySQLMatcher(repo, threshold=0.7, dictionary_index=FailingIndex())

    result = matcher.match("타이레놀정500mg")

    assert result.method == "exact"
    assert repo.calls == ["exact"]


def test_local_reload_success_replaces_index():
    class MockReloadRepo:
        def __init__(self):
            self.meds = [_drug("A001", "타이레놀정500mg")]
            self.aliases = []
            self.err_aliases = []

        def fetch_all_medications_for_index(self):
            return self.meds

        def fetch_all_aliases_for_index(self):
            return self.aliases

        def fetch_all_error_aliases_for_index(self):
            return self.err_aliases

    repo = MockReloadRepo()
    index = DrugDictionaryIndex(drug_repository=repo)
    
    # 1. Initial Load
    index.ensure_loaded()
    assert index.match("타이레놀정500mg", 0.7).method == "exact"
    
    # 2. Add alias and reload
    repo.aliases = [{"item_seq": "A001", "alias_name": "타이", "alias_normalized": "타이", "alias_source": "test"}]
    result = index.reload()
    
    assert result is True
    # The new alias should now match
    match = index.match("타이", 0.7)
    assert match.method == "alias"


def test_local_reload_failure_keeps_existing_index():
    class MockReloadRepoFailing:
        def __init__(self):
            self.fail_mode = False

        def fetch_all_medications_for_index(self):
            if self.fail_mode:
                raise Exception("DB is down")
            return [_drug("A001", "타이레놀정500mg")]

        def fetch_all_aliases_for_index(self):
            return []

        def fetch_all_error_aliases_for_index(self):
            return []

    repo = MockReloadRepoFailing()
    index = DrugDictionaryIndex(drug_repository=repo)
    
    # 1. Initial Load
    index.ensure_loaded()
    assert index.match("타이레놀정500mg", 0.7).method == "exact"
    
    # 2. Trigger Failure
    repo.fail_mode = True
    result = index.reload()
    
    # 3. Validation
    assert result is False
    # The exact match should still work using the old index
    assert index.match("타이레놀정500mg", 0.7).method == "exact"
