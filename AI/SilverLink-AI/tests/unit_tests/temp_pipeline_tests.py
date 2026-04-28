def test_pipeline_reload_dictionary_calls_index_reload():
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
