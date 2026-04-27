import pytest
import os
from unittest.mock import Mock, patch

@pytest.fixture
def mock_response():
    resp = Mock()
    resp.json.return_value = [
        {"faqId": 1, "question": "Q1", "answerText": "A1", "category": "General"}
    ]
    resp.raise_for_status = Mock()
    return resp

@pytest.mark.skipif(not os.getenv("OPENAI_API_KEY"), reason="Env vars missing")
def test_sync_all_faqs(mock_response):
    """DataSyncService.sync_all_faqs 단위 테스트"""
    
    with patch("app.chatbot.services.data_sync_service.requests.get", return_value=mock_response), \
         patch("app.chatbot.services.data_sync_service.EmbeddingService") as MockEmbed, \
         patch("app.chatbot.services.data_sync_service.VectorStoreService") as MockStore:
        
        # Setup mocks
        mock_embed_instance = MockEmbed.return_value
        mock_embed_instance.create_embedding.return_value = [0.1] * 1536
        
        mock_store_instance = MockStore.return_value
        
        # Execute
        from app.chatbot.services.data_sync_service import DataSyncService
        service = DataSyncService()
        service.sync_all_faqs()
        
        # Verify
        mock_embed_instance.create_embedding.assert_called()
        mock_store_instance.insert_faq.assert_called_once()
        
        # Verify insert arguments format
        call_args = mock_store_instance.insert_faq.call_args[0][0]
        assert len(call_args) == 5 # ids, embeddings, categories, questions, answers
        assert call_args[0] == [1] # IDs
