"""Test helper utilities for integration tests"""
import time
from faker import Faker
from functools import wraps
from typing import Dict, Any, List
import logging
import asyncio

logger = logging.getLogger(__name__)


def measure_response_time(func):
    """Decorator to measure response time of a function"""
    @wraps(func)
    async def async_wrapper(*args, **kwargs):
        start_time = time.time()
        result = await func(*args, **kwargs)
        end_time = time.time()
        elapsed = end_time - start_time
        logger.info(f"{func.__name__} took {elapsed:.3f} seconds")
        return result, elapsed
    
    @wraps(func)
    def sync_wrapper(*args, **kwargs):
        start_time = time.time()
        result = func(*args, **kwargs)
        end_time = time.time()
        elapsed = end_time - start_time
        logger.info(f"{func.__name__} took {elapsed:.3f} seconds")
        return result, elapsed
    
    if asyncio.iscoroutinefunction(func):
        return async_wrapper
    return sync_wrapper


def assert_response_structure(response: Dict[str, Any], required_fields: List[str]):
    """Assert that response has required fields"""
    for field in required_fields:
        assert field in response, f"Missing required field: {field}"


def assert_chat_response_valid(response: Dict[str, Any]):
    """Validate chat response structure"""
    required_fields = ["answer", "thread_id", "sources", "confidence"]
    assert_response_structure(response, required_fields)
    
    assert isinstance(response["answer"], str), "answer must be string"
    assert isinstance(response["thread_id"], str), "thread_id must be string"
    assert isinstance(response["sources"], list), "sources must be list"
    assert isinstance(response["confidence"], (int, float)), "confidence must be numeric"
    assert len(response["answer"]) > 0, "answer cannot be empty"


def create_test_faq_data(count: int = 5) -> List[Dict[str, Any]]:
    """Create test FAQ data"""
    fake = Faker(['ko_KR'])
    
    categories = ["노인장기요양보험", "복지서비스", "건강관리", "생활지원"]
    
    faqs = []
    for i in range(count):
        faqs.append({
            "id": i + 1,
            "category": fake.random_element(categories),
            "question": f"테스트 질문 {i+1}: {fake.sentence()}",
            "answer": f"테스트 답변 {i+1}: {fake.paragraph()}"
        })
    
    return faqs


def create_test_inquiry_data(guardian_id: int, elderly_id: int, count: int = 3) -> List[Dict[str, Any]]:
    """Create test inquiry data"""
    fake = Faker(['ko_KR'])
    
    inquiries = []
    for i in range(count):
        inquiries.append({
            "id": i + 1,
            "guardian_id": guardian_id,
            "elderly_id": elderly_id,
            "question": f"문의 {i+1}: {fake.sentence()}",
            "answer": f"답변 {i+1}: {fake.paragraph()}"
        })
    
    return inquiries


def cleanup_test_data(vector_store, collection_name: str):
    """Cleanup test data from collection"""
    try:
        from pymilvus import utility
        if utility.has_collection(collection_name):
            collection = vector_store.Collection(collection_name)
            collection.drop()
            logger.info(f"Dropped test collection: {collection_name}")
    except Exception as e:
        logger.warning(f"Failed to cleanup collection {collection_name}: {e}")


