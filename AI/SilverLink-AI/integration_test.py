import requests
import sys

# Configurations
SPRING_BOOT_URL = "http://localhost:8080"
PYTHON_CHATBOT_URL = "http://localhost:8000"

# Colors for output
GREEN = "\033[92m"
RED = "\033[91m"
RESET = "\033[0m"

def log(message, success=True):
    color = GREEN if success else RED
    print(f"{color}[{'SUCCESS' if success else 'FAILED'}] {message}{RESET}")

def test_python_health():
    """Python 서버 상태 확인"""
    try:
        res = requests.get(f"{PYTHON_CHATBOT_URL}/health")
        if res.status_code == 200:
            log("Python Chatbot Server is Healthy")
            return True
        else:
            log(f"Python Health Check Failed: {res.status_code}", False)
            return False
    except Exception as e:
        log(f"Python Server Unreachable: {e}", False)
        return False

def test_data_sync_faqs():
    """FAQ 데이터 동기화 테스트"""
    print("\nTesting FAQ Sync...")
    try:
        res = requests.post(f"{PYTHON_CHATBOT_URL}/sync/faqs")
        if res.status_code == 200:
            log("FAQ Sync Successful")
            print(f"Response: {res.json()}")
        else:
            log(f"FAQ Sync Failed: {res.status_code} - {res.text}", False)
    except Exception as e:
        log(f"FAQ Sync Error: {e}", False)

def test_data_sync_inquiries():
    """Inquiry 데이터 동기화 테스트"""
    print("\nTesting Inquiry Sync...")
    try:
        res = requests.post(f"{PYTHON_CHATBOT_URL}/sync/inquiries")
        if res.status_code == 200:
            log("Inquiry Sync Successful")
            print(f"Response: {res.json()}")
        else:
            log(f"Inquiry Sync Failed: {res.status_code} - {res.text}", False)
    except Exception as e:
        log(f"Inquiry Sync Error: {e}", False)

def test_direct_python_chat():
    """Python 챗봇 직접 호출 테스트"""
    print("\nTesting Direct Python Chat Endpoint...")
    payload = {
        "message": "안녕하세요, 약 먹는 시간 알려주세요.",
        "thread_id": "test_integration_thread",
        "guardian_id": 1,
        "elderly_id": 1
    }
    
    try:
        res = requests.post(f"{PYTHON_CHATBOT_URL}/chat", json=payload)
        if res.status_code == 200:
            data = res.json()
            log("Direct Chat Successful")
            print(f"Answer: {data.get('answer')}")
            print(f"Sources: {data.get('sources')}")
        else:
            log(f"Direct Chat Failed: {res.status_code} - {res.text}", False)
    except Exception as e:
        log(f"Direct Chat Error: {e}", False)

def main():
    print("=== SilverLink Integration Test ===")
    
    # 1. Health Check
    if not test_python_health():
        sys.exit(1)
        
    # 2. Data Sync Test (requires Spring Boot running)
    test_data_sync_faqs()
    test_data_sync_inquiries()
    
    # 3. Chat Logic Test
    test_direct_python_chat()
    
    # 4. Spring Boot Proxy Test (Full E2E)
    test_spring_boot_proxy_chat()

def test_spring_boot_proxy_chat():
    """Spring Boot를 통한 챗봇 호출 테스트 (E2E)"""
    print("\nTesting Spring Boot Proxy Chat (Full E2E)...")
    
    # Note: 실제 인증 토큰이 필요한 경우 여기에 추가
    payload = {
        "message": "어르신 건강 상태는 어떻게 확인하나요?",
        "guardianId": 1,
        "elderlyId": 1
    }
    
    try:
        # Spring Boot의 /api/chatbot/chat 엔드포인트 호출
        res = requests.post(
            f"{SPRING_BOOT_URL}/api/chatbot/chat",
            json=payload,
            # headers={"Authorization": "Bearer YOUR_TOKEN"}  # 필요시 추가
        )
        
        if res.status_code == 200:
            data = res.json()
            log("Spring Boot Proxy Chat Successful")
            print(f"Answer: {data.get('answer')}")
            print(f"Thread ID: {data.get('threadId')}")
            print(f"Sources: {data.get('sources')}")
            print(f"Confidence: {data.get('confidence')}")
        elif res.status_code == 401 or res.status_code == 403:
            log(f"Spring Boot Proxy requires authentication (status {res.status_code})", False)
            print("Tip: 인증이 필요한 경우 테스트 토큰을 추가하세요.")
        else:
            log(f"Spring Boot Proxy Failed: {res.status_code} - {res.text}", False)
    except Exception as e:
        log(f"Spring Boot Proxy Error: {e}", False)

if __name__ == "__main__":
    main()
