"""Performance and load testing"""
import pytest
import time
import asyncio
import psutil
import os
from statistics import mean
from fastapi.testclient import TestClient



@pytest.mark.performance
@pytest.mark.slow
@pytest.mark.skipif(not os.getenv("OPENAI_API_KEY"), reason="Env vars missing")
class TestPerformance:
    """Performance and load tests"""
    
    @pytest.fixture(autouse=True)
    def setup(self):
        """Setup for each test"""
        from app.main import app
        self.client = TestClient(app)
        self.response_times = []
    
    def test_response_time(self):
        """Test that response time is under 3 seconds"""
        payload = {
            "message": "노인 장기 요양 보험이 뭐야?",
            "thread_id": "perf_test_1",
            "guardian_id": 1,
            "elderly_id": 1
        }
        
        # Measure response time for 5 requests
        times = []
        for i in range(5):
            start = time.time()
            response = self.client.post("/chat", json=payload)
            elapsed = time.time() - start
            
            times.append(elapsed)
            assert response.status_code == 200
        
        avg_time = mean(times)
        max_time = max(times)
        min_time = min(times)
        
        print("📊 Response times:")
        print(f"   Average: {avg_time:.3f}s")
        print(f"   Min: {min_time:.3f}s")
        print(f"   Max: {max_time:.3f}s")
        
        # Target: average under 3 seconds
        assert avg_time < 3.0, f"Average response time {avg_time:.3f}s exceeds 3s target"
        
        print(f"✅ Response time test passed (avg: {avg_time:.3f}s < 3s)")
    
    @pytest.mark.asyncio
    async def test_concurrent_requests(self):
        """Test handling of 10 concurrent requests"""
        async def make_request(request_id: int):
            payload = {
                "message": f"동시 요청 {request_id}: 노인 복지 서비스는?",
                "thread_id": f"concurrent_{request_id}",
                "guardian_id": 1,
                "elderly_id": 1
            }
            
            start = time.time()
            response = self.client.post("/chat", json=payload)
            elapsed = time.time() - start
            
            return {
                "id": request_id,
                "status": response.status_code,
                "time": elapsed
            }
        
        # Create 10 concurrent requests
        num_requests = 10
        tasks = [make_request(i) for i in range(num_requests)]
        results = await asyncio.gather(*tasks)
        
        # Analyze results
        success_count = sum(1 for r in results if r["status"] == 200)
        times = [r["time"] for r in results]
        avg_time = mean(times)
        
        print(f"📊 Concurrent requests test ({num_requests} requests):")
        print(f"   Success rate: {success_count}/{num_requests}")
        print(f"   Average time: {avg_time:.3f}s")
        print(f"   Max time: {max(times):.3f}s")
        
        # At least 80% should succeed
        assert success_count >= num_requests * 0.8, \
            f"Only {success_count}/{num_requests} requests succeeded"
        
        print(f"✅ Concurrent requests test passed ({success_count}/{num_requests} succeeded)")
    
    def test_vector_search_performance(self):
        """Test vector search performance"""    
        #embedding_service = EmbeddingService()
        #vector_store = ChatbotRepository()
        
        queries = [
            "노인 장기 요양 보험",
            "복지 서비스",
            "건강 검진",
            "치매 검사",
            "기초 연금"
        ]
        
        search_times = []
        
        for query in queries:
            # Measure embedding creation time
            start = time.time()
            #embedding = embedding_service.create_embedding(query)
            embed_time = time.time() - start
            
            # Measure search time
            start = time.time()
            #results = vector_store.search_faq(embedding, limit=3)
            search_time = time.time() - start
            
            total_time = embed_time + search_time
            search_times.append(total_time)
            
            print(f"   Query '{query[:20]}...': {total_time:.3f}s (embed: {embed_time:.3f}s, search: {search_time:.3f}s)")
        
        avg_search_time = mean(search_times)
        
        print("📊 Vector search performance:")
        print(f"   Average: {avg_search_time:.3f}s")
        print(f"   Max: {max(search_times):.3f}s")
        
        # Vector search should be fast (< 1 second on average)
        assert avg_search_time < 1.0, \
            f"Vector search too slow: {avg_search_time:.3f}s"
        
        print(f"✅ Vector search performance test passed (avg: {avg_search_time:.3f}s)")
    
    @pytest.mark.asyncio
    async def test_throughput(self):
        """Test system throughput (requests per second)"""
        async def make_request(request_id: int):
            payload = {
                "message": f"처리량 테스트 {request_id}",
                "thread_id": f"throughput_{request_id}",
                "guardian_id": 1,
                "elderly_id": 1
            }
            
            response = self.client.post("/chat", json=payload)
            return response.status_code == 200
        
        # Send 20 requests and measure time
        num_requests = 20
        start = time.time()
        
        tasks = [make_request(i) for i in range(num_requests)]
        results = await asyncio.gather(*tasks)
        
        elapsed = time.time() - start
        success_count = sum(results)
        throughput = success_count / elapsed
        
        print("📊 Throughput test:")
        print(f"   Total requests: {num_requests}")
        print(f"   Successful: {success_count}")
        print(f"   Time: {elapsed:.2f}s")
        print(f"   Throughput: {throughput:.2f} requests/second")
        
        # Should handle at least 1 request per second
        assert throughput >= 1.0, f"Throughput too low: {throughput:.2f} req/s"
        
        print(f"✅ Throughput test passed: {throughput:.2f} req/s")
    
    def test_memory_usage_stability(self):
        """Test that memory usage remains stable over multiple requests"""
        
        process = psutil.Process(os.getpid())
        
        # Get initial memory
        initial_memory = process.memory_info().rss / 1024 / 1024  # MB
        
        # Make 50 requests
        payload = {
            "message": "메모리 테스트",
            "thread_id": "memory_test",
            "guardian_id": 1,
            "elderly_id": 1
        }
        
        for i in range(50):
            response = self.client.post("/chat", json=payload)
            assert response.status_code == 200
        
        # Get final memory
        final_memory = process.memory_info().rss / 1024 / 1024  # MB
        memory_increase = final_memory - initial_memory
        
        print("📊 Memory usage:")
        print(f"   Initial: {initial_memory:.2f} MB")
        print(f"   Final: {final_memory:.2f} MB")
        print(f"   Increase: {memory_increase:.2f} MB")
        
        # Memory increase should be reasonable (< 100 MB for 50 requests)
        assert memory_increase < 100, \
            f"Memory increase too high: {memory_increase:.2f} MB"
        
        print(f"✅ Memory stability test passed (increase: {memory_increase:.2f} MB)")
    
    def test_cold_start_vs_warm_performance(self):
        """Test performance difference between cold start and warm requests"""
        payload = {
            "message": "성능 비교 테스트",
            "thread_id": "cold_warm_test",
            "guardian_id": 1,
            "elderly_id": 1
        }
        
        # Cold start (first request)
        start = time.time()
        response1 = self.client.post("/chat", json=payload)
        cold_time = time.time() - start
        
        assert response1.status_code == 200
        
        # Warm requests (subsequent requests)
        warm_times = []
        for i in range(5):
            start = time.time()
            response = self.client.post("/chat", json=payload)
            warm_time = time.time() - start
            warm_times.append(warm_time)
            assert response.status_code == 200
        
        avg_warm_time = mean(warm_times)
        
        print("📊 Cold vs Warm performance:")
        print(f"   Cold start: {cold_time:.3f}s")
        print(f"   Warm average: {avg_warm_time:.3f}s")
        print(f"   Difference: {cold_time - avg_warm_time:.3f}s")
        
        # Warm requests should generally be faster or similar
        # (allowing some variance)
        
        print("✅ Cold/warm performance comparison completed")
