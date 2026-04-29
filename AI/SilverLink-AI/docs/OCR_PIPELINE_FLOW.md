# AI 기반 OCR 약 식별 파이프라인 상세 구조 및 흐름도

이 문서는 SilverLink AI의 OCR 기반 의약품 식별 파이프라인(`medication_pipeline.py`)의 전체 흐름과 각 단계별 내부 로직을 상세히 설명합니다. 파이프라인은 신뢰할 수 있는 매칭 결과를 도출하기 위해 총 5개의 Phase와 8개의 Stage로 구성되어 있습니다.

---

## Phase 1: 텍스트 수집 및 정규화 (전처리)

OCR 모터(Luxia)로부터 전달받은 날것(Raw)의 텍스트에서 노이즈를 제거하고 검색 가능한 형태의 '약품명 후보군'을 도출합니다. 특히 한국어 처방전과 약 봉투의 특성을 반영한 정교한 텍스트 필터링이 수행됩니다.

### Stage 1: OCR 텍스트 수신
- 📝 **관련 파일:** `app/api/endpoints/ocr.py` (API 진입점) 및 `app/ocr/services/medication_pipeline.py` (오케스트레이터)
- **Input:** 처방전/약봉투의 Raw OCR 텍스트 문자열(개행 포함) 및 단어별 신뢰도가 담긴 `Token Confidence` 배열.
- **Logic:** 전체 파이프라인 진행 및 DB 비차단 로깅 추적을 위해 고유한 `Request ID (UUID)`를 발급하고, 텍스트가 너무 짧거나 비어있는지 유효성을 1차 검증하여 불필요한 연산을 방지합니다.

### Stage 2: 텍스트 정규화 (`TextNormalizer`)
- 📝 **관련 파일:** `app/ocr/services/text_normalizer.py`
- **주요 목적:** OCR의 고질적인 오인식을 교정하고, 노이즈 행을 제거하여 순수한 약품명 객체(`NormalizedDrug`) 단위로 분리합니다.
- **핵심 정규화 로직:**
  1. **띄어쓰기 및 오인식 교정 (Regex 매칭):**
     - 숫자 중간의 불필요한 공백 결합 (`SPACED_DIGIT_DOSAGE_PATTERN`): 예) `5 0 0 mg` → `500mg`
     - 숫자 0과 영문자 O/o 혼동 교정 (`OCR_ZERO_DOSAGE_PATTERN`): 예) `5OOmg` → `500mg`
  2. **단위 및 특수문자 정규화 (`UNIT_ALIASES`):**
     - 한글/특수기호 단위를 영문 표준 코드로 통일합니다. (예: `밀리그램`, `m.g` → `mg` / `마이크로그램`, `μg` → `mcg` / `단위` → `iu`)
     - 정규표현식(`NOISE_PATTERN`)으로 불필요한 구두점과 노이즈 문자를 제거합니다.
  3. **비약품 라인 필터링 (Noise Filtering):**
     - **행정/의료 정보 제외:** `['환자', '병원', '처방', '조제', '의사', '약사', '주의사항']` 등의 키워드(`ADMIN_KEYWORDS`)가 있으면 스킵합니다.
     - **복용법 라인 제외:** 용량/제형이 없으면서 `['식전', '식후', '아침', '점심', '저녁', '취침', '하루', '포']` 등의 키워드(`DOSING_KEYWORDS`)가 있으면 스킵합니다.
  4. **제형(Form) 및 용량(Dosage) 분리 파싱:**
     - `정`, `캡슐`, `시럽`, `주사액`, `크림` 등 20여 개의 제형(`FORM_TYPES`)과 추출된 용량을 텍스트에서 분리하여 객체 속성으로 담습니다.
- **Output:** 정제 및 중복이 제거된 `[NormalizedDrug(name="타이레놀", dosage="500mg", form_type="정", source="normalizer", ...)]` 형태의 객체 배열을 반환하며, 각 후보에는 해당 라인의 단어 신뢰도를 평균 낸 `ocr_confidence` 점수가 부여됩니다.

> 🔄 **흐름 전환 (Data Flow):** Stage 2에서 추출된 `NormalizedDrug` 배열 데이터는 그대로 다음 단계로 전달되거나, 텍스트가 복잡하여 LLM 옵션이 켜져있을 경우 Stage 2.5의 결과물과 병합됩니다.

---

## Phase 2: 보조 지표 구조화

정규화 로직만으로 한계가 있는 복잡한 텍스트 구조를 인공지능(LLM)을 활용해 파싱하는 선택적(Optional) 보조 단계입니다.

### Stage 2.5: Optional LLM 구조화 추출 (`LLMExtractor`)
- 📝 **관련 파일:** `app/ocr/services/llm_extractor.py`
- **주요 목적:** 노이즈가 많거나 텍스트가 줄바꿈 없이 복잡하게 얽힌 처방전 텍스트에서 약품명과 용량, 제형을 명확히 분리하여 구조화된 JSON 배열 형태로 추출합니다.
- **핵심 로직:**
  - **프롬프트 설정:** 일관성을 위해 `temperature=0.0`으로 고정된 OpenAI 모델을 호출하며, "모르면 빈 배열을 반환하라"는 지시를 포함하여 안전성을 확보합니다.
  - **Hallucination 방지 검증 (`_verify_in_source`):** LLM이 만들어낸 가짜 약품명(Hallucination)을 막기 위해, 추출된 이름이 원본 OCR 텍스트에 포함되어 있는지(한글 2글자 이상 또는 영문 3글자 이상 연속 매칭) 반드시 검증합니다.
  - **결과 병합 (Merge):** 기존 정규화 단계(Stage 2)에서 찾지 못한 신규 후보군만 추가로 병합합니다.
  - **태깅 (Guardrail):** 이 단계에서 추가된 후보는 `source="llm_hint"`로 태깅됩니다. 파이프라인 후반부에서 `llm_hint` 단독 후보는 무조건 자동 확정을 방지하고 사용자 확인으로 넘기게 됩니다.

> 🔄 **흐름 전환 (Data Flow):** Stage 2와 Stage 2.5를 거쳐 최종적으로 하나로 병합된 `NormalizedDrug` 배열을 순회하며, 추출된 각 약품명(name)을 데이터베이스 매칭 엔진(Stage 3)의 검색어로 사용합니다.

---

## Phase 3: 하이브리드 다중 매칭 엔진

정제/추출된 약품명 후보를 인메모리 인덱스와 MySQL 약품 마스터 DB(`medications_master`)와 순차적으로 매칭하여 가장 점수가 높은 후보(MatchCandidate)를 도출합니다.

### Stage 3: MySQL 1차 다중 매칭 (`MySQLMatcher`)
- 📝 **관련 파일:** `app/ocr/services/mysql_matcher.py` 및 `app/ocr/repository/drug_repository.py`
- 데이터베이스 풀스캔을 방지하고 정확도를 높이기 위해, 속도와 정확도가 높은 검색부터 시작하는 **우선순위 기반 6단계 폭포수(Waterfall) 검색**을 수행합니다. (스레드 안전한 인메모리 `LocalDrugIndex` 캐시 매칭 실패 시 아래 로직 순차 실행)
1. **Exact Match:** DB의 약품명과 정확히 일치하는지 검색합니다.
2. **Alias Match:** 대표 별칭(예: 괄호/제형이 제거된 이름, 브랜드명) 테이블(`medication_aliases`)에서 검색합니다.
3. **Error Alias Match:** OCR 전용 오인식 변형 테이블(`medication_error_aliases`)에서 검색합니다.
4. **Prefix Match:** 검색어로 시작하는 약품명 검색 (`LIKE 'text%'`).
5. **Ngram Match:** MySQL의 FULLTEXT INDEX를 이용한 N-Gram 기반 부분 일치 검색으로 유력 후보군을 도출합니다.
6. **Fuzzy Match (문자열 유사도):** 전체 DB가 아닌 Ngram으로 축소된 최대 50건의 후보 풀 내에서만 `rapidfuzz` 라이브러리를 사용해 Levenshtein 거리 기반 유사도를 계산합니다 (가중치: 단순비율 60% + 부분일치율 40%, 최소점수 0.5 이상).
> **Alias 충돌 감지 (Conflict Detection):** 만약 동일한 별칭 검색어가 DB에서 서로 다른 약품 코드(item_seq) 2개 이상에 매핑될 경우 `alias_conflict=True` 속성을 반환하여 추후 자동 확정을 막습니다.

> 🔄 **흐름 전환 (Data Flow):** 모든 `NormalizedDrug` 후보에 대해 MySQL 검색을 마친 후, 수집된 전체 매칭 후보군(`MatchCandidate`) 중에서 가장 높은 매칭 점수(`best_mysql_score`)를 확인합니다.

### Stage 4: VectorDB 2차 매칭 (`VectorMatcher`)
- 📝 **관련 파일:** `app/ocr/services/vector_matcher.py` 및 `app/chatbot/services/embedding_service.py`
- **트리거 조건:** Stage 3의 최고 매칭 점수가 시스템 임계치(Threshold, 기본 0.85) 미만일 때만 자원 소모를 최소화하기 위해 제한적으로 작동합니다.
- **주요 목적:** 단순 문자열 매칭(오타 교정, 유사도 등)이 모두 실패했을 때, ChromaDB에 구축된 벡터 유사도를 통해 의미상 가장 가까운 약품을 탐색하는 최후의 Fallback 메커니즘입니다.
- **핵심 로직:**
  1. **임베딩 변환:** `EmbeddingService`를 호출하여 정규화된 텍스트를 실수형 임베딩(Embedding) 벡터로 생성합니다.
  2. **유사도 검색 (Top-K):** ChromaDB를 쿼리하여 코사인 거리(Distance)가 가장 가까운 상위 5개(`top_k=5`)의 약품 코드(`item_seq`)와 메타데이터를 추출합니다.
  3. **점수 변환 로직:** 계산된 코사인 거리를 직관적인 유사도 점수(`score = 1.0 - distance`)로 변환합니다.
  4. **MySQL 교차 조회:** ChromaDB의 결과물은 메타데이터에 불과하므로, 추출된 `item_seq`를 바탕으로 MySQL 마스터 DB에서 실제 약품의 전체 상세 정보(`DrugInfo`)를 다시 한 번 안전하게 조회합니다.
  5. **가드레일 태깅:** 이 단계에서 찾은 후보들은 `evidence={"source": "vector_db"}` 형태로 태깅됩니다. 파이프라인 후반부에서 벡터 검색 단독 결과는 안전상의 이유로 절대 자동 확정(MATCHED)되지 않고 **사용자 수동 확인(NEED_USER_CONFIRMATION)** 으로 강제 전환됩니다.

> 🔄 **흐름 전환 (Data Flow):** Stage 3(및 작동했을 경우 Stage 4)에서 수집된 수많은 `MatchCandidate` 객체들을 약품 코드(`item_seq`) 기준으로 중복 제거합니다. 동일 약품 중 가장 점수가 높은 하나만 남긴 뒤, 연산 최적화를 위해 **상위 10개(Top 10)** 로 추려내어 다음 정밀 검증 단계로 넘깁니다.

---

## Phase 4: 정밀 검증 및 신뢰도 평가

DB에서 찾아온 상위 후보군(Candidate)들이 정말로 입력된 OCR 원본 텍스트와 일치하는지 내부 하드 룰(Rule)에 기반해 정밀하게 점검하고, 그 결과를 바탕으로 최종 신뢰도 점수(Pseudo-Confidence)를 부여합니다.

### Stage 5: 규칙 검증 (`RuleValidator`)
- 📝 **관련 파일:** `app/ocr/services/rule_validator.py`
- **주요 목적:** 문자열이 비슷하더라도, 환자에게 치명적일 수 있는 '다른 용량'이나 '다른 제형'의 약품이 오매칭되는 것을 원천 차단하기 위해 검증 근거(Evidence)를 수집합니다.
- **핵심 로직:**
  - **용량/단위 파싱 및 검증 (`_compare_strength`):** OCR 원본 텍스트와 DB의 약품명(`item_name`) 양쪽 모두에서 정규표현식(`STRENGTH_PATTERN`)을 사용해 숫자(Decimal)와 단위(예: mg, ml, mcg, iu 등)를 추출합니다. 추출된 숫자-단위 쌍이 정확히 일치할 때만 `True`를, 다를 경우 `False`를 반환합니다. 파싱이 불가능하면 `None`(판단 불가)을 반환합니다.
  - **제조사 검증:** OCR 텍스트 내에 DB 후보의 전체 제조사명(`entp_name`) 문자열이 존재하는지 검사합니다.
  - **제형 검증:** OCR에서 추출된 제형(예: '정', '캡슐')이 DB 약품명에 포함되어 있는지 검사합니다.
  - **키워드 보너스:** 약의 효능(`efcy_qesitm`) 문자열과 OCR 원문 양쪽 모두에 `["해열", "진통", "감기", "고혈압", "혈당", "위장", "소화", "불면", "알레르기", "천식"]` 중 일치하는 키워드가 있다면, 키워드당 0.02점씩 최대 0.05점의 추가 보너스 점수를 산정합니다.
  - **결과:** 검증 결과는 참/거짓 판단만 수행하여 각 후보의 `evidence` 딕셔너리에 태깅되며, 검증 메시지를 남기지만 이 단계에서 직접 점수를 깎거나 더하지는 않습니다.

> 🔄 **흐름 전환 (Data Flow):** 규칙 검증이 끝나면 Evidence가 추가된 후보군을 다시 한 번 중복 제거하고 상위 10개를 추려내어 점수 재계산 모듈로 전달합니다.

### Stage 6: Pseudo-Confidence 점수 재계산 (`PseudoConfidenceScorer`)
- 📝 **관련 파일:** `app/ocr/services/pseudo_confidence_scorer.py`
- **주요 목적:** RuleValidator가 수집한 Evidence를 바탕으로 단순 텍스트 유사도가 아닌 '의료적 중요도'에 기반한 7가지 지표(Breakdown)로 최종 매칭 신뢰도(0.0~1.0)를 재계산합니다.
- **2-Pass 알고리즘:**
  - **Pass 1 (기본 점수 계산):** 약품명 유사도(30%), 용량 일치(25%), 단위 일치(15%), 제형 일치(10%), 제조사 일치(10%) 가중치를 부여합니다. 단, 검증 결과가 `None`(판단 불가)인 항목은 가중치의 **절반**만 부여하여 불이익을 최소화합니다.
  - **Pass 2 (격차 보너스 부여):** Pass 1의 점수로 후보들을 1차 정렬한 뒤, 1순위와 2순위 간의 점수 격차(Gap)를 계산합니다. 격차가 0.30 이상일 경우 최대 10%(0.10)의 보너스 점수(`top_gap`)를 **1순위 후보에게만** 추가로 부여하여 '확실한 후보'임을 시스템에 알립니다.
- **강력한 패널티 (Penalty Guard):**
  - **`strength_mismatch` (-0.40):** 검증 단계에서 용량 불일치(`False`)가 확정된 경우 점수를 40%나 깎아내려 오매칭을 강제 방지합니다.
  - **`low_ocr_confidence` (-0.10):** 원본 OCR 텍스트 라인의 인식 신뢰도가 0.75 미만일 경우 불확실성 패널티를 부여합니다.

> 🔄 **흐름 전환 (Data Flow):** 점수 재계산이 완료된 후보군들을 점수 순으로 내림차순 정렬하고, 최종적으로 **상위 5개(Top 5)** 로 압축하여 최종 판정 모듈로 전달합니다.

---

## Phase 5: 최종 판정 및 시스템 연동 (후처리)

최종 계산된 점수를 바탕으로 파이프라인의 최종 상태(성공, 확인 필요, 모호함 등)를 판정하고, 어르신 친화적인 설명문을 생성한 뒤 전체 처리 결과를 시스템에 비동기 로깅합니다.

### Stage 7: 최종 상태 판정 및 가드레일 (`_decide`)
- 📝 **관련 파일:** `app/ocr/services/medication_pipeline.py` (내부 `_decide()` 메서드)
- **주요 목적:** 모든 계산이 끝난 후보군을 바탕으로 최종 상태를 판정하며, 사소한 위험 요소라도 발견되면 자동 확정을 막는 강력한 수문장(Guardrail) 역할을 합니다.
- **핵심 로직 (폭포수 검증):**
  1. **Alias 충돌:** 동일한 별칭이 여러 약에 매핑되는 경우(`alias_conflict == True`) 곧바로 `AMBIGUOUS` 반환.
  2. **단일 소스 의존 방지:** 최상위 후보의 출처(`source`)가 `vector_db`이거나 `llm_hint`일 경우, 교차 검증이 부족하다고 판단해 무조건 `NEED_USER_CONFIRMATION` (사용자 확인 필요) 강제 전환.
  3. **저신뢰도 OCR 필터링:** 정규화 단계에서 측정한 `ocr_confidence`가 0.75 미만인 단어가 포함되어 있다면 오인식일 가능성이 크므로 `LOW_CONFIDENCE` 반환.
  4. **용량 불일치 감지:** `strength_match == False`일 경우 치명적인 의료 사고를 막기 위해 `NEED_USER_CONFIRMATION` 반환.
  5. **모호성 검사:** 1순위와 2순위의 점수 차이가 `0.08` 미만이면 어느 약인지 확신할 수 없으므로 `AMBIGUOUS` 반환.
  6. **최종 임계치 검사:** 1순위 점수가 시스템 임계치(기본 `0.85`)보다 낮으면 `LOW_CONFIDENCE` 반환.
  7. **MATCHED:** 위 모든 방어벽을 통과해야만 최종적으로 성공(`MATCHED`) 상태를 획득할 수 있습니다.

> 🔄 **흐름 전환 (Data Flow):** 판정이 완료되면, 상태가 `MATCHED`인 경우 상위 5개 후보 전체를, 그 외의 경우엔 상위 3개 후보만을 추려서 어르신 맞춤 설명 생성 프롬프트(Stage 8)의 Context로 주입합니다.

### Stage 8: LLM 기반 어르신 맞춤 설명 생성 (`LLMDescriptor`)
- 📝 **관련 파일:** `app/ocr/services/llm_descriptor.py`
- **주요 목적:** 확정된(또는 상위 3개) 약품의 DB 성분/효능/용법 정보를 바탕으로, 어르신이 이해하기 쉬운 복약 안내문 JSON을 생성합니다.
- **핵심 로직:**
  - **프롬프트 통제:** LLM 시스템 프롬프트를 통해 "존댓말 사용(해요체)", "의학 용어를 쉬운 말로 풀어서 설명", "주어진 DB 정보 외의 새로운 정보(효능/부작용) 창작 절대 금지"를 엄격히 지시합니다.
  - **상태 기반 어조 조절:** 만약 판정 상태가 `MATCHED`가 아닐 경우, 환자에게 "확정된 약"이라고 단정지어 말하지 않고 "사용자 확인이 필요하다"는 식의 유보적인 안내를 하도록 프롬프트를 동적으로 제어합니다.
  - **시간 매핑:** 아침/조식/점심/중식/취침전 등의 다양한 한국어 복용 시간 표현을 시스템에서 처리가 용이한 명확한 영문 키워드(`morning`, `noon`, `evening`, `night`)로 파싱하여 반환합니다.
  - **안전한 Fallback:** 만약 네트워크 지연이나 형식이 깨져 파싱(`JSONDecodeError`)에 실패하거나 LLM 호출 자체에 실패하더라도, 파이프라인 전체가 실패하지 않도록 DB에 있는 기존 효능 정보(`efcy_qesitm`)와 복용법(`use_method_qesitm`)을 기본 포맷으로 조립해 반환하는 튼튼한 Fallback 로직이 내장되어 있습니다.

> 🔄 **흐름 전환 (Data Flow):** 파이프라인 연산이 모두 종료되면, 소요 시간(`total_ms`)과 식별된 약품, 설명문 등을 조립하여 최종 객체(`PipelineResult`)를 반환합니다. 이와 동시에 전체 연산 내역을 바탕그라운드 스레드에서 DB에 저장합니다.

### 비동기 로깅 (`_save_result`)
- 📝 **관련 파일:** `app/ocr/services/medication_pipeline.py` (내부 `_save_result()` 메서드) 및 `app/ocr/repository/ocr_result_repository.py`
- **주요 목적:** 파이프라인의 모든 궤적과 중간 산출물을 빠짐없이 저장하여, 추후 어드민/개발자의 디버깅과 AI 모델 성능 평가(Observability)를 완벽하게 지원합니다.
- **Non-blocking 저장:** 생성된 전체 후보군 내역(`candidates`), 정규화된 약품명 리스트(`normalized_names`), 판단 근거(`decision_reasons` 배열), 각 8단계별 소요 시간(`Pipeline Stages MS`), 식별된 약품 코드(`best_drug_item_seq`) 등 모든 메타데이터를 하나의 거대한 JSON 레코드로 조립해 `medication_ocr_results` 테이블에 상세 기록합니다.
- **격리된 처리:** 이 과정은 API 메인 스레드를 차단하지 않는 비동기/Fire-and-Forget 방식으로 동작합니다. 즉, DB 저장 중 일시적인 시스템 오류가 발생하더라도, 사용자(프론트엔드)에게는 지연 없이 정상적으로 OCR 식별 결과를 반환(API Response)하도록 안전하게 격리되어 있습니다.
