from app.chatbot.repository.base_repository import BaseRepository
from app.core.config import configs
from typing import Any
import logging

logger = logging.getLogger(__name__)


def _milvus():
    from pymilvus import Collection, CollectionSchema, DataType, FieldSchema, connections, utility

    return connections, Collection, FieldSchema, CollectionSchema, DataType, utility

class ChatbotRepository(BaseRepository):
    """Milvus/Zilliz 벡터 DB 서비스"""

    def __init__(self):
        self._connect()
        self.faq_collection = self._get_or_create_faq_collection()
        self.inquiry_collection = self._get_or_create_inquiry_collection()

    def _connect(self):
        connections, _, _, _, _, _ = _milvus()
        try:
            connections.connect(
                alias="default",
                uri=configs.MILVUS_URI,
                token=configs.MILVUS_TOKEN
            )
            logger.info("Connected to Milvus/Zilliz")
        except Exception as e:
            logger.error(f"Failed to connect to Milvus: {e}")
            raise

    def _get_or_create_faq_collection(self) -> Any:
        _, Collection, FieldSchema, CollectionSchema, DataType, utility = _milvus()
        name = configs.FAQ_COLLECTION_NAME
        if utility.has_collection(name):
            return Collection(name)

        fields = [
            FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=False),
            FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=1536),
            FieldSchema(name="category", dtype=DataType.VARCHAR, max_length=50),
            FieldSchema(name="question", dtype=DataType.VARCHAR, max_length=2000),
            FieldSchema(name="answer", dtype=DataType.VARCHAR, max_length=2000)
        ]
        schema = CollectionSchema(fields, "FAQ Collection")
        collection = Collection(name, schema)
        
        index_params = {
            "metric_type": "COSINE",
            "index_type": "AUTOINDEX",
            "params": {}
        }
        collection.create_index(field_name="embedding", index_params=index_params)
        return collection

    def _get_or_create_inquiry_collection(self) -> Any:
        _, Collection, FieldSchema, CollectionSchema, DataType, utility = _milvus()
        name = configs.INQUIRY_COLLECTION_NAME
        if utility.has_collection(name):
            return Collection(name)

        fields = [
            FieldSchema(name="id", dtype=DataType.INT64, is_primary=True, auto_id=False),
            FieldSchema(name="embedding", dtype=DataType.FLOAT_VECTOR, dim=1536),
            FieldSchema(name="guardian_id", dtype=DataType.INT64),
            FieldSchema(name="elderly_id", dtype=DataType.INT64),
            FieldSchema(name="question", dtype=DataType.VARCHAR, max_length=2000),
            FieldSchema(name="answer", dtype=DataType.VARCHAR, max_length=2000)
        ]
        schema = CollectionSchema(fields, "Inquiry Collection")
        collection = Collection(name, schema)
        
        index_params = {
            "metric_type": "COSINE",
            "index_type": "AUTOINDEX",
            "params": {}
        }
        collection.create_index(field_name="embedding", index_params=index_params)
        return collection

    def insert_faq(self, data: list):
        self.faq_collection.insert(data)
        self.faq_collection.flush()

    def insert_inquiry(self, data: list):
        self.inquiry_collection.insert(data)
        self.inquiry_collection.flush()
    
    def search_faq(self, embedding: list[float], limit: int = 3):
        self.faq_collection.load()
        return self.faq_collection.search(
            data=[embedding],
            anns_field="embedding",
            param={"metric_type": "COSINE"},
            limit=limit,
            output_fields=["question", "answer", "category"]
        )

    def search_inquiry(self, embedding: list[float], guardian_id: int, elderly_id: int, limit: int = 2):
        self.inquiry_collection.load()
        return self.inquiry_collection.search(
            data=[embedding],
            anns_field="embedding",
            param={"metric_type": "COSINE"},
            expr=f"guardian_id == {guardian_id} and elderly_id == {elderly_id}",
            limit=limit,
            output_fields=["question", "answer"]
        )
