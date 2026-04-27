"""Authentication and authorization integration tests"""
import pytest
import os
import requests
from tests.utils.mock_data import GUARDIAN_ELDERLY_RELATIONS


@pytest.mark.integration
@pytest.mark.skipif(not os.getenv("OPENAI_API_KEY"), reason="Env vars missing")
class TestAuthentication:
    """Test authentication and authorization"""
    
    @pytest.fixture(autouse=True)
    def setup(self, spring_boot_url):
        """Setup for each test"""
        self.spring_url = spring_boot_url
        self.chatbot_endpoint = f"{self.spring_url}/api/chatbot/chat"
    
    def test_valid_guardian_elderly_relation(self):
        """Test valid Guardian-Elderly relationship"""
        # This test requires Spring Boot to be running
        # For now, we'll test the data structure
        
        valid_relations = GUARDIAN_ELDERLY_RELATIONS["valid"]
        
        assert len(valid_relations) > 0
        
        for relation in valid_relations:
            assert "guardian_id" in relation
            assert "elderly_id" in relation
            assert isinstance(relation["guardian_id"], int)
            assert isinstance(relation["elderly_id"], int)
        
        print(f"✅ Valid relations structure verified: {len(valid_relations)} relations")
    
    @pytest.mark.skip(reason="Requires Spring Boot server running")
    def test_invalid_guardian_id(self):
        """Test request with invalid Guardian ID (should return 403)"""
        payload = {
            "guardianId": 999,  # Non-existent guardian
            "elderlyId": 1,
            "message": "테스트 메시지"
        }
        
        try:
            response = requests.post(self.chatbot_endpoint, json=payload)
            
            # Should return 403 Forbidden or 401 Unauthorized
            assert response.status_code in [401, 403], \
                f"Expected 401/403, got {response.status_code}"
            
            print(f"✅ Invalid guardian_id properly rejected with {response.status_code}")
        except requests.exceptions.ConnectionError:
            pytest.skip("Spring Boot server not running")
    
    @pytest.mark.skip(reason="Requires Spring Boot server running")
    def test_invalid_elderly_id(self):
        """Test request with invalid Elderly ID (should return 403)"""
        payload = {
            "guardianId": 1,
            "elderlyId": 999,  # Non-existent elderly
            "message": "테스트 메시지"
        }
        
        try:
            response = requests.post(self.chatbot_endpoint, json=payload)
            
            # Should return 403 Forbidden
            assert response.status_code == 403, \
                f"Expected 403, got {response.status_code}"
            
            print("✅ Invalid elderly_id properly rejected")
        except requests.exceptions.ConnectionError:
            pytest.skip("Spring Boot server not running")
    
    @pytest.mark.skip(reason="Requires Spring Boot server running")
    def test_unauthorized_access(self):
        """Test unauthorized access (no authentication)"""
        payload = {
            "guardianId": 1,
            "elderlyId": 1,
            "message": "테스트 메시지"
        }
        
        # No authentication headers
        try:
            response = requests.post(self.chatbot_endpoint, json=payload)
            
            # Should return 401 or 403
            assert response.status_code in [401, 403], \
                f"Expected 401/403, got {response.status_code}"
            
            print("✅ Unauthorized access properly rejected")
        except requests.exceptions.ConnectionError:
            pytest.skip("Spring Boot server not running")
    
    @pytest.mark.skip(reason="Requires Spring Boot server running")
    def test_guardian_id_mismatch(self):
        """Test when request guardian_id doesn't match authenticated user"""
        # Guardian 1 trying to access Guardian 2's data
        invalid_relations = GUARDIAN_ELDERLY_RELATIONS["invalid"]
        
        for relation in invalid_relations:
            payload = {
                "guardianId": relation["guardian_id"],
                "elderlyId": relation["elderly_id"],
                "message": "테스트 메시지"
            }
            
            try:
                response = requests.post(self.chatbot_endpoint, json=payload)
                
                # Should return 403 Forbidden
                assert response.status_code == 403, \
                    f"Expected 403 for invalid relation, got {response.status_code}"
                
            except requests.exceptions.ConnectionError:
                pytest.skip("Spring Boot server not running")
        
        print("✅ Guardian-Elderly mismatch properly rejected")
    
    def test_guardian_elderly_relation_data_integrity(self):
        """Test Guardian-Elderly relation data integrity"""
        valid = GUARDIAN_ELDERLY_RELATIONS["valid"]
        invalid = GUARDIAN_ELDERLY_RELATIONS["invalid"]
        
        # Check no overlap between valid and invalid
        valid_pairs = {(r["guardian_id"], r["elderly_id"]) for r in valid}
        invalid_pairs = {(r["guardian_id"], r["elderly_id"]) for r in invalid}
        
        overlap = valid_pairs & invalid_pairs
        assert len(overlap) == 0, f"Found overlapping relations: {overlap}"
        
        print("✅ Guardian-Elderly relation data integrity verified")
