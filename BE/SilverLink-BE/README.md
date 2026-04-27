# SilverLink Backend (SilverLink-BE)

## 1. 프로젝트 개요 (Project Overview)
**SilverLink** 프로젝트의 백엔드 서비스입니다. 시니어/독거노인을 위한 AI 기반 콜봇(AICC) 및 챗봇 모니터링 플랫폼의 핵심 비즈니스 로직과 데이터 관리를 담담당합니다.
AI 서버(FastAPI) 및 프론트엔드(React/TypeScript)와 연동되어 데이터의 영속성을 보장하고 관리자/상담사/보호자를 위한 안정적인 API를 제공합니다.

---

## 2. 주요 기능 (Key Features)

- **사용자 인증 및 권한 관리 (Auth/User)**
  - JWT(JSON Web Token) 기반 인증 및 Spring Security를 활용한 API 보안
  - 보호자, 상담사, 관리자 등 역할(Role) 기반의 세밀한 권한 제어
  - WebAuthn(Passkey) 기반 생체/간편 인증 지원
- **어르신 모니터링 및 응급/민원 관리 (Elderly/Emergency)**
  - 대상 어르신 정보 관리 및 일일 건강/감정 상태 모니터링
  - AI 콜봇/챗봇을 통해 감지된 응급 상황 및 민원 접수 로직 처리
  - Twilio를 활용한 SMS 알림 기능
- **AICC(콜봇/챗봇) 연동 (Call/Chatbot/Counseling)**
  - AI 서버(FastAPI)와의 WebClient 기반 비동기 HTTP 통신
  - 상담 콜 기록 저장, 감정 분석 결과 및 요약 데이터 관리
- **복약 관리 및 처방전 OCR (Medication/OCR)**
  - Luxia Document AI를 활용한 처방전 이미지 텍스트 추출(OCR) 비동기 처리 연동
  - 어르신 복약 일정 및 알림 설정 관리
  - AWS S3 기반의 안전한 처방전 이미지 파일 관리
- **복지 정보 제공 (Welfare)**
  - 공공데이터포털(국립사회보장정보원) 오픈 API 연동을 통한 지역/국가 복지 정보 제공

---

## 3. 기술 스택 (Tech Stack)

### 백엔드 코어 (Backend Core)
- **Language**: Java 21
- **Framework**: Spring Boot 3.x (3.2+)
- **Build Tool**: Gradle
- **Security**: Spring Security, JWT (jjwt), WebAuthn (webauthn-server-core)
- **Network**: Spring WebFlux (WebClient, Reactor Netty)

### 데이터베이스 및 스토리지 (Database & Storage)
- **RDBMS**: MySQL 8.x
- **In-Memory Cache**: Redis (세션/토큰 및 OTP 캐싱)
- **ORM**: Spring Data JPA, Hibernate
- **Storage**: AWS S3 SDK (s3)

### 외부 시스템 연동 (External APIs)
- **OCR**: Luxia Document AI
- **Message/MFA**: Twilio SDK (SMS, Verify)
- **Public Data**: 공공데이터포털 API 연동

### 기타 도구 (Other Tools)
- **Documentation**: Springdoc OpenAPI (Swagger UI)
- **Utils**: Lombok, ModelMapper, dotenv-java

---

## 4. 프로젝트 구조 (Project Structure)

```text
SilverLink-BE/
├── .github/                        # GitHub Actions 설정 (필요시)
├── build.gradle                    # 의존성 및 빌드 설정 파일
├── Dockerfile                      # 배포를 위한 도커 이미지 생성 설정
└── src/
    ├── main/
    │   ├── java/com/aicc/silverlink/
    │   │   ├── admin/              # 관리자 기능 도메인
    │   │   ├── auth/               # 인증 (JWT, WebAuthn) 도메인
    │   │   ├── call/               # 콜봇 통화/기록 관리
    │   │   ├── chatbot/            # 챗봇 메시지/세션 관리
    │   │   ├── counseling/         # 상담사 및 상담 기록 관리
    │   │   ├── elderly/            # 어르신 사용자 관리
    │   │   ├── emergency/          # 응급 상황 및 알림 도메인
    │   │   ├── guardian/           # 보호자 기능 도메인
    │   │   ├── medication/         # 복약 일정 및 약품 관리
    │   │   ├── ocr/                # Luxia API 연동 및 처방전 OCR
    │   │   ├── welfare/            # 공공데이터 연동 복지 정보
    │   │   └── global/             # 공통 예외 처리, 응답 포맷, 보안 필터 등
    │   └── resources/
    │       └── application.yml     # 환경 변수 및 스프링 기본 설정
    └── test/                       # 단위 테스트 및 통합 테스트
```

---

## 5. 시작하기 (Getting Started)

### 사전 요구사항 (Prerequisites)
- Java 21 이상 (JDK 21)
- MySQL 8.0 이상
- Redis Server
- AWS 계정 (S3 버킷 및 액세스 키)
- Twilio, Luxia Document AI API 키

### 환경 변수 설정
프로젝트 최상단 폴더에 `.env` 파일을 생성하거나 환경 변수를 주입해야 합니다.
`README_ENV.md` 또는 `.env.example` 파일을 참고하여 아래 값들을 설정하세요.

```env
# DB Settings
DB_HOST=localhost
DB_PORT=3306
DB_NAME=silverlink
DB_USER=root
DB_PASSWORD=your_db_password

# Redis
SPRING_DATA_REDIS_HOST=localhost
SPRING_DATA_REDIS_PORT=6379

# JWT Secret
JWT_SECRET=your_secret_key_here

# AWS S3 Settings
AWS_ACCESS_KEY_ID=your_aws_access_key
AWS_SECRET_ACCESS_KEY=your_aws_secret_key

# Third Party APIs
TWILIO_ACCOUNT_SID=your_twilio_sid
TWILIO_AUTH_TOKEN=your_twilio_token
TWILIO_VERIFY_SERVICE_SID=your_verify_sid
TWILIO_MESSAGING_SERVICE_SID=your_messaging_sid
API_SERVICE_KEY=your_public_data_api_key
LUXIA_API_KEY=your_luxia_api_key

# Other Services
CHATBOT_PYTHON_URL=http://localhost:8000
CALLBOT_API_URL=http://localhost:8000
```

### 실행 방법 

#### 1. 로컬 환경에서 직접 실행
```bash
# 의존성 설치 및 빌드
./gradlew clean build

# 스프링 부트 애플리케이션 실행
./gradlew bootRun
```

#### 2. Docker를 활용한 실행 (권장)
```bash
# Docker 이미지 빌드
docker build -t silverlink-be .

# 컨테이너 실행 (환경변수를 주입하거나 docker-compose를 활용)
docker run --env-file .env -p 8080:8080 silverlink-be
```

### API 명세서 확인 (Swagger)
서버 실행 후 브라우저에서 아래 경로로 접속하여 API 명세를 확인하고 테스트할 수 있습니다.
- http://localhost:8080/swagger-ui/index.html
