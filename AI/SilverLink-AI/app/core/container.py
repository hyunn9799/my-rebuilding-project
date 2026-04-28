from dependency_injector import containers, providers

# from app.core.database import Database
from app.callbot.repository import CallbotRepository
from app.chatbot.repository import ChatbotRepository
from app.ocr.repository import OcrRepository
from app.callbot.services import CallbotService
from app.chatbot.services.chatbot_service import ChatbotService
from app.chatbot.services.data_sync_service import DataSyncService
from app.chatbot.services.embedding_service import EmbeddingService
from app.ocr.services import OcrService
from app.core.config import configs
from app.integration.llm.openai_client import LLM
from app.integration.tts.luxia_client import TTS
from app.integration.call import CALL
from app.queue.sqs_client import SQSClient
from app.queue.worker import SQSWorker
from app.queue.dlq_handler import DLQHandler

# OCR 파이프라인 신규 모듈
from app.ocr.repository.drug_repository import DrugRepository
from app.ocr.repository.drug_vector_repository import DrugVectorRepository
from app.ocr.repository.ocr_result_repository import OcrResultRepository
from app.ocr.repository.alias_suggestion_repository import AliasSuggestionRepository
from app.ocr.services.text_normalizer import TextNormalizer
from app.ocr.services.mysql_matcher import MySQLMatcher
from app.ocr.services.drug_dictionary_index import DrugDictionaryIndex
from app.ocr.services.vector_matcher import VectorMatcher
from app.ocr.services.rule_validator import RuleValidator
from app.ocr.services.llm_descriptor import LLMDescriptor
from app.ocr.services.llm_extractor import LLMExtractor
from app.ocr.services.medication_pipeline import MedicationPipeline


class Container(containers.DeclarativeContainer):
    wiring_config = containers.WiringConfiguration(
        modules=[
            "app.api.endpoints.callbot",
            "app.api.endpoints.chatbot",
            "app.api.endpoints.ocr",
        ]
    )

    # db = providers.Singleton(Database, db_url=configs.DATABASE_URI)
    llm = providers.Singleton(LLM, model_version=configs.INFERENCE_MODEL, api_key=configs.OPENAI_API_KEY)
    # stt = providers.Singleton(STT, model_name="naver", secret_key=configs.CLOVA_SECRET_KEY, url=configs.CLOVA_STT_URL)
    tts = providers.Singleton(TTS, api_key=configs.LUXIA_API_KEY, url=configs.CALL_CONTROLL_URL)
    call = providers.Singleton(CALL, account_sid=configs.TWILIO_SID, auth_token=configs.TWILIO_TOKEN, url=configs.CALL_CONTROLL_URL, number=configs.NUMBER, silverlink_number=configs.SILVERLINK_NUMBER)
    
    # AWS SQS
    sqs_client = providers.Singleton(
        SQSClient,
        queue_url=configs.SQS_QUEUE_URL,
        dlq_url=configs.SQS_DLQ_URL,
        region_name=configs.AWS_REGION,
        aws_access_key_id=configs.AWS_ACCESS_KEY_ID,
        aws_secret_access_key=configs.AWS_SECRET_ACCESS_KEY
    )
     
    callbot_repository = providers.Factory(CallbotRepository)
    chatbot_repository = providers.Factory(ChatbotRepository)
    ocr_repository = providers.Factory(OcrRepository)

    callbot_service = providers.Factory(CallbotService, callbot_repository=callbot_repository, llm=llm, call=call, tts=tts)
    datasync_service = providers.Factory(DataSyncService, chatbot_repository=chatbot_repository)
    chatbot_service = providers.Factory(ChatbotService, chatbot_repository=chatbot_repository)
    ocr_service = providers.Factory(OcrService, ocr_repository=ocr_repository, llm=llm)

    # ──────────────────────────────────────────
    # OCR 약 식별 파이프라인
    # ──────────────────────────────────────────
    embedding_service = providers.Singleton(EmbeddingService)
    drug_repository = providers.Factory(DrugRepository)
    drug_vector_repository = providers.Singleton(DrugVectorRepository)
    text_normalizer = providers.Factory(TextNormalizer)
    drug_dictionary_index = providers.Singleton(
        DrugDictionaryIndex,
        drug_repository=drug_repository,
    )
    mysql_matcher = providers.Factory(
        MySQLMatcher,
        drug_repository=drug_repository,
        dictionary_index=drug_dictionary_index,
    )
    vector_matcher = providers.Factory(
        VectorMatcher,
        drug_repository=drug_repository,
        vector_repository=drug_vector_repository,
        embedding_service=embedding_service,
    )
    rule_validator = providers.Factory(RuleValidator)
    llm_descriptor = providers.Factory(LLMDescriptor, llm=llm)

    # Phase 5: OCR 결과 저장 + 사용자 확인
    ocr_result_repository = providers.Factory(OcrResultRepository)
    alias_suggestion_repository = providers.Factory(AliasSuggestionRepository)

    # Phase 6: LLM 구조화 추출
    llm_extractor = providers.Factory(LLMExtractor, llm=llm)

    medication_pipeline = providers.Factory(
        MedicationPipeline,
        text_normalizer=text_normalizer,
        mysql_matcher=mysql_matcher,
        vector_matcher=vector_matcher,
        rule_validator=rule_validator,
        llm_descriptor=llm_descriptor,
        ocr_result_repo=ocr_result_repository,
        llm_extractor=llm_extractor,
    )

    # SQS Worker & DLQ Handler
    sqs_worker = providers.Factory(
        SQSWorker,
        sqs_client=sqs_client,
        callbot_service=callbot_service,
        call_client=call
    )
    dlq_handler = providers.Factory(DLQHandler, sqs_client=sqs_client)
