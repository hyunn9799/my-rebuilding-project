# SilverLink 챗봇 통합 테스트 가이드

## 📋 개요

이 문서는 SilverLink 챗봇 시스템의 통합 테스트 실행 방법을 안내합니다.

## 🛠️ 테스트 환경 설정

### 1. 의존성 설치

```bash
cd d:\lecture\Project\Sesac-Saltlux-Final\SilverLink-AI
pip install -r requirements.txt
```

### 2. 환경 변수 확인

`.env` 파일에 다음 설정이 있는지 확인:
- `OPENAI_API_KEY`: OpenAI API 키
- `MILVUS_URI`: Milvus/Zilliz 엔드포인트
- `MILVUS_TOKEN`: Milvus/Zilliz 인증 토큰

### 3. 서버 실행

**Python AI 서버:**
```bash
cd d:\lecture\Project\Sesac-Saltlux-Final\SilverLink-AI
python -m uvicorn src.app.main:app --reload --port 8000
```

**Spring Boot 서버 (선택사항):**
```bash
cd d:\lecture\Project\Sesac-Saltlux-Final\SilverLink-BE
./gradlew bootRun
```

## 🧪 테스트 실행 방법

### 전체 테스트 실행

```bash
# 모든 테스트 실행
pytest tests/ -v

# 특정 마커만 실행
pytest tests/ -v -m integration
pytest tests/ -v -m database
pytest tests/ -v -m e2e
pytest tests/ -v -m performance
```

### 카테고리별 테스트 실행

```bash
# 데이터베이스 연결 테스트
pytest tests/integration/test_database_connectivity.py -v

# 벡터 검색 테스트
pytest tests/integration/test_vector_search.py -v

# 챗봇 서비스 테스트
pytest tests/integration/test_chatbot_service.py -v

# 인증 테스트
pytest tests/integration/test_authentication.py -v

# 에러 처리 테스트
pytest tests/integration/test_error_handling.py -v

# End-to-End 테스트
pytest tests/integration/test_end_to_end.py -v

# 성능 테스트
pytest tests/integration/test_performance.py -v -s
```

### 빠른 테스트 (느린 테스트 제외)

```bash
pytest tests/ -v -m "not slow"
```

### 커버리지 측정

```bash
# 커버리지 리포트 생성
pytest tests/ --cov=src --cov-report=html --cov-report=term

# 리포트 확인
start htmlcov/index.html
```

### 통합 테스트 스크립트 실행

```bash
# 개선된 통합 테스트 스크립트
python tests/integration_test.py
```

## 📊 테스트 카테고리

### 1. 데이터베이스 연결 테스트
- Milvus/Zilliz 연결 확인
- FAQ/Inquiry 컬렉션 스키마 검증
- 인덱스 확인

### 2. 벡터 검색 테스트
- FAQ 벡터 검색 정확도
- Inquiry 필터링 검색
- 검색 결과 랭킹
- 병렬 검색
- 의미적 유사도

### 3. 챗봇 서비스 테스트
- 기본 채팅 처리
- Thread ID 컨텍스트 유지
- 대화 메모리 관리
- 임베딩 생성

### 4. 인증/권한 테스트
- Guardian-Elderly 관계 검증
- 권한 없는 접근 차단
- 잘못된 ID 처리

### 5. 에러 처리 테스트
- 빈 메시지 처리
- 잘못된 요청 형식
- Null 값 처리
- 타임아웃 처리
- 특수 문자 처리

### 6. End-to-End 테스트
- FAQ 동기화 플로우
- Spring Boot ↔ Python 통신
- 연속 대화 시나리오
- 다중 사용자 동시 요청

### 7. 성능 테스트
- 응답 시간 측정 (목표: < 3초)
- 동시 요청 처리
- 벡터 검색 성능
- 메모리 사용량
- 처리량 (throughput)

## 🎯 테스트 성공 기준

✅ **모든 자동화 테스트 통과** (pytest 100% pass)  
✅ **평균 응답 시간 < 3초**  
✅ **동시 요청 10개 이상 처리 가능**  
✅ **에러 케이스 적절히 처리** (400, 403, 422, 500 응답)  
✅ **대화 컨텍스트 유지** (thread_id 기반)

## 🔧 트러블슈팅

### Milvus 연결 실패
```
ConnectionError: Failed to connect to Milvus
```
**해결방법:**
- `.env` 파일의 `MILVUS_URI`와 `MILVUS_TOKEN` 확인
- Zilliz 클라우드 서비스 상태 확인

### OpenAI API 오류
```
OpenAI API Error: Invalid API key
```
**해결방법:**
- `.env` 파일의 `OPENAI_API_KEY` 확인
- API 키 유효성 및 크레딧 잔액 확인

### Spring Boot 연결 실패
```
ConnectionError: Connection refused
```
**해결방법:**
- Spring Boot 서버가 실행 중인지 확인
- 포트 8080이 사용 가능한지 확인
- 해당 테스트는 `@pytest.mark.skip`으로 건너뛸 수 있음

### 테스트 타임아웃
```
TimeoutError: Request timed out
```
**해결방법:**
- 네트워크 연결 확인
- OpenAI API 응답 시간이 느릴 수 있음 (재시도)
- 타임아웃 값 증가 (테스트 코드에서 `timeout` 파라미터 조정)

## 📝 테스트 작성 가이드

### 새로운 테스트 추가

1. `tests/integration/` 디렉토리에 `test_*.py` 파일 생성
2. 적절한 마커 추가 (`@pytest.mark.integration` 등)
3. `conftest.py`의 픽스처 활용
4. 테스트 함수 이름은 `test_`로 시작

### 테스트 마커

- `@pytest.mark.integration`: 통합 테스트
- `@pytest.mark.database`: 데이터베이스 관련 테스트
- `@pytest.mark.e2e`: End-to-End 테스트
- `@pytest.mark.slow`: 느린 테스트 (10초 이상)
- `@pytest.mark.performance`: 성능 테스트
- `@pytest.mark.asyncio`: 비동기 테스트

### 픽스처 사용 예시

```python
def test_example(test_client, vector_store_service, sample_faq_data):
    # test_client: FastAPI 테스트 클라이언트
    # vector_store_service: VectorStoreService 인스턴스
    # sample_faq_data: 샘플 FAQ 데이터
    pass
```

## 🚀 CI/CD 통합

### GitHub Actions 예시

```yaml
name: Integration Tests

on: [push, pull_request]

jobs:
  test:
    runs-on: ubuntu-latest
    steps:
      - uses: actions/checkout@v2
      - name: Set up Python
        uses: actions/setup-python@v2
        with:
          python-version: '3.10'
      - name: Install dependencies
        run: |
          pip install -r requirements.txt
      - name: Run tests
        env:
          OPENAI_API_KEY: ${{ secrets.OPENAI_API_KEY }}
          MILVUS_URI: ${{ secrets.MILVUS_URI }}
          MILVUS_TOKEN: ${{ secrets.MILVUS_TOKEN }}
        run: |
          pytest tests/ -v -m "not slow"
```

## 📞 문의

테스트 관련 문의사항은 개발팀에 문의하세요.
