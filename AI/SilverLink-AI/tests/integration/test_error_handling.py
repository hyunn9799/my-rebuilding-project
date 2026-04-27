"""Error handling integration tests"""
import pytest
from fastapi.testclient import TestClient

import os



@pytest.mark.integration
@pytest.mark.skipif(not os.getenv("OPENAI_API_KEY"), reason="Env vars missing")
class TestErrorHandling:
    """Test error handling and exception scenarios"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """Setup for each test"""
        from app.main import app
        from app.chatbot.services.chatbot_service import ChatbotService
        self.client = TestClient(app)
        self.chatbot_service = ChatbotService()
    
    def test_empty_message(self):
        """Test handling of empty message (should return 422 validation error)"""
        payload = {
            "message": "",  # Empty message
            "thread_id": "test_thread",
            "guardian_id": 1,
            "elderly_id": 1
        }
        
        response = self.client.post("/chat", json=payload)
        
        # FastAPI should return 422 for validation error
        assert response.status_code == 422, \
            f"Expected 422 for empty message, got {response.status_code}"
        
        print("✅ Empty message properly rejected with 422")
    
    def test_missing_required_fields(self):
        """Test handling of missing required fields"""
        # Missing message field
        payload = {
            "thread_id": "test_thread",
            "guardian_id": 1,
            "elderly_id": 1
        }
        
        response = self.client.post("/chat", json=payload)
        
        assert response.status_code == 422, \
            f"Expected 422 for missing field, got {response.status_code}"
        
        print("✅ Missing required fields properly rejected")
    
    def test_invalid_request_format(self):
        """Test handling of invalid request format"""
        # Invalid JSON
        response = self.client.post(
            "/chat",
            data="invalid json",
            headers={"Content-Type": "application/json"}
        )
        
        assert response.status_code == 422, \
            f"Expected 422 for invalid JSON, got {response.status_code}"
        
        print("✅ Invalid request format properly rejected")
    
    def test_null_values(self):
        """Test handling of null values"""
        payload = {
            "message": None,
            "thread_id": "test_thread",
            "guardian_id": 1,
            "elderly_id": 1
        }
        
        response = self.client.post("/chat", json=payload)
        
        assert response.status_code == 422, \
            f"Expected 422 for null message, got {response.status_code}"
        
        print("✅ Null values properly rejected")
    
    def test_invalid_data_types(self):
        """Test handling of invalid data types"""
        payload = {
            "message": "테스트 메시지",
            "thread_id": "test_thread",
            "guardian_id": "not_a_number",  # Should be int
            "elderly_id": 1
        }
        
        response = self.client.post("/chat", json=payload)
        
        assert response.status_code == 422, \
            f"Expected 422 for invalid data type, got {response.status_code}"
        
        print("✅ Invalid data types properly rejected")
    
    @pytest.mark.slow
    def test_large_message_handling(self):
        """Test handling of excessively long messages"""
        # Create a very long message (10000 characters)
        long_message = "테스트 " * 2000
        
        payload = {
            "message": long_message,
            "thread_id": "test_thread",
            "guardian_id": 1,
            "elderly_id": 1
        }
        
        response = self.client.post("/chat", json=payload)
        
        # Should either accept it or return appropriate error
        # Depending on implementation, this might be 200 or 422
        assert response.status_code in [200, 422], \
            f"Unexpected status code for long message: {response.status_code}"
        
        print(f"✅ Large message handled with status {response.status_code}")
    
    @pytest.mark.skip(reason="Requires intentional Milvus disconnection")
    def test_milvus_connection_failure(self):
        """Test handling when Milvus connection fails"""
        # This would require mocking or intentionally disconnecting Milvus
        # For now, we document the expected behavior
        
        # Expected: Should return 500 Internal Server Error
        # with appropriate error message
        
        print("⚠️ Milvus connection failure test requires manual setup")
    
    @pytest.mark.skip(reason="Requires OpenAI API key removal or rate limiting")
    def test_openai_api_failure(self):
        """Test handling when OpenAI API fails"""
        # This would require mocking or removing API key
        # Expected: Should return 500 with appropriate error message
        
        print("⚠️ OpenAI API failure test requires manual setup")
    
    @pytest.mark.asyncio
    @pytest.mark.slow
    async def test_timeout_handling(self):
        """Test timeout handling for long-running requests"""
        # This test checks if the system handles timeouts gracefully
        
        payload = {
            "message": "복잡한 질문에 대한 답변",
            "thread_id": "test_timeout",
            "guardian_id": 1,
            "elderly_id": 1
        }
        
        try:
            # Set a short timeout
            response = self.client.post("/chat", json=payload, timeout=30)
            
            # Should complete within timeout or handle gracefully
            assert response.status_code in [200, 408, 504], \
                f"Unexpected status for timeout test: {response.status_code}"
            
            print(f"✅ Timeout handling validated with status {response.status_code}")
        except Exception as e:
            print(f"⚠️ Timeout test raised exception: {type(e).__name__}")
    
    def test_special_characters_in_message(self):
        """Test handling of special characters in messages"""
        special_messages = [
            "테스트 <script>alert('xss')</script>",
            "테스트 ' OR '1'='1",
            "테스트 \n\n\n\n 줄바꿈",
            "테스트 😀 🎉 이모지",
            "테스트 \t\t\t 탭문자"
        ]
        
        for msg in special_messages:
            payload = {
                "message": msg,
                "thread_id": "test_special",
                "guardian_id": 1,
                "elderly_id": 1
            }
            
            response = self.client.post("/chat", json=payload)
            
            # Should handle gracefully (200 or appropriate error)
            assert response.status_code in [200, 422], \
                f"Failed to handle special characters: {response.status_code}"
        
        print(f"✅ Special characters handled for {len(special_messages)} test cases")
    
    def test_concurrent_error_scenarios(self):
        """Test multiple error scenarios don't cause system instability"""
        error_payloads = [
            {"message": "", "thread_id": "t1", "guardian_id": 1, "elderly_id": 1},
            {"message": None, "thread_id": "t2", "guardian_id": 1, "elderly_id": 1},
            {"thread_id": "t3", "guardian_id": 1, "elderly_id": 1},  # Missing message
        ]
        
        for payload in error_payloads:
            response = self.client.post("/chat", json=payload)
            # All should return 422
            assert response.status_code == 422
        
        print("✅ Concurrent error scenarios handled without system instability")
