from pydantic import BaseModel
from typing import List

class ChatRequest(BaseModel):
    message: str
    thread_id: str
    guardian_id: int
    elderly_id: int
    model_config = {
        "json_schema_extra": {
            "example": {
                "message": "어르신이 갑자기 열이 나시는데 어떻게 해야 하나요?",
                "thread_id": "emergency_test_01",
                "guardian_id": 1,
                "elderly_id": 100
            }
        }
    }

class ChatbotRequest(BaseModel):
    message: str
    thread_id: str
    guardian_id: int
    elderly_id: int

class ChatResponse(BaseModel):
    answer: str
    thread_id: str
    sources: List[str] = []
    confidence: float = 0.0
