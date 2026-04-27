"""
Enhanced Integration Test Script for SilverLink Chatbot
Tests both Python AI service and Spring Boot backend integration
"""
import requests
import json
import time
import pytest
from typing import Dict, Any

PYTHON_URL = "http://localhost:5000"
SPRING_URL = "http://localhost:5080"

class Colors:
    """ANSI color codes for terminal output"""
    GREEN = '\033[92m'
    RED = '\033[91m'
    YELLOW = '\033[93m'
    BLUE = '\033[94m'
    RESET = '\033[0m'

def print_success(msg: str):
    print(f"{Colors.GREEN}[OK] {msg}{Colors.RESET}")

def print_error(msg: str):
    print(f"{Colors.RED}[ERROR] {msg}{Colors.RESET}")

def print_warning(msg: str):
    print(f"{Colors.YELLOW}[WARN] {msg}{Colors.RESET}")

def print_info(msg: str):
    print(f"{Colors.BLUE}[INFO] {msg}{Colors.RESET}")

def print_section(title: str):
    print(f"\n{Colors.BLUE}{'='*60}")
    print(f"{title}")
    print(f"{'='*60}{Colors.RESET}\n")

@pytest.mark.skip(reason="Requires running servers - manual integration test")
def test_python_health():
    """Test Python AI service health check"""
    print_section("[1] Testing Python AI Service Health")
    try:
        response = requests.get(f"{PYTHON_URL}/health", timeout=5)
        print_info(f"Status Code: {response.status_code}")
        
        if response.status_code == 200:
            data = response.json()
            print_info(f"Response: {json.dumps(data, ensure_ascii=False)}")
            print_success("Python AI service is healthy")
            return True
        else:
            print_error("Python AI service health check failed")
            return False
    except Exception as e:
        print_error(f"Connection Error: {e}")
        return False

@pytest.mark.skip(reason="Requires running servers - manual integration test")
def test_sync_faqs():
    """Test FAQ synchronization"""
    print_section("[2] Testing FAQ Sync")
    try:
        start_time = time.time()
        response = requests.post(f"{PYTHON_URL}/sync/faqs", timeout=30)
        elapsed = time.time() - start_time
        
        print_info(f"Status Code: {response.status_code}")
        print_info(f"Time: {elapsed:.2f}s")
        print_info(f"Response: {response.text}")
        
        if response.status_code == 200:
            print_success(f"FAQ Sync Successful (took {elapsed:.2f}s)")
            return True
        else:
            print_error("FAQ Sync Failed")
            return False
    except Exception as e:
        print_error(f"Connection Error: {e}")
        return False

@pytest.mark.skip(reason="Requires running servers - manual integration test")
def test_chat_direct(message: str, thread_id: str = "test_thread_1") -> Dict[str, Any]:
    """Test direct chat to Python AI service"""
    print_section(f"[3] Testing Direct Chat: '{message}'")
    
    payload = {
        "message": message,
        "thread_id": thread_id,
        "guardian_id": 1,
        "elderly_id": 1
    }
    
    try:
        start_time = time.time()
        response = requests.post(f"{PYTHON_URL}/chat", json=payload, timeout=30)
        elapsed = time.time() - start_time
        
        print_info(f"Status Code: {response.status_code}")
        print_info(f"Response Time: {elapsed:.2f}s")
        
        if response.status_code == 200:
            data = response.json()
            print_info(f"Answer: {data.get('answer', '')[:200]}...")
            print_info(f"Sources: {data.get('sources', [])}")
            print_info(f"Confidence: {data.get('confidence', 0):.4f}")
            print_success(f"Chat Request Successful (took {elapsed:.2f}s)")
            return data
        else:
            print_error(f"Chat Request Failed: {response.text}")
            return None
    except Exception as e:
        print_error(f"Connection Error: {e}")
        return None

@pytest.mark.skip(reason="Requires running servers - manual integration test")
def test_chat_proxy():
    """Test chat through Spring Boot proxy"""
    print_section("[4] Testing Chat Proxy (Spring Boot → Python)")
    
    payload = {
        "guardianId": 1,
        "elderlyId": 1,
        "message": "노인 장기 요양 보험이 뭐야?"
    }
    
    headers = {
        "Content-Type": "application/json"
    }

    try:
        start_time = time.time()
        response = requests.post(f"{SPRING_URL}/api/chatbot/chat", json=payload, headers=headers, timeout=30)
        elapsed = time.time() - start_time
        
        print_info(f"Status Code: {response.status_code}")
        print_info(f"Response Time: {elapsed:.2f}s")
        
        try:
            data = response.json()
            print_info(f"Response: {json.dumps(data, indent=2, ensure_ascii=False)[:300]}...")
        except Exception:
            print_info(f"Response Text: {response.text[:200]}")

        if response.status_code == 200:
            print_success("Chat Proxy Request Successful")
            return True
        elif response.status_code in [403, 401]:
            print_warning("Auth Error - You may need to log in or disable security for testing")
            return False
        else:
            print_error("Chat Proxy Request Failed")
            return False
            
    except Exception as e:
        print_error(f"Connection Error to Spring Boot: {e}")
        return False

@pytest.mark.skip(reason="Requires running servers - manual integration test")
def test_continuous_conversation():
    """Test continuous conversation with context"""
    print_section("[5] Testing Continuous Conversation")
    
    thread_id = "continuous_test"
    messages = [
        "노인 장기 요양 보험이 뭐야?",
        "신청 방법은?",
        "필요한 서류는?"
    ]
    
    for i, msg in enumerate(messages, 1):
        print_info(f"Turn {i}/{len(messages)}: {msg}")
        result = test_chat_direct(msg, thread_id)
        
        if result:
            print_success(f"Turn {i} completed")
        else:
            print_error(f"Turn {i} failed")
            return False
        
        time.sleep(1)  # Small delay between messages
    
    print_success("Continuous conversation test completed")
    return True

@pytest.mark.skip(reason="Requires running servers - manual integration test")
def test_error_cases():
    """Test error handling"""
    print_section("[6] Testing Error Cases")
    
    # Test empty message
    print_info("Testing empty message...")
    try:
        response = requests.post(f"{PYTHON_URL}/chat", json={
            "message": "",
            "thread_id": "error_test",
            "guardian_id": 1,
            "elderly_id": 1
        })
        
        if response.status_code == 422:
            print_success("Empty message properly rejected (422)")
        else:
            print_warning(f"Unexpected status for empty message: {response.status_code}")
    except Exception as e:
        print_error(f"Error test failed: {e}")

def run_all_tests():
    """Run all integration tests"""
    print(f"\n{Colors.BLUE}{'='*60}")
    print("SilverLink Chatbot Integration Tests")
    print(f"{'='*60}{Colors.RESET}\n")
    
    results = {}
    
    # Test 1: Python Health
    results['python_health'] = test_python_health()
    
    # Test 2: FAQ Sync (optional, may take time)
    # results['faq_sync'] = test_sync_faqs()
    
    # Test 3: Direct Chat
    results['direct_chat'] = test_chat_direct("노인 장기 요양 보험이 뭐야?") is not None
    
    # Test 4: Spring Boot Proxy (optional, requires Spring Boot)
    # results['spring_proxy'] = test_chat_proxy()
    
    # Test 5: Continuous Conversation
    results['continuous'] = test_continuous_conversation()
    
    # Test 6: Error Cases
    test_error_cases()
    
    # Summary
    print_section("Test Summary")
    passed = sum(results.values())
    total = len(results)
    
    for test_name, passed_flag in results.items():
        status = "PASSED" if passed_flag else "FAILED"
        color = Colors.GREEN if passed_flag else Colors.RED
        print(f"{color}{test_name}: {status}{Colors.RESET}")
    
    print(f"\n{Colors.BLUE}Total: {passed}/{total} tests passed{Colors.RESET}\n")
    
    return passed == total

if __name__ == "__main__":
    success = run_all_tests()
    exit(0 if success else 1)

