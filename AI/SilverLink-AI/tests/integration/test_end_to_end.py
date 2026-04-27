import pytest
import requests
import asyncio
import os
from fastapi.testclient import TestClient



@pytest.mark.e2e
@pytest.mark.integration
@pytest.mark.skipif(not os.getenv("OPENAI_API_KEY"), reason="Env vars missing")
class TestEndToEnd:
    """End-to-end integration flow tests"""
    
    @pytest.fixture(autouse=True)
    def setup(self, python_ai_url, spring_boot_url):
        """Setup for each test"""
        self.python_url = python_ai_url
        self.spring_url = spring_boot_url
        from app.main import app
        self.client = TestClient(app)
    
    @pytest.mark.skip(reason="Requires FAQ data in database")
    def test_faq_sync_to_search_flow(self):
        """Test complete FAQ sync → search → response flow"""
        # Step 1: Sync FAQs
        sync_response = requests.post(f"{self.python_url}/sync/faqs")
        
        assert sync_response.status_code == 200
        sync_data = sync_response.json()
        assert sync_data["status"] == "success"
        
        print("✅ Step 1: FAQ sync completed")
        
        # Step 2: Search via chat
        chat_payload = {
            "message": "노인 장기 요양 보험이 뭐야?",
            "thread_id": "e2e_test_1",
            "guardian_id": 1,
            "elderly_id": 1
        }
        
        chat_response = self.client.post("/chat", json=chat_payload)
        
        assert chat_response.status_code == 200
        chat_data = chat_response.json()
        
        assert "answer" in chat_data
        assert len(chat_data["answer"]) > 0
        assert "sources" in chat_data
        
        print("✅ Step 2: Chat search completed")
        print(f"   Answer: {chat_data['answer'][:100]}...")
    
    @pytest.mark.skip(reason="Requires both servers running")
    def test_spring_to_python_communication(self):
        """Test Spring Boot → Python AI server communication"""
        # This requires Spring Boot server to be running
        
        payload = {
            "guardianId": 1,
            "elderlyId": 1,
            "message": "노인 복지 서비스는?"
        }
        
        try:
            response = requests.post(
                f"{self.spring_url}/api/chatbot/chat",
                json=payload,
                timeout=10
            )
            
            # Should succeed if both servers are running
            assert response.status_code in [200, 401, 403], \
                f"Unexpected status: {response.status_code}"
            
            if response.status_code == 200:
                data = response.json()
                assert "answer" in data
                print("✅ Spring Boot → Python communication successful")
            else:
                print(f"⚠️ Authentication required (status: {response.status_code})")
                
        except requests.exceptions.ConnectionError:
            pytest.skip("Spring Boot server not running")
    
    @pytest.mark.asyncio
    async def test_continuous_conversation(self):
        """Test continuous conversation scenario (3-5 turns)"""
        thread_id = "e2e_conversation"
        guardian_id = 1
        elderly_id = 1
        
        conversation = [
            "노인 장기 요양 보험이 뭐야?",
            "신청 방법은?",
            "필요한 서류는?",
            "비용은 얼마나 드나요?",
            "등급은 어떻게 나뉘나요?"
        ]
        
        for i, message in enumerate(conversation):
            payload = {
                "message": message,
                "thread_id": thread_id,
                "guardian_id": guardian_id,
                "elderly_id": elderly_id
            }
            
            response = self.client.post("/chat", json=payload)
            
            assert response.status_code == 200, \
                f"Turn {i+1} failed with status {response.status_code}"
            
            data = response.json()
            assert "answer" in data
            assert len(data["answer"]) > 0
            
            print(f"✅ Turn {i+1}/{len(conversation)}: {message[:30]}...")
            
            # Small delay between messages
            await asyncio.sleep(0.5)
        
        print(f"✅ Continuous conversation completed: {len(conversation)} turns")
    
    @pytest.mark.asyncio
    @pytest.mark.slow
    async def test_multi_user_concurrent_requests(self):
        """Test multiple users making concurrent requests"""
        # Simulate 5 concurrent users
        async def make_request(user_id: int):
            payload = {
                "message": f"사용자 {user_id}의 질문: 노인 복지 서비스는?",
                "thread_id": f"user_{user_id}",
                "guardian_id": user_id,
                "elderly_id": user_id
            }
            
            response = self.client.post("/chat", json=payload)
            return response.status_code, user_id
        
        # Create tasks for 5 concurrent users
        tasks = [make_request(i) for i in range(1, 6)]
        results = await asyncio.gather(*tasks)
        
        # All should succeed
        for status, user_id in results:
            assert status == 200, f"User {user_id} request failed with {status}"
        
        print(f"✅ Concurrent requests test: {len(results)} users processed successfully")
    
    @pytest.mark.asyncio
    async def test_complete_user_journey(self):
        """Test complete user journey simulation"""
        thread_id = "complete_journey"
        guardian_id = 1
        elderly_id = 1
        
        # Journey steps
        journey = [
            {
                "step": "Initial inquiry",
                "message": "노인 장기 요양 보험에 대해 알고 싶어요"
            },
            {
                "step": "Follow-up question",
                "message": "신청은 어떻게 하나요?"
            },
            {
                "step": "Specific detail",
                "message": "필요한 서류는 무엇인가요?"
            },
            {
                "step": "Cost inquiry",
                "message": "비용은 얼마나 드나요?"
            },
            {
                "step": "Final clarification",
                "message": "감사합니다. 정리해서 다시 설명해주세요"
            }
        ]
        
        for item in journey:
            payload = {
                "message": item["message"],
                "thread_id": thread_id,
                "guardian_id": guardian_id,
                "elderly_id": elderly_id
            }
            
            response = self.client.post("/chat", json=payload)
            
            assert response.status_code == 200, \
                f"{item['step']} failed"
            
            data = response.json()
            assert "answer" in data
            
            print(f"✅ {item['step']}: Success")
            
            await asyncio.sleep(0.3)
        
        print(f"✅ Complete user journey test passed: {len(journey)} steps")
    
    def test_health_check_endpoints(self):
        """Test health check endpoints"""
        # Python AI server health
        response = self.client.get("/health")
        assert response.status_code == 200
        data = response.json()
        assert data["status"] == "healthy"
        
        print("✅ Python AI health check passed")
        
        # Root endpoint
        root_response = self.client.get("/")
        assert root_response.status_code == 200
        
        print("✅ Root endpoint check passed")
    
    @pytest.mark.skip(reason="Requires both servers and data")
    def test_full_system_integration(self):
        """Test full system integration with all components"""
        # This is a comprehensive test that requires:
        # 1. MySQL database with FAQ/Inquiry data
        # 2. Milvus with indexed data
        # 3. Python AI server running
        # 4. Spring Boot server running
        
        # Step 1: Verify Python server
        health = requests.get(f"{self.python_url}/health")
        assert health.status_code == 200
        
        # Step 2: Verify Spring Boot server
        # (would need appropriate endpoint)
        
        # Step 3: Test data sync
        sync = requests.post(f"{self.python_url}/sync/faqs")
        assert sync.status_code == 200
        
        # Step 4: Test chat through Spring Boot proxy
        payload = {
            "guardianId": 1,
            "elderlyId": 1,
            "message": "통합 테스트 질문"
        }
        
        chat = requests.post(f"{self.spring_url}/api/chatbot/chat", json=payload)
        # May require authentication
        assert chat.status_code in [200, 401, 403]
        
        print("✅ Full system integration test completed")
