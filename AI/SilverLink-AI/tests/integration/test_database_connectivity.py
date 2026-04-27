"""Database connectivity integration tests"""
import pytest
import os



@pytest.mark.database
@pytest.mark.integration
@pytest.mark.skipif(not os.getenv("OPENAI_API_KEY"), reason="Env vars missing")
class TestDatabaseConnectivity:
    """Test database connectivity and collection management"""
    
    def test_milvus_connection(self, milvus_connection):
        """Test Milvus/Zilliz connection is successful"""
        from pymilvus import connections, utility
        # Check if connection exists
        assert connections.has_connection("default")
        
        # Try to list collections (this will fail if not connected)
        collections = utility.list_collections()
        assert isinstance(collections, list)
        print(f"✅ Connected to Milvus. Found {len(collections)} collections: {collections}")
    
    def test_faq_collection_exists(self, vector_store_service):
        """Test FAQ collection exists and has correct schema"""
        from app.core.config import configs
        from pymilvus import utility
        collection_name = configs.FAQ_COLLECTION_NAME
        
        # Check collection exists
        assert utility.has_collection(collection_name), f"FAQ collection '{collection_name}' does not exist"
        
        # Get collection
        collection = vector_store_service.faq_collection
        assert collection is not None
        
        # Check schema
        schema = collection.schema
        field_names = [field.name for field in schema.fields]
        
        expected_fields = ["id", "embedding", "category", "question", "answer"]
        for field in expected_fields:
            assert field in field_names, f"Missing field '{field}' in FAQ collection schema"
        
        print(f"✅ FAQ collection exists with fields: {field_names}")
    
    def test_inquiry_collection_exists(self, vector_store_service):
        """Test Inquiry collection exists and has correct schema"""
        from app.core.config import configs
        from pymilvus import utility
        collection_name = configs.INQUIRY_COLLECTION_NAME
        
        # Check collection exists
        assert utility.has_collection(collection_name), f"Inquiry collection '{collection_name}' does not exist"
        
        # Get collection
        collection = vector_store_service.inquiry_collection
        assert collection is not None
        
        # Check schema
        schema = collection.schema
        field_names = [field.name for field in schema.fields]
        
        expected_fields = ["id", "embedding", "guardian_id", "elderly_id", "question", "answer"]
        for field in expected_fields:
            assert field in field_names, f"Missing field '{field}' in Inquiry collection schema"
        
        print(f"✅ Inquiry collection exists with fields: {field_names}")
    
    def test_collection_schema_validation(self, vector_store_service):
        """Validate collection schemas in detail"""
        # FAQ collection schema
        faq_schema = vector_store_service.faq_collection.schema
        faq_fields = {field.name: field for field in faq_schema.fields}
        
        # Check FAQ embedding dimension
        assert faq_fields["embedding"].params["dim"] == 1536, "FAQ embedding dimension should be 1536"
        
        # Check FAQ primary key
        assert faq_fields["id"].is_primary, "FAQ 'id' should be primary key"
        
        # Inquiry collection schema
        inquiry_schema = vector_store_service.inquiry_collection.schema
        inquiry_fields = {field.name: field for field in inquiry_schema.fields}
        
        # Check Inquiry embedding dimension
        assert inquiry_fields["embedding"].params["dim"] == 1536, "Inquiry embedding dimension should be 1536"
        
        # Check Inquiry primary key
        assert inquiry_fields["id"].is_primary, "Inquiry 'id' should be primary key"
        
        print("✅ Collection schemas validated successfully")
    
    def test_collection_indexes(self, vector_store_service):
        """Test that collections have proper indexes"""
        # FAQ collection index
        faq_collection = vector_store_service.faq_collection
        faq_indexes = faq_collection.indexes
        
        assert len(faq_indexes) > 0, "FAQ collection should have at least one index"
        
        # Check if embedding field has index
        embedding_index = None
        for index in faq_indexes:
            if index.field_name == "embedding":
                embedding_index = index
                break
        
        assert embedding_index is not None, "FAQ collection should have index on 'embedding' field"
        assert embedding_index.params["metric_type"] == "COSINE", "FAQ index should use COSINE metric"
        
        # Inquiry collection index
        inquiry_collection = vector_store_service.inquiry_collection
        inquiry_indexes = inquiry_collection.indexes
        
        assert len(inquiry_indexes) > 0, "Inquiry collection should have at least one index"
        
        print("✅ Collection indexes validated successfully")
    
    @pytest.mark.slow
    def test_collection_statistics(self, vector_store_service):
        """Get collection statistics"""
        # Load collections
        vector_store_service.faq_collection.load()
        vector_store_service.inquiry_collection.load()
        
        # Get FAQ stats
        faq_count = vector_store_service.faq_collection.num_entities
        print(f"📊 FAQ collection has {faq_count} entities")
        
        # Get Inquiry stats
        inquiry_count = vector_store_service.inquiry_collection.num_entities
        print(f"📊 Inquiry collection has {inquiry_count} entities")
        
        # Collections can be empty, but should be accessible
        assert faq_count >= 0
        assert inquiry_count >= 0
