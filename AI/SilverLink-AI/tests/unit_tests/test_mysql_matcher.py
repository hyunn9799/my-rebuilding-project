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
    assert result.candidates[0].evidence["source"] == "mysql_alias"
    assert result.candidates[0].evidence["alias_name"] == "타이레놀"
    assert repo.calls == ["exact", "alias"]


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
    assert result.candidates[0].evidence["source"] == "mysql_error_alias"
    assert result.candidates[0].evidence["error_text"] == "타이레놀5OOmg"
    assert repo.calls == ["exact", "alias", "error_alias"]
