import requests
import logging
from app.core.config import configs
from app.chatbot.services.embedding_service import EmbeddingService
from app.chatbot.repository.chatbot_repository import ChatbotRepository

logger = logging.getLogger(__name__)

class DataSyncService:
    """Spring Boot API와 데이터 동기화 서비스"""

    def __init__(self, chatbot_repository: ChatbotRepository):
        self.chatbot_repository = chatbot_repository
        self.embedding_service = EmbeddingService()
        self.base_url = configs.SPRING_BOOT_URL

    def sync_all_faqs(self):
        try:
            url = f"{self.base_url}/api/data/faqs/all"
            response = requests.get(url)
            response.raise_for_status()
            faqs = response.json()
            
            if not faqs:
                logger.info("No FAQs to sync")
                return

            ids = []
            embeddings = []
            categories = []
            questions = []
            answers = []

            for faq in faqs:
                text = f"{faq['question']} {faq['answerText']}"
                embedding = self.embedding_service.create_embedding(text)
                
                ids.append(faq['faqId'])
                embeddings.append(embedding)
                categories.append(faq['category'])
                questions.append(faq['question'])
                answers.append(faq['answerText'])

            self.chatbot_repository.insert_faq([ids, embeddings, categories, questions, answers])
            logger.info(f"Synced {len(faqs)} FAQs")

        except Exception as e:
            logger.error(f"Failed to sync FAQs: {e}")
            raise

    def sync_all_inquiries(self):
        try:
            url = f"{self.base_url}/api/data/inquiries/answered"
            response = requests.get(url)
            response.raise_for_status()
            inquiries = response.json()

            if not inquiries:
                logger.info("No Inquiries to sync")
                return

            ids = []
            embeddings = []
            guardian_ids = []
            elderly_ids = []
            questions = []
            answers = []

            for inquiry in inquiries:
                text = f"{inquiry['question']} {inquiry['answer']}"
                embedding = self.embedding_service.create_embedding(text)
                
                ids.append(inquiry['inquiryId'])
                embeddings.append(embedding)
                guardian_ids.append(inquiry['guardianUserId'])
                elderly_ids.append(inquiry['elderlyUserId'])
                questions.append(inquiry['question'])
                answers.append(inquiry['answer'])

            self.chatbot_repository.insert_inquiry([ids, embeddings, guardian_ids, elderly_ids, questions, answers])
            logger.info(f"Synced {len(inquiries)} Inquiries")

        except Exception as e:
            logger.error(f"Failed to sync Inquiries: {e}")
            raise
