# 환경 설정 가이드

## 환경별 설정 파일

### 로컬 개발 환경
- 파일: `.env.local`
- 사용 시나리오: 개발자 로컬 PC에서 개발할 때
- 특징:
  - 로컬 MySQL 사용 (`localhost:3306`)
  - 로컬 Redis 사용 (`localhost:6379`)
  - Python 챗봇 서버: `http://localhost:5000`
  - JPA DDL: `update` (테이블 자동 생성/수정)
  - WebAuthn: `localhost` 도메인

### 프로덕션 환경
- 파일: `.env.production`
- 사용 시나리오: AWS 배포 환경
- 특징:
  - AWS RDS MySQL 사용
  - AWS ElastiCache Redis 사용
  - Python 챗봇 서버: `http://silverlink-ai:5000` (Docker 내부 네트워크)
  - JPA DDL: `validate` (테이블 검증만, 수정 안 함)
  - WebAuthn: CloudFront 도메인

## 사용 방법

### 1. 로컬 개발 시
```bash
# .env.local을 .env로 복사
cp .env.local .env

# MySQL 비밀번호 수정
# .env 파일에서 DB_PASSWORD를 실제 로컬 MySQL 비밀번호로 변경

# Spring Boot 실행
./gradlew bootRun
```

### 2. 프로덕션 배포 시
```bash
# .env.production을 .env로 복사
cp .env.production .env

# Docker Compose 또는 ECS에서 실행
docker-compose up -d
```

### 3. Python 챗봇 서버 실행

#### 로컬 개발
```bash
cd SilverLink-AI
python -m uvicorn app.main:app --host 0.0.0.0 --port 5000 --reload
```

또는 배치 파일 사용:
```bash
cd SilverLink-AI
run_api.bat
```

#### 프로덕션
Docker Compose에서 자동으로 실행됩니다.

## 주요 설정 항목

### 챗봇 Python 서버 URL
- **로컬**: `CHATBOT_PYTHON_URL=http://localhost:5000`
- **프로덕션**: `CHATBOT_PYTHON_URL=http://silverlink-ai:5000`

### 데이터베이스
- **로컬**: `DB_HOST=localhost`
- **프로덕션**: `DB_HOST=database-1-silverlink.c7uoqu4iemyb.ap-northeast-2.rds.amazonaws.com`

### Redis
- **로컬**: `SPRING_DATA_REDIS_HOST=localhost`
- **프로덕션**: `SPRING_DATA_REDIS_HOST=master.silverlink-valkey.gox88q.apn2.cache.amazonaws.com`

### JPA DDL
- **로컬**: `JPA_DDL_AUTO=update` (개발 편의성)
- **프로덕션**: `JPA_DDL_AUTO=validate` (안전성)

## 보안 주의사항

⚠️ **절대 Git에 커밋하지 말 것:**
- `.env`
- `.env.local`
- `.env.production`

✅ **Git에 커밋해도 되는 것:**
- `.env.example` (샘플 파일, 실제 값 없음)
- `README_ENV.md` (이 문서)

## 트러블슈팅

### 챗봇 연결 오류 (Connection refused)
1. Python 챗봇 서버가 실행 중인지 확인
2. 포트 번호 확인 (5000)
3. `.env` 파일의 `CHATBOT_PYTHON_URL` 확인

### 데이터베이스 연결 오류
1. MySQL 서버 실행 확인
2. `.env` 파일의 DB 설정 확인
3. 방화벽 설정 확인 (3306 포트)

### Redis 연결 오류
1. Redis 서버 실행 확인
2. `.env` 파일의 Redis 설정 확인
3. 방화벽 설정 확인 (6379 포트)
