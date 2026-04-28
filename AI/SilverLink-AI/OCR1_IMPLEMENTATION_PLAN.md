# OCR1 구현 계획

## 구현 진행 기록

### 2026-04-27 Phase 1 MVP 안정화 일부 완료

- `TextNormalizer`를 실제 한글/영문 OCR 텍스트 기준으로 재정리했다.
- `5 0 0 mg`, `5OOmg` 같은 OCR spacing/zero 오인식 용량을 `500mg` 형태로 정규화한다.
- `mg`, `ml`, `mcg`, `g`, `iu`와 주요 한국어 단위 표현을 canonical unit으로 통일한다.
- 처방/환자/병원/복용법 라인은 약품명 후보에서 제외하는 기본 필터를 추가했다.
- `RuleValidator`의 용량 비교를 숫자만 비교하는 방식에서 숫자+단위 비교로 강화했다.
- `500mg` OCR이 `50mg` DB 후보와 매칭되는 경우 `NEED_USER_CONFIRMATION`으로 내려가도록 강한 패널티와 evidence를 추가했다.
- `MedicationPipeline`의 low OCR confidence/top1-top2 gap 기준을 상수화했다.
- Spring Boot OCR proxy의 Python AI 기본 URL을 `http://localhost:8000`으로 맞췄다.
- Spring Boot OCR validation response DTO에 Python AI 응답의 snake_case 필드 매핑을 추가했다.

검증:

```bash
python -m compileall -f app\ocr app\api\endpoints\ocr.py
python -m pytest tests\unit_tests\test_medication_pipeline.py -q
.\gradlew.bat compileJava
```

결과:

```text
Python OCR compile: passed
OCR unit tests: 6 passed
Spring compileJava: passed with 2 existing deprecation warnings
```

### 2026-04-27 Phase 1 FE 사용자 확인 표시 추가

- `SeniorOCR.tsx`의 OCR 검증 응답 타입에 `decision_status`, `match_confidence`, `requires_user_confirmation`, `decision_reasons`, `evidence`, `validation_messages`를 추가했다.
- OCR 결과 카드에 최종 판정 배지와 신뢰도, 등록 전 확인 여부, 판정 사유를 표시한다.
- 후보 약품 카드에 match method, 업체명, 함량 확인 배지, 검증 메시지를 표시한다.
- 함량 충돌(`evidence.strength_match === false`)도 낮은 confidence와 동일하게 확인 필요로 표시한다.

검증:

```bash
npm ci
npm run build
```

결과:

```text
Frontend build: passed
```

참고:

- `npm ci`는 최초 sandbox 권한 문제로 실패해 권한 승인 후 재실행했다.

### 2026-04-27 Phase 3 Entity-weighted pseudo-confidence 완료

- `ScoreBreakdown` 모델을 추가하여 7가지 세부 지표(name, strength, unit, form, manufacturer, top_gap, penalty) 기반 점수를 구조화했다.
- `PseudoConfidenceScorer` 모듈을 신규 추가했다.
  - 2-pass 방식으로 1차 점수 계산 후, 단일 최고점에만 top1-top2 gap 보너스를 부여하여 공정성을 보장했다.
  - 용량 불일치(-0.40)와 낮은 OCR 신뢰도(-0.10)에 대한 강한 패널티를 적용했다.
- `RuleValidator`를 리팩터링하여 점수 계산 로직을 제거하고, 순수하게 evidence 태깅 및 검증 메시지 생성만 담당하도록 분리했다.
- `MedicationPipeline`에 Scorer를 Stage 6로 통합했다.
- 단위 테스트(`test_pseudo_confidence.py`)를 추가하여 용량 충돌 시 점수 급감(자동 확정 방지), gap 보너스, 제형/제조사 보너스 등을 검증했다.

검증:

```bash
python -m pytest tests\unit_tests\test_pseudo_confidence.py tests\unit_tests\test_mysql_matcher.py tests\unit_tests\test_medication_pipeline.py -v
python -m compileall -f app\ocr
```

결과:

```text
18 passed (pseudo_confidence 8 + mysql_matcher 5 + pipeline 5)
Python OCR compile: passed
```

### 2026-04-27 Phase 2 alias 충돌 검증 및 seed 스크립트 완료

- `MatchResult`에 `alias_conflict` 필드를 추가했다.
- `MySQLMatcher._has_alias_conflict()`로 alias/error_alias 후보가 2개 이상 서로 다른 `item_seq`에 매핑되면 충돌을 감지한다.
- `MedicationPipeline._decide()`에서 `alias_conflict=True`이면 `AMBIGUOUS` + `requires_user_confirmation=True`로 판정한다.
- `scripts/seed_aliases.py`를 추가했다.
  - `medications_master` 약품명에서 괄호 제거, 브랜드명 추출, 용량 제거로 alias를 생성한다.
  - OCR 오인식 변형(O/0, l/1, digit spacing 등)으로 error_alias를 생성한다.
  - `ON DUPLICATE KEY UPDATE` 기반 upsert로 재실행 안전하다.

검증:

```bash
python -m pytest tests\unit_tests\test_mysql_matcher.py tests\unit_tests\test_medication_pipeline.py -v
python -m compileall -f app\ocr scripts\seed_aliases.py
```

결과:

```text
11 passed (mysql_matcher 5 + pipeline 6)
Python OCR + seed script compile: passed
```

### 2026-04-27 Phase 2 alias/error alias 검색 계층 추가

- `DrugRepository`에 `medication_aliases` 정확 일치 조회를 추가했다.
- `DrugRepository`에 `medication_error_aliases` OCR 오인식 alias 조회를 추가했다.
- `create_table_if_not_exists()`가 `medications_master`와 함께 alias/error alias 테이블을 생성/확인하도록 확장했다.
- `MySQLMatcher` 검색 순서를 `exact -> alias -> error_alias -> prefix -> ngram -> fuzzy`로 재정렬했다.
- alias 후보는 `mysql_alias`, error alias 후보는 `mysql_error_alias` evidence를 포함한다.

검증:

```bash
python -m pytest tests\unit_tests\test_mysql_matcher.py tests\unit_tests\test_medication_pipeline.py -q
python -m compileall -f app\ocr
```

결과:

```text
MySQL matcher + OCR pipeline unit tests: 8 passed
Python OCR compile: passed
```
- Vite 빌드는 browserslist 데이터 오래됨, 일부 chunk 500KB 초과 경고를 출력했지만 실패하지 않았다.

### 2026-04-28 Phase 4-A 안전성 보완 완료

- `mysql_matcher.py`의 `_try_fuzzy` 5000건 full scan 조회를 제거하고 `ngram_match` 기반으로 후보 풀(최대 50건)을 축소한 후 fuzzy 검색을 수행하도록 개선했다.
- VectorDB/RAG 검색 결과만으로 약품이 `MATCHED` 상태로 자동 확정되는 것을 방지하고 강제로 `NEED_USER_CONFIRMATION` 상태로 전환하는 안전 장치를 추가했다.
- `DrugDictionaryIndex.ensure_loaded()`에 `threading.Lock`과 원자적 참조 교체(atomic swap)를 도입하여 멀티 스레드 환경의 race condition을 해결했다.
- `drug_repository.py`의 위험한 메서드(`fetch_all_for_fuzzy`)를 Deprecated 처리했다.
- 모든 단위 테스트 통과(28건) 확인.

### 2026-04-28 Phase 5 OCR 결과 저장 및 사용자 확인 API 완료

- OCR 파이프라인 결과를 저장할 `OcrResultRecord` 모델 및 `OcrResultRepository` 생성 (`medication_ocr_results` 테이블).
- 파이프라인에서 응답에 영향 없이 결과를 자동 저장하는 비차단(non-blocking) 저장 로직 추가.
- `medication_alias_suggestions` 테이블 추가. 사용자가 PENDING 상태의 alias로 자동 제안(`AliasSuggestionRepository`)할 수 있도록 구현. 동일 제안 시 `frequency` 자동 증가 기능.
- `POST /ocr/confirm-medication` 및 `GET /ocr/pending-confirmations/{elderly_user_id}` API 구현.
- `PipelineResult` 및 API 응답 스키마에 `request_id` 추가하여 기존 구조 유지.

### 2026-04-28 Phase 6 Optional LLM Structured Extraction 완료

- `LLMExtractor` 신규 모듈 추가 (`app/ocr/services/llm_extractor.py`).
  - OCR 원문에서 약품명/용량/제형을 JSON 구조화 추출.
  - Hallucination 방지: 추출된 약품명이 OCR 원문에 존재하는지 검증 (`_verify_in_source`).
  - 실패 시 빈 리스트 반환 (non-blocking fallback).
- `NormalizedDrug.source` 필드 추가 (`normalizer` / `llm_hint`).
- 파이프라인에 Stage 2.5 LLM extraction 단계 삽입. 기존 정규화 결과에 없는 후보만 merge.
- `_decide()`에 LLM hint 단독 후보 자동확정 방지 guard 추가.
- 단위 테스트 9건 추가. 전체 37건 통과.

## 1. 현재 판단

`OCR1.txt`의 방향은 현재 SilverLink 구조에서 구현 가능하다.

다만 현재 코드는 전체 설계의 완성본이 아니라, MySQL 후보 검색과 규칙 검증을 중심으로 한 MVP 골격이 먼저 들어간 상태다. 문서에서 제안한 Redis fast lookup, alias/error alias, entity-weighted pseudo-confidence, OCR raw/result 저장, 사용자 확인 흐름은 추가 구현이 필요하다.

## 2. 현재 구현된 범위

### AI 서버

- `app/ocr/services/medication_pipeline.py`
  - OCR 텍스트 수신
  - 텍스트 정규화
  - MySQL 후보 검색
  - VectorDB fallback
  - Rule validation
  - Decision
  - LLM 설명 생성

- `app/ocr/services/mysql_matcher.py`
  - exact match
  - prefix match
  - MySQL ngram fulltext match
  - fuzzy match

- `app/ocr/services/text_normalizer.py`
  - OCR 텍스트 라인 단위 정규화
  - 약품명 후보 추출
  - 용량/제형 일부 추출
  - OCR token confidence가 있으면 line confidence 계산

- `app/ocr/services/rule_validator.py`
  - 제조사 일치 보너스
  - 용량 일치/불일치 검증
  - 제형 일치 검증
  - OCR confidence 낮음 처리

- `app/ocr/services/vector_matcher.py`
  - MySQL 후보 점수가 낮을 때 ChromaDB fallback 검색

- `app/ocr/services/llm_descriptor.py`
  - 최종 후보를 기반으로 사용자 설명 생성
  - LLM이 약품을 임의 확정하거나 새 정보를 생성하지 않도록 프롬프트 제약 포함

### Backend

- `domain/ocr/OcrController.java`
  - Luxia Document AI 호출

- `domain/ocr/controller/OcrProxyController.java`
  - Spring Boot에서 Python AI 서버의 OCR 검증 API 호출

- `domain/medication`
  - 복약 일정 등록/조회/삭제
  - OCR 로그 엔티티 일부 존재

### Frontend

- `src/api/ocr.ts`
  - OCR API 호출

- `src/pages/senior/SeniorOCR.tsx`
  - OCR 결과 검증 API 호출 흐름 일부 구현

- `src/pages/senior/SeniorMedication.tsx`
  - OCR 결과 기반 복약 등록 흐름 일부 구현

## 3. 검증 결과

아래 검증은 통과했다.

```bash
python -m compileall -f app\ocr app\api\endpoints\ocr.py
python -m pytest tests\unit_tests\test_medication_pipeline.py -q
```

결과:

```text
4 passed
```

## 4. 주요 미구현 항목

### Redis fast lookup

`OCR1.txt`에서 제안한 Redis key 구조는 아직 구현되어 있지 않다.

필요 key 예시:

```text
drug:exact:{normalized_name}
drug:alias:{normalized_alias}
drug:error_alias:{synthetic_ocr_error}
drug:ngram:{token}
ocr:candidate:{hash(raw_text)}
drug:detail:{drug_id}
```

### Alias / error alias

현재 MySQL 검색은 `medications_master` 중심이다.

추가 필요:

- `drug_alias`
- `drug_error_alias`
- alias 충돌 검증
- synthetic OCR-error alias 생성/관리

### Pseudo-confidence

현재 점수는 후보 검색 score에 규칙 보너스/패널티를 더하는 수준이다.

OCR1에서 제안한 entity-weighted pseudo-confidence를 별도 점수 모델로 분리해야 한다.

필요 요소:

- 제품명 exact/alias/error_alias 점수
- 성분 일치 점수
- 용량 일치 점수
- 단위 일치 점수
- 제형 일치 점수
- 제조사 일치 점수
- top1/top2 gap
- 용량 충돌 강한 패널티
- 성분 충돌 강한 패널티

### Raw OCR 저장

BE에 `MedicationOcrLog` 엔티티는 있지만, AI 파이프라인의 raw text/result/evidence 저장과 완전히 연결되어 있지 않다.

필요 저장 항목:

- raw OCR text
- normalized candidates
- search candidates
- decision status
- match confidence
- evidence
- warnings
- selected drug id
- user confirmation result

### 후보 검색 성능

현재 fuzzy는 `fetch_all_for_fuzzy(limit=5000)` 이후 Python에서 rapidfuzz를 계산한다.

데이터가 커지면 병목이 될 수 있다.

개선 방향:

```text
exact / alias / error_alias / prefix
-> 부족하면 ngram으로 후보 20~50개 축소
-> 축소 후보에만 fuzzy 계산
```

### API 경로/환경 설정

Spring Boot 기본 Python AI URL은 `http://localhost:5000`이고, AI 서버 기본 포트는 `8000`이다.

배포/로컬 실행 전에 다음 값을 맞춰야 한다.

```yaml
chatbot:
  python:
    url: http://localhost:8000
```

또한 AI API는 `/api/ocr/validate-medication` 경로로 노출된다.

## 5. 구현 우선순위

## Phase 1. 안정적인 MVP 정리

목표:

- 현재 파이프라인을 운영 가능한 수준으로 정리
- 정확한 OCR 텍스트에 대해 안정적으로 `MATCHED` 처리
- 애매한 경우 `NEED_USER_CONFIRMATION` 또는 `AMBIGUOUS` 처리

작업:

- MySQL `medications_master` 스키마 확인 및 migration 정리
- `item_name_normalized` 생성 규칙 통일
- `TextNormalizer` 한글/영문/단위 정규화 개선
- `RuleValidator`의 용량/단위 비교 정확도 개선
- `Decision` 기준값 명확화
- BE `chatbot.python.url` 기본값을 AI 서버 포트와 맞춤
- FE에서 `decision_status`, `requires_user_confirmation`, `decision_reasons` 표시

완료 기준:

- exact/prefix 기반 정상 약품은 `MATCHED`
- 용량이 불명확하면 자동 등록하지 않고 사용자 확인
- 상위 후보 점수 차이가 작으면 `AMBIGUOUS`
- 단위 테스트와 최소 API 통합 테스트 통과

## Phase 2. Alias 기반 후보 검색

목표:

- OCR 흔들림과 별칭 표현을 DB 기반으로 흡수
- LLM 없이도 후보 검색 정확도 향상

작업:

- `drug_alias` 테이블 추가
- `drug_error_alias` 테이블 추가
- alias repository 추가
- MySQL 검색 순서 변경

검색 순서:

```text
exact
alias
error_alias
prefix
ngram
limited fuzzy
```

- alias 충돌 검증 로직 추가
- public data 또는 기존 약품명에서 초기 alias 생성 스크립트 작성

완료 기준:

- 대표 alias로 동일 drug_id 검색 가능
- OCR error alias로 동일 drug_id 검색 가능
- alias 충돌 시 자동 확정하지 않음

## Phase 3. Entity-weighted pseudo-confidence

목표:

- OCR confidence가 없거나 약한 상황에서도 검색 근거 기반 신뢰도 산출
- 용량/성분 충돌은 강하게 차단

작업:

- `PseudoConfidenceScorer` 신규 모듈 추가
- score breakdown 모델 추가
- top1/top2 gap 계산 명시화
- `RuleValidator`와 scoring 역할 분리
- response에 score breakdown 포함

예상 구조:

```json
{
  "score": 0.91,
  "breakdown": {
    "name_match": 0.25,
    "alias_match": 0.20,
    "strength_match": 0.30,
    "unit_match": 0.20,
    "top_gap": 0.10,
    "penalty": 0.00
  }
}
```

완료 기준:

- `5mg` vs `50mg` vs `500mg` 케이스에서 자동 확정 방지
- 성분 충돌 후보는 제외 또는 `AMBIGUOUS`
- 점수 근거가 API 응답에 포함됨

## Phase 4. Redis fast lookup

목표:

- 자주 반복되는 OCR/alias/ngram 검색 부하 감소
- MySQL source of truth 원칙 유지

작업:

- AI 또는 BE 중 Redis 책임 위치 결정
- Redis client 구성
- cache-aside wrapper 추가
- key namespace/TTL 정책 정의
- master 변경 시 cache invalidation 전략 추가

권장 TTL:

```text
exact/alias cache: master 변경 전까지 또는 수동 무효화
error alias cache: 배포/동기화 시 갱신
ocr candidate result: 1시간~1일
drug detail summary: 1일 또는 master 변경 시 무효화
```

완료 기준:

- 동일 OCR raw text 재요청 시 cache hit
- Redis 장애 시 MySQL 경로로 fallback
- Redis가 정답 저장소처럼 쓰이지 않음

## Phase 5. OCR raw/result 저장과 사용자 확인 루프 (완료)

목표:

- OCR 원문과 판단 근거를 추적 가능하게 저장
- 자동 확정 불가 케이스를 사용자 확인으로 연결

작업:

- `MedicationOcrLog` 확장 또는 별도 result table 추가
- AI response 전체 evidence 저장
- 사용자 선택 후보 저장
- FE 확인 UI 개선 (현재 API 단계까지만 진행)
- 사용자가 고른 후보를 alias/error_alias 개선 데이터로 활용할 수 있게 로그화

완료 기준:

- OCR 요청별 raw/result/evidence 추적 가능
- `NEED_USER_CONFIRMATION` 상태에서 사용자가 후보 선택 가능
- 사용자 선택 결과가 추후 alias 개선 데이터로 남음

## Phase 6. Optional LLM structured extraction (완료)

목표:

- 복잡하게 붙은 OCR 텍스트에서 약품명/용량/복용법 후보를 구조화
- 최종 결정은 DB/Rule에 맡김

작업:

- JSON schema/Pydantic 기반 LLM extraction 모듈 추가
- LLM 결과를 candidate hint로만 사용
- hallucination 방지 프롬프트 강화
- LLM 실패 시 기존 deterministic pipeline 유지

완료 기준:

- LLM이 없는 환경에서도 기본 검색 동작
- LLM 결과만으로 `MATCHED` 확정하지 않음
- 구조화 결과는 evidence 또는 hint로 남음

## 6. 권장 최종 구조

```text
Luxia OCR
-> Raw OCR Store
-> Text Normalization
-> Optional LLM Structured Extraction
-> Entity Tagging
-> Redis Fast Lookup
-> MySQL Candidate Search
-> Candidate Merge
-> Entity-weighted Pseudo-Confidence
-> Rule Validation
-> Decision
-> VectorDB/RAG Fallback
-> LLM Explanation
-> FE User Confirmation / Medication Registration
```

## 7. 주의할 점

- LLM은 약품 확정 주체가 되면 안 된다.
- RAG/VectorDB는 최종 결정용이 아니라 후보 보완용이어야 한다.
- MySQL을 source of truth로 유지해야 한다.
- Redis는 캐시와 빠른 조회 계층으로만 사용해야 한다.
- 용량/단위 불일치는 이름 유사도보다 더 강하게 판단해야 한다.
- 자동 등록보다 사용자 확인이 필요한 케이스를 명확히 분리해야 한다.

## 8. 예상 작업 순서 요약

1. 현재 MVP 안정화 (완료)
2. MySQL alias/error alias 테이블 추가 (완료)
3. 검색 순서 재정렬 (완료)
4. pseudo-confidence scorer 분리 (완료)
5. OCR 로그/evidence 저장 및 확인 루프 API (완료)
6. Optional LLM structured extraction 도입 (완료)
7. Redis cache-aside 추가 (향후 과제)
8. FE 사용자 확인 UI 강화 (향후 과제)

## 9. 결론

현재 코드베이스는 `OCR1.txt`의 방향으로 확장하기 좋은 상태다.

단, 현재 구현은 전체 설계 중 MySQL 검색, VectorDB fallback, Rule Validation, LLM 설명 생성의 1차 골격 수준이다. 실사용 품질로 올리려면 alias/error alias, pseudo-confidence, Redis, 로그 저장, 사용자 확인 루프를 순서대로 추가해야 한다.
