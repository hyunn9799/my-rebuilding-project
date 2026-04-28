from app.ocr.model.drug_model import DrugInfo
from app.ocr.services.mysql_matcher import MySQLMatcher


def _drug(item_seq: str, name: str) -> DrugInfo:
    return DrugInfo(
        item_seq=item_seq,
        item_name=name,
        item_name_normalized=name,
        entp_name="테스트제약",
    )


class FakeDrugRepository:
    def __init__(self):
        self.calls = []
        self.alias_results = []
        self.error_alias_results = []
        self.prefix_results = []

    def exact_match(self, name: str):
        self.calls.append("exact")
        return []

    def alias_match(self, name: str):
        self.calls.append("alias")
        return self.alias_results

    def error_alias_match(self, name: str):
        self.calls.append("error_alias")
        return self.error_alias_results

    def prefix_match(self, name: str):
        self.calls.append("prefix")
        return self.prefix_results

    def ngram_match(self, name: str):
        self.calls.append("ngram")
        return []

    def fetch_all_for_fuzzy(self):
        self.calls.append("fuzzy")
        return []


def test_mysql_matcher_uses_alias_before_prefix():
    repo = FakeDrugRepository()
    repo.alias_results = [
        (
            _drug("A001", "타이레놀정500mg"),
            0.98,
            {"alias_name": "타이레놀"},
        )
    ]
    matcher = MySQLMatcher(repo, threshold=0.7)

    result = matcher.match("타이레놀")

    assert result.method == "alias"
    assert result.best_score == 0.98
    assert result.candidates[0].method == "alias"
    assert result.candidates[0].evidence["source"] == "mysql_fallback"
    assert result.candidates[0].evidence["match_method"] == "mysql_alias"
    assert result.candidates[0].evidence["alias_name"] == "타이레놀"
    assert repo.calls == ["exact", "alias"]
    assert result.alias_conflict is False


def test_mysql_matcher_uses_error_alias_before_prefix():
    repo = FakeDrugRepository()
    repo.error_alias_results = [
        (
            _drug("A002", "타이레놀정500mg"),
            0.855,
            {
                "error_text": "타이레놀5OOmg",
                "normalized_error_text": "타이레놀500mg",
                "correction_reason": "OCR O/0 confusion",
            },
        )
    ]
    matcher = MySQLMatcher(repo, threshold=0.7)

    result = matcher.match("타이레놀5OOmg")

    assert result.method == "error_alias"
    assert result.best_score == 0.855
    assert result.candidates[0].method == "error_alias"
    assert result.candidates[0].evidence["source"] == "mysql_fallback"
    assert result.candidates[0].evidence["match_method"] == "mysql_error_alias"
    assert result.candidates[0].evidence["error_text"] == "타이레놀5OOmg"
    assert repo.calls == ["exact", "alias", "error_alias"]
    assert result.alias_conflict is False


def test_alias_conflict_detected_when_multiple_drugs():
    """동일 alias가 2개 이상 다른 drug에 매핑되면 alias_conflict=True."""
    repo = FakeDrugRepository()
    repo.alias_results = [
        (
            _drug("A001", "타이레놀정500mg"),
            0.98,
            {"alias_name": "타이레놀"},
        ),
        (
            _drug("B002", "타이레놀이알정650mg"),
            0.98,
            {"alias_name": "타이레놀"},
        ),
    ]
    matcher = MySQLMatcher(repo, threshold=0.7)

    result = matcher.match("타이레놀")

    assert result.method == "alias"
    assert result.alias_conflict is True
    assert len(result.candidates) == 2


def test_alias_conflict_not_set_for_same_drug():
    """동일 alias가 같은 drug에 여러 번 매핑되면 alias_conflict=False."""
    repo = FakeDrugRepository()
    repo.alias_results = [
        (
            _drug("A001", "타이레놀정500mg"),
            0.98,
            {"alias_name": "타이레놀"},
        ),
        (
            _drug("A001", "타이레놀정500mg"),
            0.95,
            {"alias_name": "타이레놀정"},
        ),
    ]
    matcher = MySQLMatcher(repo, threshold=0.7)

    result = matcher.match("타이레놀")

    assert result.method == "alias"
    assert result.alias_conflict is False
    # 중복 제거 → 1개
    assert len(result.candidates) == 1


def test_error_alias_conflict_detected():
    """error_alias에서도 복수 drug 매핑 시 alias_conflict=True."""
    repo = FakeDrugRepository()
    repo.error_alias_results = [
        (
            _drug("A001", "약품A"),
            0.85,
            {"error_text": "약품a", "correction_reason": "case confusion"},
        ),
        (
            _drug("B002", "약품B"),
            0.80,
            {"error_text": "약품a", "correction_reason": "case confusion"},
        ),
    ]
    matcher = MySQLMatcher(repo, threshold=0.7)

    result = matcher.match("약품a")

    assert result.alias_conflict is True
    assert result.method == "error_alias"
