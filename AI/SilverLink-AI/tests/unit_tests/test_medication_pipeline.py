import pytest

from app.ocr.model.drug_model import DrugInfo, MatchCandidate, MatchResult, OCRToken
from app.ocr.services.llm_descriptor import LLMDescriptor
from app.ocr.services.medication_pipeline import MedicationPipeline
from app.ocr.services.rule_validator import RuleValidator
from app.ocr.services.text_normalizer import TextNormalizer


class FakeMySQLMatcher:
    def __init__(self, candidates):
        self.candidates = candidates

    def match(self, normalized_name: str) -> MatchResult:
        return MatchResult(
            candidates=self.candidates,
            best_score=self.candidates[0].score if self.candidates else 0.0,
            method=self.candidates[0].method if self.candidates else "none",
        )


class FakeVectorMatcher:
    def __init__(self):
        self.called = False

    def match(self, normalized_name: str, top_k: int = 5) -> MatchResult:
        self.called = True
        return MatchResult()


class FakeLLMDescriptor:
    async def generate_description(self, candidates, ocr_text, decision_status="MATCHED"):
        return {
            "description": f"{decision_status}: {len(candidates)} candidate(s)",
            "medications": [],
            "warnings": [],
        }


def _candidate(name: str, score: float, method: str = "exact") -> MatchCandidate:
    return MatchCandidate(
        drug_info=DrugInfo(
            item_seq=f"SEQ-{name}",
            item_name=name,
            item_name_normalized=name,
            entp_name="테스트제약",
            efcy_qesitm="해열 및 진통",
            use_method_qesitm="정해진 용법에 따라 복용",
            atpn_qesitm="이상 증상이 있으면 전문가에게 상담",
        ),
        score=score,
        method=method,
        evidence={"source": f"mysql_{method}", "name_match": score},
    )


@pytest.mark.asyncio
async def test_pipeline_returns_matched_for_high_confidence_exact_candidate():
    pipeline = MedicationPipeline(
        text_normalizer=TextNormalizer(),
        mysql_matcher=FakeMySQLMatcher([_candidate("타이레놀정", 0.9)]),
        vector_matcher=FakeVectorMatcher(),
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process("타이레놀정 500mg\n식후 30분")

    assert result.success is True
    assert result.decision_status == "MATCHED"
    assert result.requires_user_confirmation is False
    assert result.match_confidence >= 0.7
    assert any(stage.stage == "decision" for stage in result.pipeline_stages)


@pytest.mark.asyncio
async def test_pipeline_requires_confirmation_when_top_scores_are_close():
    pipeline = MedicationPipeline(
        text_normalizer=TextNormalizer(),
        mysql_matcher=FakeMySQLMatcher([
            _candidate("타이레놀정", 0.9),
            _candidate("타이레놀서방정", 0.86),
        ]),
        vector_matcher=FakeVectorMatcher(),
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process("타이레놀정 500mg")

    assert result.success is True
    assert result.decision_status == "AMBIGUOUS"
    assert result.requires_user_confirmation is True


@pytest.mark.asyncio
async def test_pipeline_marks_low_confidence_from_ocr_tokens():
    pipeline = MedicationPipeline(
        text_normalizer=TextNormalizer(),
        mysql_matcher=FakeMySQLMatcher([_candidate("타이레놀정", 0.9)]),
        vector_matcher=FakeVectorMatcher(),
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process(
        "타이레놀정 500mg",
        ocr_tokens=[
            OCRToken(value="타이레놀정", confidence=0.61),
            OCRToken(value="500mg", confidence=0.66),
        ],
    )

    assert result.decision_status == "LOW_CONFIDENCE"
    assert result.requires_user_confirmation is True


def test_llm_prompt_contains_safety_constraints():
    descriptor = LLMDescriptor(llm=None)
    messages = descriptor._build_prompt([_candidate("타이레놀정", 0.9)], "타이레놀정", "AMBIGUOUS")

    system_prompt = messages[0]["content"]
    user_prompt = messages[1]["content"]

    assert "DB 정보에 없는 효능" in system_prompt
    assert "약품 식별" in system_prompt
    assert "AMBIGUOUS" in user_prompt
def test_text_normalizer_joins_spaced_dosage_digits():
    normalizer = TextNormalizer()

    result = normalizer.normalize("\ud0c0\uc774\ub808\ub1805 0 0 mg\n\uc2dd\ud6c4 30\ubd84")

    assert len(result) == 1
    assert result[0].name == "\ud0c0\uc774\ub808\ub180"
    assert result[0].dosage == "500mg"


@pytest.mark.asyncio
async def test_pipeline_requires_confirmation_on_strength_mismatch():
    pipeline = MedicationPipeline(
        text_normalizer=TextNormalizer(),
        mysql_matcher=FakeMySQLMatcher([_candidate("\ud0c0\uc774\ub808\ub180\uc815 50mg", 0.92)]),
        vector_matcher=FakeVectorMatcher(),
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process("\ud0c0\uc774\ub808\ub180 500mg")

    assert result.decision_status == "NEED_USER_CONFIRMATION"
    assert result.requires_user_confirmation is True
    assert result.identified_drugs[0].evidence["strength_match"] is False


@pytest.mark.asyncio
async def test_pipeline_vectordb_only_candidate_not_auto_matched():
    """VectorDB 후보만 있을 때 MATCHED가 아닌 NEED_USER_CONFIRMATION."""

    class VectorOnlyMySQLMatcher:
        """MySQL에서 매칭 실패 → VectorDB만 후보를 제공하는 시나리오."""
        def match(self, normalized_name: str) -> MatchResult:
            return MatchResult(candidates=[], best_score=0.0, method="none")

    class HighScoreVectorMatcher:
        """높은 코사인 유사도로 후보를 반환하는 VectorMatcher."""
        def match(self, normalized_name: str, top_k: int = 5) -> MatchResult:
            return MatchResult(
                candidates=[
                    MatchCandidate(
                        drug_info=DrugInfo(
                            item_seq="VEC-001",
                            item_name="타이레놀정500mg",
                            item_name_normalized="타이레놀정500mg",
                            entp_name="한국존슨앤드존슨",
                            efcy_qesitm="해열 및 진통",
                            use_method_qesitm="1회 1~2정",
                            atpn_qesitm="용량 주의",
                        ),
                        score=0.92,
                        method="vector",
                        evidence={
                            "source": "vector_db",
                            "distance": 0.08,
                            "name_match": 0.92,
                        },
                    )
                ],
                best_score=0.92,
                method="vector",
            )

    pipeline = MedicationPipeline(
        text_normalizer=TextNormalizer(),
        mysql_matcher=VectorOnlyMySQLMatcher(),
        vector_matcher=HighScoreVectorMatcher(),
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process("타이레놀정 500mg")

    # VectorDB 후보만으로는 자동 확정되지 않아야 함
    assert result.decision_status == "NEED_USER_CONFIRMATION"
    assert result.requires_user_confirmation is True
