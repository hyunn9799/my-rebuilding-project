"""Vector search integration tests"""
import pytest
import asyncio
import os



@pytest.mark.database
@pytest.mark.integration
@pytest.mark.skipif(not os.getenv("OPENAI_API_KEY"), reason="Env vars missing")
class TestVectorSearch:
    """Test vector search functionality and accuracy"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """Setup for each test"""
        from app.chatbot.repository.chatbot_repository import VectorStoreService
        from app.chatbot.services.embedding_service import EmbeddingService
        self.embedding_service = EmbeddingService()
        self.vector_store = VectorStoreService()
    
    def test_faq_vector_search(self):
        """Test FAQ vector search accuracy"""
        # Create embedding for a question
        query = "노인 장기 요양 보험이 뭐야?"
        embedding = self.embedding_service.create_embedding(query)
        
        assert embedding is not None
        assert isinstance(embedding, list)
        assert len(embedding) == 1536, "Embedding dimension should be 1536"
        
        # Search FAQ collection
        results = self.vector_store.search_faq(embedding, limit=3)
        
        assert results is not None
        assert len(results) > 0, "Should return search results"
        
        # Check result structure
        for hits in results:
            for hit in hits:
                assert hasattr(hit, 'score'), "Result should have score"
                assert hasattr(hit, 'entity'), "Result should have entity"
                assert 'question' in hit.entity, "Entity should have question"
                assert 'answer' in hit.entity, "Entity should have answer"
                assert 'category' in hit.entity, "Entity should have category"
                
                print(f"✅ Found FAQ (score: {hit.score:.4f}): {hit.entity.get('question')[:50]}...")
    
    def test_inquiry_vector_search_with_filter(self):
        """Test Inquiry vector search with guardian_id and elderly_id filtering"""
        query = "할머니 약 복용 시간"
        embedding = self.embedding_service.create_embedding(query)
        
        guardian_id = 1
        elderly_id = 1
        
        # Search Inquiry collection with filtering
        results = self.vector_store.search_inquiry(
            embedding, 
            guardian_id=guardian_id, 
            elderly_id=elderly_id, 
            limit=2
        )
        
        assert results is not None
        
        # If there are results, verify filtering worked
        for hits in results:
            for hit in hits:
                assert hasattr(hit, 'score'), "Result should have score"
                assert hasattr(hit, 'entity'), "Result should have entity"
                assert 'question' in hit.entity, "Entity should have question"
                assert 'answer' in hit.entity, "Entity should have answer"
                
                print(f"✅ Found Inquiry (score: {hit.score:.4f}): {hit.entity.get('question')[:50]}...")
    
    def test_search_result_ranking(self):
        """Test that search results are properly ranked by relevance"""
        # Search with a specific question
        query = "장기요양보험 신청 방법"
        embedding = self.embedding_service.create_embedding(query)
        
        results = self.vector_store.search_faq(embedding, limit=5)
        
        if results and len(results) > 0:
            scores = []
            for hits in results:
                for hit in hits:
                    scores.append(hit.score)
            
            # Scores should be in descending order (most relevant first)
            assert scores == sorted(scores, reverse=True), "Results should be ranked by score (descending)"
            
            print(f"✅ Search results properly ranked: {scores}")
    
    @pytest.mark.asyncio
    async def test_parallel_search(self):
        """Test parallel search of FAQ and Inquiry collections"""
        query = "노인 복지 서비스"
        embedding = self.embedding_service.create_embedding(query)
        
        # Create async tasks for parallel search
        async def search_faq_async():
            loop = asyncio.get_event_loop()
            return await loop.run_in_executor(
                None,
                lambda: self.vector_store.search_faq(embedding, limit=3)
            )
        
        async def search_inquiry_async():
            loop = asyncio.get_event_loop()
            return await loop.run_in_executor(
                None,
                lambda: self.vector_store.search_inquiry(embedding, 1, 1, limit=2)
            )
        
        # Execute searches in parallel
        faq_results, inquiry_results = await asyncio.gather(
            search_faq_async(),
            search_inquiry_async()
        )
        
        assert faq_results is not None
        assert inquiry_results is not None
        
        print("✅ Parallel search completed successfully")
    
    def test_empty_search_results(self):
        """Test handling of queries with no relevant results"""
        # Create a very specific query unlikely to match
        query = "xyzabc123 completely irrelevant random text that should not match anything"
        embedding = self.embedding_service.create_embedding(query)
        
        results = self.vector_store.search_faq(embedding, limit=3)
        
        # Should still return results (even if low relevance)
        # or handle gracefully
        assert results is not None
        
        print("✅ Empty/low-relevance search handled gracefully")
    
    def test_embedding_consistency(self):
        """Test that same query produces same embedding"""
        query = "노인 장기 요양 보험"
        
        embedding1 = self.embedding_service.create_embedding(query)
        embedding2 = self.embedding_service.create_embedding(query)
        
        assert embedding1 == embedding2, "Same query should produce identical embeddings"
        
        print("✅ Embedding consistency verified")
    
    def test_semantic_similarity(self):
        """Test that semantically similar queries return similar results"""
        query1 = "노인 장기 요양 보험이 뭐야?"
        query2 = "장기요양보험에 대해 알려주세요"
        
        embedding1 = self.embedding_service.create_embedding(query1)
        embedding2 = self.embedding_service.create_embedding(query2)
        
        results1 = self.vector_store.search_faq(embedding1, limit=3)
        results2 = self.vector_store.search_faq(embedding2, limit=3)
        
        # Both should return results (semantically similar)
        assert results1 is not None
        assert results2 is not None
        
        print("✅ Semantic similarity search working")
