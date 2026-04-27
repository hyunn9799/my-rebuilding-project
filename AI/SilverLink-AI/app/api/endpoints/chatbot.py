# --- Chatbot & Data Sync Routes ---
from pymilvus import connections
from app.chatbot.schema.chat_schema import ChatRequest, ChatResponse
from dependency_injector.wiring import Provide
from fastapi import APIRouter, Depends

from app.core.container import Container
from app.core.middleware import inject_chatbot
# from app.model.user import User
from app.chatbot.services.chatbot_service import ChatbotService
from app.chatbot.services.data_sync_service import DataSyncService

router = APIRouter(
    prefix="/chatbot",
    tags=["chatbot"],
)

@inject_chatbot
def get_post_list(
    service: ChatbotService = Depends(Provide[Container.chatbot_service]),
):
    return service.test()


# set routes
@router.get(
    "/chat/status",
    #tags=["Chatbot"],
    summary="챗봇 기능 상태 확인",
    description="챗봇 서비스의 핵심 기능(Milvus 연결, 설정 로드)이 정상적인지 확인합니다."
)
@inject_chatbot
def chat_status(service: ChatbotService = Depends(Provide[Container.chatbot_service])):
    """챗봇 기능 상태 점검"""
    status = {
        "service": "SilverLink-Chatbot",
        "milvus_connection": "unknown",
        "ready_to_chat": False
    }
    
    # 1. 챗봇 서비스 로드 확인
    if service:
        status["chatbot_service"] = "loaded"
    
    # 2. Milvus 데이터베이스 연결 확인
    try:
        # 'default' alias는 VectorStoreService나 EmbeddingService 초기화 시 설정됨
        if connections.has_connection("default"):
            status["milvus_connection"] = "connected"
            status["ready_to_chat"] = True
        else:
            # 연결이 없다면 시도해볼 수도 있겠지만, 여기서는 상태만 체크
            status["milvus_connection"] = "disconnected"
    except Exception as e:
        status["milvus_connection"] = f"error: {str(e)}"

    return status

#self.data_sync_service = DataSyncService()

@router.post(
    "/chat",
    response_model=ChatResponse,
    #tags=["Chatbot"],
    summary="AI 챗봇 대화",
    description="""
    보호자가 어르신 돌봄 관련 질문을 하면 AI 챗봇이 답변합니다.
    
    **기능:**
    - FAQ 데이터 검색
    - 개인 문의(Inquiry) 이력 검색
    - 대화 컨텍스트 유지 (thread_id 기반)
    - OpenAI GPT 기반 답변 생성
    
    **필수 파라미터:**
    - message: 사용자 질문
    - thread_id: 대화 스레드 ID (예: guardian_123)
    - guardian_id: 보호자 ID
    - elderly_id: 어르신 ID
    """
)
@inject_chatbot
async def chat_endpoint(request: ChatRequest, service: ChatbotService = Depends(Provide[Container.chatbot_service])):
    """챗봇 질문 처리"""
    result = await service.process_chat(
        message=request.message,
        thread_id=request.thread_id,
        guardian_id=request.guardian_id,
        elderly_id=request.elderly_id
    )
    return ChatResponse(
        answer=result["answer"],
        thread_id=request.thread_id,
        sources=result["sources"],
        confidence=result["confidence"]
    )
@router.post(
    "/sync/faqs",
    #tags=["Data Sync"],
    summary="FAQ 데이터 동기화",
    description="""
    Spring Boot 백엔드에서 FAQ 데이터를 가져와 Milvus 벡터 DB에 동기화합니다.
    
    **동작:**
    1. Spring Boot API에서 FAQ 데이터 조회
    2. 질문+답변 텍스트를 OpenAI 임베딩으로 변환
    3. Milvus FAQ 컬렉션에 저장
    
    **사용 시점:**
    - FAQ 데이터가 업데이트되었을 때
    - 초기 데이터 셋업 시
    """
)
@inject_chatbot
def sync_faqs(datasync_service: DataSyncService = Depends(Provide[Container.datasync_service])):
    """FAQ 데이터 동기화"""
    try:
        datasync_service.sync_all_faqs()
        return {"status": "success", "message": "FAQs synced successfully"}
    except Exception as e:
        return {"status": "error", "message": str(e)}
@router.post(
    "/sync/inquiries",
    #tags=["Data Sync"],
    summary="Inquiry 데이터 동기화",
    description="""
    Spring Boot 백엔드에서 Inquiry(문의) 데이터를 가져와 Milvus 벡터 DB에 동기화합니다.
    
    **동작:**
    1. Spring Boot API에서 Inquiry 데이터 조회
    2. 질문+답변 텍스트를 OpenAI 임베딩으로 변환
    3. Milvus Inquiry 컬렉션에 저장 (guardian_id, elderly_id 포함)
    
    **사용 시점:**
    - Inquiry 데이터가 업데이트되었을 때
    - 초기 데이터 셋업 시
    """
)
@inject_chatbot
def sync_inquiries(datasync_service: DataSyncService = Depends(Provide[Container.datasync_service])):
    """Inquiry 데이터 동기화"""
    try:
        datasync_service.sync_all_inquiries()
        return {"status": "success", "message": "Inquiries synced successfully"}
    except Exception as e:
        return {"status": "error", "message": str(e)}