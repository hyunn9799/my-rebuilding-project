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
    repo.aliases = [{"item_seq": "A001", "alias_name": "타이", "alias_normalized": "타이"}]
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
