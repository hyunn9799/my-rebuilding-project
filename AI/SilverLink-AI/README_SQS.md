# SilverLink-AI SQS Worker 사용 가이드

## 📋 개요

SilverLink-AI는 AWS SQS를 사용하여 비동기 통화 처리를 수행합니다.

### 아키텍처

```
Spring Boot BE → SQS Queue → Worker → Twilio Call
                     ↓
                   DLQ (실패 시)
```

---

## 🚀 실행 방법

### 1. 로컬 개발 환경

#### Option A: 별도 실행 (권장)

**API 서버 실행:**
```bash
# Windows
run_api.bat

# Linux/Mac
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```

**Worker 실행 (별도 터미널):**
```bash
# Windows
run_worker.bat

# Linux/Mac
python worker_main.py
```

#### Option B: 동시 실행

```bash
# Windows
run_all.bat

# Linux/Mac (tmux 사용)
tmux new-session -d -s api 'python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload'
tmux new-session -d -s worker 'python worker_main.py'
```

---

### 2. Docker 환경

#### Docker Compose 사용 (권장)

```bash
# 빌드 및 실행
docker-compose up -d

# 로그 확인
docker-compose logs -f

# API 로그만 확인
docker-compose logs -f api

# Worker 로그만 확인
docker-compose logs -f worker

# 중지
docker-compose down
```

#### 개별 Docker 실행

```bash
# API 서버
docker build -t silverlink-api .
docker run -p 8000:8000 --env-file .env silverlink-api

# Worker
docker build -t silverlink-worker -f Dockerfile.worker .
docker run --env-file .env silverlink-worker
```

---

## 📡 API 사용법

### 1. 통화 스케줄 생성 (SQS 발행)

**Endpoint:** `POST /api/callbot/schedule-call`

**Request Body:**
```json
{
  "schedule_id": 1,
  "elderly_id": 100,
  "elderly_name": "홍길동",
  "phone_number": "+821012345678",
  "scheduled_time": "2026-01-29T10:00:00"
}
```

**Response:**
```json
{
  "status": "queued",
  "message_id": "abc123-def456-...",
  "schedule_id": 1,
  "elderly_name": "홍길동",
  "scheduled_time": "2026-01-29T10:00:00",
  "queue_url": "https://sqs.ap-northeast-2.amazonaws.com/..."
}
```

**cURL 예시:**
```bash
curl -X POST "http://localhost:5000/api/callbot/schedule-call" \
  -H "Content-Type: application/json" \
  -d '{
    "schedule_id": 1,
    "elderly_id": 100,
    "elderly_name": "홍길동",
    "phone_number": "+821012345678",
    "scheduled_time": "2026-01-29T10:00:00"
  }'
```

---

### 2. 즉시 통화 (기존 방식)

**Endpoint:** `GET /api/callbot/call`

```bash
curl http://localhost:5000/api/callbot/call
```

---

## 🔧 환경 변수 설정

`.env` 파일에 다음 변수들이 설정되어 있어야 합니다:

```properties
# AWS SQS
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=your_access_key
AWS_SECRET_ACCESS_KEY=your_secret_key
SQS_QUEUE_URL=https://sqs.ap-northeast-2.amazonaws.com/123456/silverlink-queue
SQS_DLQ_URL=https://sqs.ap-northeast-2.amazonaws.com/123456/silverlink-dlq

# Twilio
TWILIO_SID=your_twilio_sid
TWILIO_TOKEN=your_twilio_token
SILVERLINK_NUMBER=+15177817915

# OpenAI
OPENAI_API_KEY=your_openai_key

# Luxia TTS
LUXIA_API_KEY=your_luxia_key

# Server URL
CALL_CONTROLL_URL=https://your-ngrok-url.ngrok-free.dev
```

---

## 📊 모니터링

### 로그 확인

**로컬 실행:**
```bash
# Worker 로그
tail -f logs/worker.log

# API 로그
# 콘솔에 출력됨
```

**Docker 실행:**
```bash
# 전체 로그
docker-compose logs -f

# Worker만
docker-compose logs -f worker

# 최근 100줄
docker-compose logs --tail=100 worker
```

---

### SQS 큐 상태 확인

**AWS Console:**
1. AWS Console → SQS
2. `silverlink-queue` 선택
3. "Messages available" 확인

**AWS CLI:**
```bash
aws sqs get-queue-attributes \
  --queue-url https://sqs.ap-northeast-2.amazonaws.com/123456/silverlink-queue \
  --attribute-names All
```

---

## 🔄 재시도 로직

Worker는 실패한 통화를 자동으로 재시도합니다:

| 시도 | 지연 시간 | 동작 |
|-----|---------|------|
| 1차 | 0초 | 즉시 처리 |
| 2차 | 30초 | 재시도 |
| 3차 | 60초 | 재시도 |
| 4차 | 90초 | 재시도 |
| 5차 | - | DLQ 이동 |

---

## 🚨 DLQ (Dead Letter Queue) 처리

### DLQ 메시지 확인

**Python 스크립트:**
```python
from app.core.container import Container

container = Container()
dlq_handler = container.dlq_handler()

# DLQ 통계
stats = dlq_handler.get_dlq_stats()
print(stats)

# DLQ 메시지 조회
messages = dlq_handler.poll_dlq(max_messages=10)
for msg in messages:
    print(f"Failed: {msg.original_message.elderly_name}")
    print(f"Reason: {msg.failure_reason}")
```

### DLQ 메시지 재처리

```python
# 특정 메시지 재처리
success = dlq_handler.reprocess_message(dlq_message)
```

---

## 🧪 테스트

### Unit Tests 실행

```bash
# 전체 테스트
pytest tests/unit_tests/test_sqs_client.py -v

# 특정 테스트만
pytest tests/unit_tests/test_sqs_client.py::TestSQSWorker -v

# 커버리지 포함
pytest tests/unit_tests/test_sqs_client.py --cov=app.queue --cov-report=html
```

---

## 🐛 트러블슈팅

### 1. Worker가 메시지를 받지 못함

**확인 사항:**
- AWS 자격증명 확인
- SQS 큐 URL 확인
- 네트워크 연결 확인

**해결:**
```bash
# AWS 자격증명 테스트
aws sqs list-queues --region ap-northeast-2
```

---

### 2. 통화가 실패함

**확인 사항:**
- Twilio 자격증명 확인
- 전화번호 형식 (E.164: +821012345678)
- ngrok URL 확인

**로그 확인:**
```bash
# Worker 로그에서 에러 확인
grep "ERROR" logs/worker.log
```

---

### 3. DLQ에 메시지가 쌓임

**확인:**
```python
# DLQ 통계 확인
dlq_handler.get_dlq_stats()
```

**조치:**
1. 실패 원인 분석
2. 문제 해결 후 재처리
3. 필요시 수동 삭제

---

## 📚 추가 자료

- [AWS SQS 문서](https://docs.aws.amazon.com/sqs/)
- [Twilio API 문서](https://www.twilio.com/docs/voice)
- [FastAPI 문서](https://fastapi.tiangolo.com/)

---

## 🆘 지원

문제가 발생하면:
1. 로그 확인 (`logs/worker.log`)
2. AWS SQS 콘솔 확인
3. DLQ 메시지 확인
4. 이슈 등록

---

**작성일**: 2026-01-29  
**버전**: 1.0
