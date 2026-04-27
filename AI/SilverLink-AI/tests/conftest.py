import pytest
import asyncio
from httpx import AsyncClient
from fastapi.testclient import TestClient
from typing import Generator, AsyncGenerator
import sys
import os

# Add src to path
sys.path.insert(0, os.path.join(os.path.dirname(__file__), '..', 'src'))




@pytest.fixture(scope="session")
def event_loop():
    """Create event loop for async tests"""
    loop = asyncio.get_event_loop_policy().new_event_loop()
    yield loop
    loop.close()


@pytest.fixture(scope="module")
def test_client() -> Generator:
    """FastAPI test client fixture"""
    from app.main import app
    with TestClient(app) as client:
        yield client


@pytest.fixture(scope="module")
async def async_test_client() -> AsyncGenerator:
    """Async FastAPI test client fixture"""
    from app.main import app
    async with AsyncClient(app=app, base_url="http://test") as client:
        yield client


@pytest.fixture(scope="session")
def milvus_connection():
    """Milvus connection fixture"""
    from app.core.config import configs
    from pymilvus import connections

    try:
        connections.connect(
            alias="default",
            uri=configs.MILVUS_URI,
            token=configs.MILVUS_TOKEN
        )
        yield connections
    finally:
        connections.disconnect("default")


@pytest.fixture(scope="function")
def vector_store_service(milvus_connection):
    """Vector store service fixture"""
    from app.chatbot.repository.chatbot_repository import ChatbotRepository
    return ChatbotRepository()


@pytest.fixture(scope="function")
def mock_guardian_elderly_data():
    """Mock Guardian-Elderly relationship data"""
    return {
        "valid_relations": [
            {"guardian_id": 1, "elderly_id": 1},
            {"guardian_id": 1, "elderly_id": 2},
            {"guardian_id": 2, "elderly_id": 3},
        ],
        "invalid_relations": [
            {"guardian_id": 1, "elderly_id": 3},  # Not related
            {"guardian_id": 2, "elderly_id": 1},  # Not related
        ]
    }


@pytest.fixture(scope="function")
def sample_faq_data():
    """Sample FAQ data for testing"""
    return [
        {
            "id": 1,
            "category": "노인장기요양보험",
            "question": "노인 장기 요양 보험이 뭐야?",
            "answer": "노인장기요양보험은 고령이나 노인성 질병 등으로 일상생활을 혼자 수행하기 어려운 노인 등에게 신체활동 또는 가사활동 지원 등의 장기요양급여를 제공하는 사회보험제도입니다."
        },
        {
            "id": 2,
            "category": "노인장기요양보험",
            "question": "장기요양보험 신청 방법은?",
            "answer": "국민건강보험공단 지사를 방문하거나 우편, 팩스, 인터넷(www.longtermcare.or.kr)을 통해 신청할 수 있습니다."
        },
        {
            "id": 3,
            "category": "복지서비스",
            "question": "노인 복지 서비스에는 어떤 것이 있나요?",
            "answer": "노인 복지 서비스에는 경로당 운영, 노인일자리 지원, 노인돌봄서비스, 치매관리 지원 등이 있습니다."
        }
    ]


@pytest.fixture(scope="function")
def sample_inquiry_data():
    """Sample Inquiry data for testing"""
    return [
        {
            "id": 1,
            "guardian_id": 1,
            "elderly_id": 1,
            "question": "할머니 약 복용 시간이 언제인가요?",
            "answer": "오전 8시, 오후 2시, 저녁 7시에 복용하시면 됩니다."
        },
        {
            "id": 2,
            "guardian_id": 1,
            "elderly_id": 1,
            "question": "할머니 병원 예약은 언제인가요?",
            "answer": "다음주 화요일 오전 10시에 서울대병원 정형외과 예약되어 있습니다."
        }
    ]


@pytest.fixture(scope="function")
def sample_chat_request():
    """Sample chat request data"""
    return {
        "message": "노인 장기 요양 보험이 뭐야?",
        "thread_id": "guardian_1",
        "guardian_id": 1,
        "elderly_id": 1
    }


@pytest.fixture(scope="function")
def cleanup_test_collections(milvus_connection):
    """Cleanup test collections after each test - use explicitly when needed"""
    yield
    # Cleanup logic can be added here if needed
    # For now, we'll keep collections for inspection
    pass


@pytest.fixture(scope="session")
def spring_boot_url():
    """Spring Boot backend URL"""
    return "http://localhost:8080"


@pytest.fixture(scope="session")
def python_ai_url():
    """Python AI service URL"""
    return "http://localhost:8000"
