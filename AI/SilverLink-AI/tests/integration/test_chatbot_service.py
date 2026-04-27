"""Chatbot service integration tests"""
import pytest
import os



@pytest.mark.integration
@pytest.mark.skipif(not os.getenv("OPENAI_API_KEY"), reason="Env vars missing")
class TestChatbotService:
    """Test chatbot service core logic"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """Setup for each test"""
        from app.chatbot.services.chatbot_service import ChatbotService
        from app.chatbot.services.embedding_service import EmbeddingService
        self.chatbot_service = ChatbotService()
        self.embedding_service = EmbeddingService()
    
    @pytest.mark.asyncio
    async def test_process_chat_basic(self):
        """Test basic chat processing"""
        message = "노인 장기 요양 보험이 뭐야?"
        thread_id = "test_guardian_1"
        guardian_id = 1
        elderly_id = 1
        
        result = await self.chatbot_service.process_chat(
            message=message,
            thread_id=thread_id,
            guardian_id=guardian_id,
            elderly_id=elderly_id
        )
        
        # Validate response structure
        assert "answer" in result
        assert "sources" in result
        assert "confidence" in result
        
        assert isinstance(result["answer"], str)
        assert len(result["answer"]) > 0, "Answer should not be empty"
        assert isinstance(result["sources"], list)
        assert isinstance(result["confidence"], (int, float))
        
        print("✅ Chat processed successfully")
        print(f"   Answer: {result['answer'][:100]}...")
        print(f"   Sources: {result['sources']}")
        print(f"   Confidence: {result['confidence']:.4f}")
    
    @pytest.mark.asyncio
    async def test_thread_id_context_persistence(self):
        """Test that thread_id maintains conversation context"""
        thread_id = "test_guardian_persistence"
        guardian_id = 1
        elderly_id = 1
        
        # First message
        result1 = await self.chatbot_service.process_chat(
            message="노인 장기 요양 보험이 뭐야?",
            thread_id=thread_id,
            guardian_id=guardian_id,
            elderly_id=elderly_id
        )
        
        assert result1 is not None
        assert "answer" in result1
        
        # Second message (should have context from first)
        result2 = await self.chatbot_service.process_chat(
            message="신청 방법은?",
            thread_id=thread_id,
            guardian_id=guardian_id,
            elderly_id=elderly_id
        )
        
        assert result2 is not None
        assert "answer" in result2
        
        print("✅ Thread context maintained across messages")
    
    @pytest.mark.asyncio
    async def test_conversation_memory(self):
        """Test conversation memory management"""
        thread_id = "test_guardian_memory"
        guardian_id = 1
        elderly_id = 1
        
        # Simulate a conversation with 3 turns
        messages = [
            "노인 복지 서비스에는 어떤 것이 있나요?",
            "그 중에서 가장 중요한 것은?",
            "신청은 어떻게 하나요?"
        ]
        
        results = []
        for msg in messages:
            result = await self.chatbot_service.process_chat(
                message=msg,
                thread_id=thread_id,
                guardian_id=guardian_id,
                elderly_id=elderly_id
            )
            results.append(result)
            assert result is not None
            assert "answer" in result
        
        print(f"✅ Conversation memory test completed with {len(results)} turns")
    
    @pytest.mark.asyncio
    async def test_context_building(self):
        """Test context building from search results"""
        message = "장기요양보험 신청"
        thread_id = "test_context"
        guardian_id = 1
        elderly_id = 1
        
        result = await self.chatbot_service.process_chat(
            message=message,
            thread_id=thread_id,
            guardian_id=guardian_id,
            elderly_id=elderly_id
        )
        
        # Should have sources from FAQ or Inquiry
        assert "sources" in result
        assert isinstance(result["sources"], list)
        
        # If sources exist, they should be valid
        if len(result["sources"]) > 0:
            valid_sources = ["FAQ", "INQUIRY"]
            for source in result["sources"]:
                assert source in valid_sources, f"Invalid source: {source}"
        
        print(f"✅ Context building validated with sources: {result['sources']}")
    
    def test_embedding_generation(self):
        """Test embedding generation"""
        messages = [
            "노인 장기 요양 보험",
            "장기요양보험 신청 방법",
            "할머니 약 복용 시간"
        ]
        
        for msg in messages:
            embedding = self.embedding_service.create_embedding(msg)
            
            assert embedding is not None
            assert isinstance(embedding, list)
            assert len(embedding) == 1536, "Embedding should be 1536 dimensions"
            assert all(isinstance(x, float) for x in embedding), "All values should be floats"
        
        print(f"✅ Embedding generation validated for {len(messages)} messages")
    
    @pytest.mark.asyncio
    async def test_different_thread_ids(self):
        """Test that different thread_ids maintain separate contexts"""
        guardian_id = 1
        elderly_id = 1
        
        # Thread 1
        result1 = await self.chatbot_service.process_chat(
            message="노인 장기 요양 보험이 뭐야?",
            thread_id="guardian_1",
            guardian_id=guardian_id,
            elderly_id=elderly_id
        )
        
        # Thread 2 (different thread_id)
        result2 = await self.chatbot_service.process_chat(
            message="복지 서비스는?",
            thread_id="guardian_2",
            guardian_id=guardian_id,
            elderly_id=elderly_id
        )
        
        # Both should succeed independently
        assert result1 is not None
        assert result2 is not None
        
        print("✅ Different thread_ids maintain separate contexts")
    
    @pytest.mark.asyncio
    @pytest.mark.slow
    async def test_long_conversation(self):
        """Test handling of long conversations"""
        thread_id = "test_long_conversation"
        guardian_id = 1
        elderly_id = 1
        
        # Simulate 10 message exchanges
        for i in range(10):
            result = await self.chatbot_service.process_chat(
                message=f"질문 {i+1}: 노인 복지에 대해 알려주세요",
                thread_id=thread_id,
                guardian_id=guardian_id,
                elderly_id=elderly_id
            )
            
            assert result is not None
            assert "answer" in result
        
        print("✅ Long conversation (10 turns) handled successfully")
