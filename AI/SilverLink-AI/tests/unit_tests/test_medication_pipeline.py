import pytest

from app.ocr.model.drug_model import DrugInfo, MatchCandidate, MatchResult, NormalizedDrug, OCRToken
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


class StaticNormalizer:
    def __init__(self, normalized_drugs):
        self.normalized_drugs = normalized_drugs

    def normalize(self, ocr_text: str, ocr_tokens=None):
        return self.normalized_drugs


class RecordingVectorMatcher:
    def __init__(self, candidates_by_name):
        self.candidates_by_name = candidates_by_name
        self.calls = []

    def match(self, normalized_name: str, top_k: int = 5) -> MatchResult:
        self.calls.append(normalized_name)
        candidates = self.candidates_by_name.get(normalized_name, [])
        return MatchResult(
            candidates=candidates,
            best_score=candidates[0].score if candidates else 0.0,
            method=candidates[0].method if candidates else "none",
        )


class FakeLLMDescriptor:
    async def generate_description(self, candidates, ocr_text, decision_status="MATCHED"):
        return {
            "description": f"{decision_status}: {len(candidates)} candidate(s)",
            "medications": [],
            "warnings": [],
        }


def _candidate(
    name: str,
    score: float,
    method: str = "exact",
    source: str | None = None,
    item_seq: str | None = None,
    entp_name: str = "테스트제약",
    ingredient: str | None = None,
) -> MatchCandidate:
    return MatchCandidate(
        drug_info=DrugInfo(
            item_seq=item_seq or f"SEQ-{name}",
            item_name=name,
            item_name_normalized=name,
            entp_name=entp_name,
            item_ingr_name=ingredient,
            efcy_qesitm="해열 및 진통",
            use_method_qesitm="정해진 용법에 따라 복용",
            atpn_qesitm="이상 증상이 있으면 전문가에게 상담",
        ),
        score=score,
        method=method,
        evidence={"source": source or f"mysql_{method}", "name_match": score},
    )


def _normalized(name: str, dosage: str | None = None, form_type: str | None = None) -> NormalizedDrug:
    return NormalizedDrug(
        name=name,
        dosage=dosage,
        form_type=form_type,
        original=name,
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
    assert result.requires_user_confirmation is True


@pytest.mark.asyncio
async def test_pipeline_vector_fallback_runs_for_partial_product_name():
    vector_matcher = RecordingVectorMatcher({
        "타이레": [
            _candidate(
                "타이레놀정500mg",
                0.93,
                method="vector",
                source="vector_db",
                entp_name="한국존슨앤드존슨",
            )
        ]
    })
    pipeline = MedicationPipeline(
        text_normalizer=StaticNormalizer([_normalized("타이레", dosage="500mg", form_type="정")]),
        mysql_matcher=FakeMySQLMatcher([]),
        vector_matcher=vector_matcher,
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process("타이레 500mg 정")

    assert vector_matcher.calls == ["타이레"]
    assert result.decision_status == "NEED_USER_CONFIRMATION"
    assert result.requires_user_confirmation is True
    assert result.identified_drugs[0].method == "vector"
    assert any(stage.stage == "vector_match" for stage in result.pipeline_stages)


@pytest.mark.asyncio
async def test_pipeline_vector_fallback_runs_for_ingredient_only_ocr():
    vector_matcher = RecordingVectorMatcher({
        "아세트아미노펜": [
            _candidate(
                "타이레놀정500mg",
                0.88,
                method="vector",
                source="vector_db",
                ingredient="아세트아미노펜",
            )
        ]
    })
    pipeline = MedicationPipeline(
        text_normalizer=StaticNormalizer([_normalized("아세트아미노펜", dosage="500mg")]),
        mysql_matcher=FakeMySQLMatcher([]),
        vector_matcher=vector_matcher,
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process("아세트아미노펜 500mg")

    assert vector_matcher.calls == ["아세트아미노펜"]
    assert result.decision_status == "NEED_USER_CONFIRMATION"
    assert result.requires_user_confirmation is True
    assert result.identified_drugs[0].drug_info.item_ingr_name == "아세트아미노펜"


@pytest.mark.asyncio
async def test_pipeline_vector_candidate_records_manufacturer_evidence():
    vector_matcher = RecordingVectorMatcher({
        "타이레": [
            _candidate(
                "타이레놀정500mg",
                0.91,
                method="vector",
                source="vector_db",
                entp_name="한국존슨앤드존슨",
            )
        ]
    })
    pipeline = MedicationPipeline(
        text_normalizer=StaticNormalizer([_normalized("타이레", dosage="500mg")]),
        mysql_matcher=FakeMySQLMatcher([]),
        vector_matcher=vector_matcher,
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process("한국존슨앤드존슨 타이레 500mg")

    assert result.decision_status == "NEED_USER_CONFIRMATION"
    assert result.identified_drugs[0].evidence["manufacturer_match"] is True


@pytest.mark.asyncio
async def test_pipeline_uses_vector_when_alias_has_no_candidate():
    vector_candidate = _candidate(
        "게보린정",
        0.99,
        method="vector",
        source="vector_db",
    )
    vector_matcher = RecordingVectorMatcher({"게보링": [vector_candidate]})
    pipeline = MedicationPipeline(
        text_normalizer=StaticNormalizer([_normalized("게보링", form_type="정")]),
        mysql_matcher=FakeMySQLMatcher([]),
        vector_matcher=vector_matcher,
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process("게보링정")

    assert vector_matcher.calls == ["게보링"]
    assert result.identified_drugs[0].drug_info.item_name == "게보린정"
    assert result.decision_status == "NEED_USER_CONFIRMATION"


@pytest.mark.asyncio
async def test_pipeline_keeps_low_confidence_for_irrelevant_vector_result():
    vector_matcher = RecordingVectorMatcher({
        "건강기능식품": [
            _candidate(
                "타이레놀정500mg",
                0.2,
                method="vector",
                source="vector_db",
            )
        ]
    })
    pipeline = MedicationPipeline(
        text_normalizer=StaticNormalizer([_normalized("건강기능식품")]),
        mysql_matcher=FakeMySQLMatcher([]),
        vector_matcher=vector_matcher,
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
        match_threshold=0.7,
    )

    result = await pipeline.process("건강기능식품 안내문")

    assert vector_matcher.calls == ["건강기능식품"]
    assert result.decision_status == "LOW_CONFIDENCE"
    assert result.match_confidence < 0.7
    assert result.requires_user_confirmation is True


def test_pipeline_reload_dictionary_calls_index_reload():
    from app.ocr.services.drug_dictionary_index import DrugDictionaryIndex

    class FakeDictionaryIndex(DrugDictionaryIndex):
        def __init__(self):
            self.reloaded = False
        def reload(self):
            self.reloaded = True
            return True
            
    fake_index = FakeDictionaryIndex()
    mysql_matcher = FakeMySQLMatcher([])
    mysql_matcher.dictionary_index = fake_index
    
    pipeline = MedicationPipeline(
        text_normalizer=TextNormalizer(),
        mysql_matcher=mysql_matcher,
        vector_matcher=FakeVectorMatcher(),
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
    )
    
    result = pipeline.reload_dictionary()
    
    assert result is True
    assert fake_index.reloaded is True
    assert pipeline.drug_index is fake_index


def test_pipeline_reload_dictionary_returns_false_if_no_index():
    pipeline = MedicationPipeline(
        text_normalizer=TextNormalizer(),
        mysql_matcher=FakeMySQLMatcher([]),
        vector_matcher=FakeVectorMatcher(),
        rule_validator=RuleValidator(),
        llm_descriptor=FakeLLMDescriptor(),
    )
    
    result = pipeline.reload_dictionary()
    
    assert result is False
