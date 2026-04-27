from langchain_openai import OpenAIEmbeddings
from app.core.config import configs

class EmbeddingService:
    """OpenAI 임베딩 서비스"""
    
    def __init__(self):
        self.embeddings = OpenAIEmbeddings(
            model=configs.EMBEDDING_MODEL,
            api_key=configs.OPENAI_API_KEY
        )

    def create_embedding(self, text: str) -> list[float]:
        return self.embeddings.embed_query(text)

    def create_embeddings(self, texts: list[str]) -> list[list[float]]:
        return self.embeddings.embed_documents(texts)
