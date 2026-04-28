# OCR 약 식별 파이프라인 — Phase 1~6 구현 완료 보고서

> **프로젝트**: SilverLink AI — 어르신 복약 관리 시스템  
> **모듈**: OCR 약 식별 파이프라인  
> **작성일**: 2026-04-28  
> **테스트**: 37 passed, 0 failed

---

## 전체 파이프라인 구조

```text
[Luxia OCR 원문]
    ↓
Stage 1: OCR 텍스트 수신
    ↓
Stage 2: TextNormalizer 정규화                    ← Phase 1
    ↓
Stage 2.5: LLM Structured Extraction (Optional)  ← Phase 6
    ↓ merge (기존 후보에 없는 것만 추가)
Stage 3: MySQL 다중 매칭 (1차)                    ← Phase 2
    ↓ exact → alias → error_alias → prefix → ngram → fuzzy
Stage 4: VectorDB 2차 매칭 (fallback)
    ↓
Stage 5: Rule Validation (evidence 태깅)          ← Phase 1
    ↓
Stage 6: Pseudo-Confidence 재계산                 ← Phase 3
    ↓
Stage 7: Decision + LLM 설명 생성                 ← Phase 4 (안전 guard)
    ↓
[OCR Result 자동 저장]                            ← Phase 5
    ↓
[API 응답 (request_id 포함)]
```

---

## Phase 1. MVP 안정화 (완료)

### 목표
정확한 OCR 텍스트에 대해 안정적으로 `MATCHED` 처리하고, 애매한 경우 `NEED_USER_CONFIRMATION` 또는 `AMBIGUOUS`로 처리한다.

### 구현 내용

#### TextNormalizer (`text_normalizer.py`)
- OCR 텍스트 라인 단위 정규화 및 약품명 후보 추출
- `5 0 0 mg`, `5OOmg` 같은 OCR spacing/zero 오인식 용량을 `500mg`으로 정규화
- `mg`, `ml`, `mcg`, `g`, `iu`와 한국어 단위 표현을 canonical unit으로 통일
- 처방/환자/병원/복용법 라인을 약품명 후보에서 제외하는 필터
- OCR token confidence 기반 line confidence 계산

#### RuleValidator (`rule_validator.py`)
- 용량 비교를 숫자+단위 방식으로 강화 (예: `500mg` vs `50mg` 자동 확정 방지)
- 제조사 일치, 제형 일치, 성분 검증
- evidence 태깅 및 검증 메시지 생성 전담

#### MedicationPipeline (`medication_pipeline.py`)
- 7단계 파이프라인 오케스트레이터
- Low OCR confidence / top1-top2 gap 기준 상수화

#### FE 연동
- `SeniorOCR.tsx`에 `decision_status`, `match_confidence`, `requires_user_confirmation`, `decision_reasons` 표시
- 후보 카드에 match method, 함량 확인 배지, 검증 메시지 표시

#### BE 연동
- Spring Boot OCR proxy의 Python AI 기본 URL을 `http://localhost:8000`으로 통일
- Response DTO에 snake_case 필드 매핑 추가

### 관련 파일
| 파일 | 설명 |
|------|------|
| `app/ocr/services/text_normalizer.py` | OCR 텍스트 정규화 |
| `app/ocr/services/rule_validator.py` | 규칙 검증 / evidence 태깅 |
| `app/ocr/services/medication_pipeline.py` | 파이프라인 오케스트레이터 |
| `app/ocr/services/llm_descriptor.py` | LLM 기반 어르신 맞춤 설명 생성 |
| `app/ocr/services/vector_matcher.py` | VectorDB(ChromaDB) fallback 검색 |

### 검증
```
Python OCR compile: passed
OCR unit tests: 6 passed
Spring compileJava: passed
Frontend build: passed
```

---

## Phase 2. Alias 기반 후보 검색 (완료)

### 목표
OCR 오인식과 별칭 표현을 DB 기반으로 흡수하여, LLM 없이도 후보 검색 정확도를 향상한다.

### 구현 내용

#### Alias / Error Alias 테이블
- `medication_aliases`: 약품 대표 별칭 (브랜드명, 괄호 제거 등)
- `medication_error_aliases`: OCR 오인식 변형 (O↔0, l↔1, digit spacing 등)
- `DrugRepository`에 alias/error_alias 정확 일치 조회 추가

#### MySQL 검색 순서 재정렬
```text
exact → alias → error_alias → prefix → ngram → limited fuzzy
```

#### Alias 충돌 검증
- `MatchResult.alias_conflict` 필드 추가
- `MySQLMatcher._has_alias_conflict()`: 동일 별칭이 2개 이상 다른 `item_seq`에 매핑되면 충돌 감지
- 충돌 시 `AMBIGUOUS` + `requires_user_confirmation=True`

#### Seed 스크립트 (`scripts/seed_aliases.py`)
- `medications_master` 약품명에서 alias 자동 생성 (괄호 제거, 브랜드명 추출, 용량 제거)
- OCR 오인식 변형으로 error_alias 자동 생성
- `ON DUPLICATE KEY UPDATE` upsert 기반 재실행 안전

### 관련 파일
| 파일 | 설명 |
|------|------|
| `app/ocr/repository/drug_repository.py` | alias/error_alias 조회 추가 |
| `app/ocr/services/mysql_matcher.py` | 6단계 검색 순서, 충돌 검증 |
| `scripts/seed_aliases.py` | 초기 alias 생성 스크립트 |

### 검증
```
11 passed (mysql_matcher 5 + pipeline 6)
```

---

## Phase 3. Entity-weighted Pseudo-Confidence (완료)

### 목표
OCR confidence가 없거나 약한 상황에서도 검색 근거 기반 신뢰도를 산출하고, 용량/성분 충돌을 강하게 차단한다.

### 구현 내용

#### PseudoConfidenceScorer (`pseudo_confidence_scorer.py`)
- 7가지 세부 지표 기반 점수 구조화 (`ScoreBreakdown`)

| 지표 | 가중치 | 설명 |
|------|--------|------|
| `name_match` | 0~0.30 | 약품명 매칭 점수 |
| `strength_match` | 0~0.25 | 용량 일치 |
| `unit_match` | 0~0.15 | 단위 일치 |
| `form_match` | 0~0.10 | 제형 일치 |
| `manufacturer_match` | 0~0.10 | 제조사 일치 |
| `top_gap` | 0~0.10 | top1-top2 격차 보너스 |
| `penalty` | -0.40~ | 충돌 패널티 |

- 2-pass 방식: 1차 점수 계산 → 단일 최고점에만 gap 보너스 부여
- 용량 불일치 강한 패널티 (-0.40)
- 낮은 OCR 신뢰도 패널티 (-0.10)

#### RuleValidator 역할 분리
- 점수 계산 로직 제거 → evidence 태깅 및 검증 메시지 생성만 담당

### 관련 파일
| 파일 | 설명 |
|------|------|
| `app/ocr/services/pseudo_confidence_scorer.py` | 신뢰도 계산 모듈 |
| `app/ocr/model/drug_model.py` | `ScoreBreakdown` 모델 |
| `tests/unit_tests/test_pseudo_confidence.py` | 단위 테스트 |

### 검증
```
18 passed (pseudo_confidence 8 + mysql_matcher 5 + pipeline 5)
```

---

## Phase 4. 안전성 보완 (완료)

### 목표
운영 환경 위험 요소 제거 및 성능 최적화. *(원 계획의 Redis cache-aside는 향후 과제로 분리)*

### 구현 내용

#### MySQL 매칭 성능 최적화
- **문제**: `_try_fuzzy`에서 5,000건 전체 DB 조회 (Full Scan)
- **해결**: `ngram_match()` 기반 후보 풀을 최대 50건으로 축소 후 `rapidfuzz` 적용
- `fetch_all_for_fuzzy` 메서드 Deprecated 처리

#### VectorDB 단독 자동 확정 방지
- **문제**: VectorDB/RAG 검색 결과만으로 `MATCHED` 자동 확정 위험
- **해결**: VectorDB 단독 최상위 후보 → `NEED_USER_CONFIRMATION` 강제 전환

#### 스레드 안전성 확보
- **문제**: `DrugDictionaryIndex` 멀티 스레드 race condition
- **해결**: `threading.Lock` + 원자적 참조 교체 (Atomic Swap) 방식 적용

### 관련 파일
| 파일 | 변경 내용 |
|------|-----------|
| `app/ocr/services/mysql_matcher.py` | ngram 기반 fuzzy 제한 |
| `app/ocr/services/medication_pipeline.py` | VectorDB guard |
| `app/ocr/services/drug_dictionary_index.py` | 스레드 안전성 |
| `app/ocr/repository/drug_repository.py` | Deprecated 처리 |

### 검증
```
28 passed, 0 failed
```

---

## Phase 5. OCR 결과 저장 & 사용자 확인 API (완료)

### 목표
OCR 파이프라인 결과와 판단 근거를 추적 가능하게 저장하고, 자동 확정 불가 케이스를 사용자 확인 워크플로우로 연결한다.

### 구현 내용

#### 5-A: OCR Result Logging

##### `medication_ocr_results` 테이블
```sql
-- 주요 컬럼
request_id          VARCHAR(36)   -- UUID, 요청별 고유 ID
elderly_user_id     BIGINT        -- 어르신 사용자 ID
raw_ocr_text        TEXT          -- OCR 원문
normalized_names    JSON          -- 정규화된 약품명 후보
candidates          JSON          -- 매칭 후보 전체 (score, method, evidence)
pipeline_stages     JSON          -- 단계별 소요시간
decision_status     VARCHAR(30)   -- 최종 판정 상태
match_confidence    DECIMAL(5,3)  -- 매칭 신뢰도
decision_reasons    JSON          -- 판정 사유
best_drug_item_seq  VARCHAR(20)   -- 최고 후보 item_seq
user_confirmed      TINYINT(1)    -- NULL=미확인, 1=확정, 0=거부
user_selected_seq   VARCHAR(20)   -- 사용자 선택 item_seq
```

##### 비차단 자동 저장
- 파이프라인 완료 시 `_save_result()` 자동 호출
- 실패해도 메인 응답에 영향 없음 (fire-and-forget)
- 실패 시 `request_id`와 오류 로그 기록

#### 5-B: 사용자 확인 API

| API | 설명 |
|-----|------|
| `POST /ocr/confirm-medication` | 사용자가 후보 확정/거부 |
| `GET /ocr/pending-confirmations/{elderly_user_id}` | 미확인 건 목록 조회 |

##### 확인 요청 스키마
```json
{
  "requestId": "UUID",
  "selectedItemSeq": "사용자가 선택한 item_seq",
  "confirmed": true
}
```

#### 5-C: Alias Suggestion (피드백 학습)

##### `medication_alias_suggestions` 테이블
```sql
item_seq            VARCHAR(20)   -- 품목기준코드
alias_name          VARCHAR(200)  -- 제안된 별칭
review_status       VARCHAR(20)   -- PENDING / APPROVED / REJECTED
frequency           INT           -- 동일 제안 횟수 (자동 증가)
```

##### 안전 설계 원칙
1. 사용자 선택 alias → `PENDING` 상태로만 저장
2. **관리자 승인 전까지 LocalDrugIndex 검색 대상 미포함**
3. 동일 (item_seq, alias_name) 중복 시 `frequency` 자동 증가
4. 향후 관리자 승인 시 실제 alias 테이블에 반영

### 관련 파일
| 파일 | 설명 |
|------|------|
| `app/ocr/model/ocr_result_model.py` | `OcrResultRecord` 모델 |
| `app/ocr/repository/ocr_result_repository.py` | 결과 저장/조회 CRUD |
| `app/ocr/repository/alias_suggestion_repository.py` | alias 제안 CRUD |
| `app/ocr/schema/medication_schema.py` | Confirm/Pending 스키마 |
| `app/api/endpoints/ocr.py` | API 엔드포인트 |
| `app/core/container.py` | DI 등록 |

### 검증
```
28 passed, 0 failed
```

---

## Phase 6. Optional LLM Structured Extraction (완료)

### 목표
복잡한 OCR 텍스트에서 LLM으로 약품명/용량/복용법을 구조화 추출한다. LLM 결과는 hint로만 사용하며, 최종 결정은 DB/Rule에 맡긴다.

### 구현 내용

#### LLMExtractor (`llm_extractor.py`)

##### 프롬프트 설계
- `temperature=0.0` 으로 일관성 확보
- "존재하지 않는 약품명을 만들어내지 마세요" (hallucination 금지)
- "확실하지 않으면 빈 배열 반환" 지시
- OCR 오류 교정 가이드 포함 (`5 0 0mg` → `500mg`, `5OOmg` → `500mg`)

##### 응답 형식
```json
{
  "medications": [
    {
      "name": "약품명",
      "dosage": "용량 또는 null",
      "form_type": "제형 또는 null",
      "original_fragment": "추출된 원문 부분"
    }
  ]
}
```

#### 3중 안전 장치

| 장치 | 위치 | 설명 |
|------|------|------|
| Hallucination Guard | `LLMExtractor._verify_in_source()` | 추출된 이름이 OCR 원문에 존재하는지 검증 |
| Evidence Tagging | Pipeline Stage 3 | `evidence.normalized_source = "llm_hint"` 태깅 |
| Decision Guard | `_decide()` | llm_hint 단독 최상위 후보 → `NEED_USER_CONFIRMATION` |

#### Pipeline 통합
- Stage 2 (정규화) 후, Stage 3 (MySQL 매칭) 전에 **Stage 2.5**로 삽입
- 기존 정규화 결과에 없는 후보만 merge (중복 방지)
- `NormalizedDrug.source` 필드: `normalizer` (기존) / `llm_hint` (LLM 추출)
- LLM 실패 시 기존 deterministic pipeline 그대로 동작

### 관련 파일
| 파일 | 설명 |
|------|------|
| `app/ocr/services/llm_extractor.py` | LLM 구조화 추출 모듈 (신규) |
| `app/ocr/model/drug_model.py` | `NormalizedDrug.source` 필드 추가 |
| `app/ocr/services/medication_pipeline.py` | Stage 2.5 삽입 + llm_hint guard |
| `app/core/container.py` | DI 등록 |
| `tests/unit_tests/test_llm_extractor.py` | 단위 테스트 9건 (신규) |

### 검증
```
37 passed, 0 failed (llm_extractor 9건 추가)
```

---

## 전체 파일 구조

```text
app/ocr/
├── model/
│   ├── drug_model.py               # DrugInfo, MatchCandidate, PipelineResult, ScoreBreakdown 등
│   └── ocr_result_model.py         # OcrResultRecord (Phase 5)
├── repository/
│   ├── drug_repository.py          # medications_master, alias, error_alias CRUD
│   ├── drug_vector_repository.py   # ChromaDB 벡터 저장소
│   ├── ocr_result_repository.py    # medication_ocr_results CRUD (Phase 5)
│   └── alias_suggestion_repository.py  # medication_alias_suggestions CRUD (Phase 5)
├── schema/
│   └── medication_schema.py        # API 요청/응답 스키마
└── services/
    ├── text_normalizer.py          # Stage 2: OCR 텍스트 정규화 (Phase 1)
    ├── llm_extractor.py            # Stage 2.5: LLM 구조화 추출 (Phase 6)
    ├── mysql_matcher.py            # Stage 3: MySQL 6단계 매칭 (Phase 2)
    ├── drug_dictionary_index.py    # 인메모리 약품 인덱스 (Phase 4)
    ├── vector_matcher.py           # Stage 4: VectorDB fallback
    ├── rule_validator.py           # Stage 5: 규칙 검증 (Phase 1/3)
    ├── pseudo_confidence_scorer.py # Stage 6: 신뢰도 계산 (Phase 3)
    ├── medication_pipeline.py      # 파이프라인 오케스트레이터
    └── llm_descriptor.py           # Stage 7: 어르신 맞춤 설명

app/api/endpoints/
└── ocr.py                          # API 엔드포인트 (Phase 5 confirm/pending 추가)

app/core/
└── container.py                    # DI Container

tests/unit_tests/
├── test_medication_pipeline.py     # 7건
├── test_mysql_matcher.py           # 6건
├── test_drug_dictionary_index.py   # 7건
├── test_pseudo_confidence.py       # 7건 (Phase 3)
└── test_llm_extractor.py          # 9건 (Phase 6)
```

---

## DB 테이블 구조

| 테이블 | Phase | 설명 |
|--------|-------|------|
| `medications_master` | 기존 | 약품 마스터 데이터 |
| `medication_aliases` | Phase 2 | 약품 대표 별칭 |
| `medication_error_aliases` | Phase 2 | OCR 오인식 별칭 |
| `medication_ocr_results` | Phase 5 | OCR 파이프라인 결과 로그 |
| `medication_alias_suggestions` | Phase 5 | 사용자 피드백 alias 제안 (PENDING) |

---

## 설계 원칙 요약

| 원칙 | 설명 |
|------|------|
| **MySQL = Source of Truth** | 모든 약품 데이터의 정답은 MySQL에만 존재 |
| **LLM ≠ 확정 주체** | LLM 결과는 hint로만 사용, 자동 MATCHED 금지 |
| **VectorDB = 후보 보완** | 벡터 검색은 fallback 보완용, 최종 결정 불가 |
| **용량 > 이름** | 용량/단위 불일치는 이름 유사도보다 강하게 판단 |
| **자동 등록 ≠ 기본값** | 확정 불가 시 반드시 사용자 확인 요청 |
| **Alias ≠ 즉시 반영** | 사용자 피드백 alias는 PENDING → 관리자 승인 후 활성화 |
| **비차단 저장** | 결과 로깅 실패가 메인 응답에 영향 주지 않음 |

---

## 향후 과제

| 항목 | 설명 |
|------|------|
| Redis cache-aside | 자주 반복되는 OCR/alias 검색 부하 감소 |
| FE 사용자 확인 UI | `NEED_USER_CONFIRMATION` 상태의 후보 선택 UI |
| 관리자 Alias 승인 UI | PENDING alias 검토/승인/거부 관리 화면 |
| 이미지 원본 저장 | OCR 원본 이미지 S3 저장 (후순위) |
