"""PseudoConfidenceScorer 단위 테스트."""
from app.ocr.model.drug_model import DrugInfo, MatchCandidate, NormalizedDrug
from app.ocr.services.pseudo_confidence_scorer import PseudoConfidenceScorer


def _drug(item_seq: str = "SEQ-001", name: str = "타이레놀정500mg") -> DrugInfo:
    return DrugInfo(
        item_seq=item_seq,
        item_name=name,
        item_name_normalized=name,
        entp_name="테스트제약",
    )


def _candidate(
    name: str = "타이레놀정500mg",
    score: float = 0.9,
    method: str = "exact",
    evidence: dict = None,
    item_seq: str = "SEQ-001",
) -> MatchCandidate:
    ev = {"name_match": score, "source": f"mysql_{method}"}
    if evidence:
        ev.update(evidence)
    return MatchCandidate(
        drug_info=_drug(item_seq, name),
        score=score,
        method=method,
        evidence=ev,
    )


def test_strength_match_gives_high_score():
    """용량 일치 시 높은 점수."""
    scorer = PseudoConfidenceScorer()
    candidate = _candidate(
        score=0.95,
        evidence={"strength_match": True, "manufacturer_match": True, "form_match": True},
    )
    nd = NormalizedDrug(name="타이레놀", dosage="500mg", original="타이레놀정 500mg")

    result = scorer.score([candidate], nd)

    assert len(result) == 1
    assert result[0].score >= 0.85  # 높은 종합 점수
    breakdown = result[0].evidence["score_breakdown"]
    assert breakdown["strength_match"] == 0.25
    assert breakdown["penalty"] == 0.0


def test_strength_mismatch_causes_heavy_penalty():
    """용량 불일치 시 강한 패널티로 낮은 점수."""
    scorer = PseudoConfidenceScorer()
    candidate = _candidate(
        name="타이레놀정50mg",
        score=0.92,
        evidence={"strength_match": False, "manufacturer_match": True},
    )
    nd = NormalizedDrug(name="타이레놀", dosage="500mg", original="타이레놀정 500mg")

    result = scorer.score([candidate], nd)

    assert result[0].score < 0.5  # 패널티로 낮아짐
    breakdown = result[0].evidence["score_breakdown"]
    assert breakdown["penalty"] == -0.4
    assert breakdown["strength_match"] == 0.0


def test_5mg_50mg_500mg_not_auto_confirmed():
    """5mg vs 50mg vs 500mg — 용량 충돌 시 자동 확정 방지."""
    scorer = PseudoConfidenceScorer()
    candidates = [
        _candidate("약품A 500mg", 0.95, item_seq="A", evidence={"strength_match": False}),
        _candidate("약품A 50mg", 0.93, item_seq="B", evidence={"strength_match": False}),
        _candidate("약품A 5mg", 0.90, item_seq="C", evidence={"strength_match": False}),
    ]
    nd = NormalizedDrug(name="약품A", dosage="500mg", original="약품A 500mg")

    result = scorer.score(candidates, nd)

    # 모든 후보에 패널티 적용 → 점수 낮음
    for c in result:
        assert c.score < 0.5


def test_manufacturer_match_bonus():
    """제조사 일치 보너스."""
    scorer = PseudoConfidenceScorer()
    with_mfr = _candidate(score=0.9, evidence={"manufacturer_match": True})
    without_mfr = _candidate(score=0.9, evidence={"manufacturer_match": False}, item_seq="SEQ-002")

    result_with = scorer.score([with_mfr])[0]
    result_without = scorer.score([without_mfr])[0]

    assert result_with.score > result_without.score


def test_top_gap_small_gives_low_gap_bonus():
    """top1-top2 점수 차이가 작으면 gap 보너스가 큰 차이일 때보다 낮음."""
    scorer = PseudoConfidenceScorer()
    # 차이가 작은 경우
    close_candidates = [
        _candidate("약품A", 0.90, item_seq="A"),
        _candidate("약품B", 0.89, item_seq="B"),  # gap = 0.01
    ]
    # 차이가 큰 경우
    far_candidates = [
        _candidate("약품C", 0.95, item_seq="C"),
        _candidate("약품D", 0.60, item_seq="D"),  # gap = 0.35
    ]

    close_result = scorer.score(close_candidates)
    far_result = scorer.score(far_candidates)

    close_breakdown = close_result[0].evidence["score_breakdown"]
    far_breakdown = far_result[0].evidence["score_breakdown"]

    # 큰 갭의 gap 보너스가 작은 갭보다 커야 함
    assert far_breakdown["top_gap"] > close_breakdown["top_gap"]


def test_breakdown_structure():
    """breakdown에 7개 필드가 모두 존재."""
    scorer = PseudoConfidenceScorer()
    candidate = _candidate(score=0.9, evidence={"strength_match": True})

    result = scorer.score([candidate])

    breakdown = result[0].evidence["score_breakdown"]
    expected_keys = {"name_match", "strength_match", "unit_match", "form_match",
                     "manufacturer_match", "top_gap", "penalty"}
    assert set(breakdown.keys()) == expected_keys
    # total은 property이므로 dict에 없음


def test_low_ocr_confidence_penalty():
    """OCR 신뢰도가 낮으면 패널티 적용."""
    scorer = PseudoConfidenceScorer()
    candidate = _candidate(score=0.9)
    nd_low = NormalizedDrug(name="타이레놀", original="타이레놀정", ocr_confidence=0.5)
    nd_high = NormalizedDrug(name="타이레놀", original="타이레놀정", ocr_confidence=0.9)

    result_low = scorer.score([candidate], nd_low)[0]
    result_high = scorer.score([candidate], nd_high)[0]

    assert result_low.score < result_high.score
    assert result_low.evidence["score_breakdown"]["penalty"] < 0
