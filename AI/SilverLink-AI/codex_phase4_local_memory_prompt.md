# Codex 작업 지시서: OCR 약품 인식 파이프라인 Phase 4 개선

## 0. 작업 배경

현재 프로젝트의 OCR 약품 인식 파이프라인을 이어서 구현한다.

중요한 전제:

- 현재 Phase 1~3은 이미 구현된 상태다.
- 기존 계획서에는 Phase 4가 `Redis fast lookup`으로 되어 있지만, 이번 구현에서는 Redis를 우선 도입하지 않는다.
- 현재 데이터 규모는 식약처 의약품 제품 허가정보 API 기준 약 43,293건이다.
- 이 규모에서는 Redis보다 서버 내부 로컬 메모리 인덱스가 우선 적합하다.
- 단, 현재 실제 약품 매칭 로직은 Python AI 서버의 `app/ocr/services` 쪽에 있으므로, 이번 작업은 우선 Python AI 서버 내부 로컬 메모리 인덱스로 구현한다.
- Spring Boot 쪽으로 약품 매칭 로직을 옮기는 대규모 리팩터링은 하지 않는다.

---

## 1. 현재 구현된 것으로 간주할 내용

아래 기능들은 이미 구현된 것으로 보고, 기존 동작을 깨지 않도록 한다.

- `TextNormalizer` 개선
- alias / error alias 검색 계층
- alias 충돌 검증
- `RuleValidator`와 `PseudoConfidenceScorer` 역할 분리
- Entity-weighted pseudo-confidence
- 용량 / 단위 충돌 시 자동 확정 방지
- FE에서 `decision_status`, `requires_user_confirmation`, `evidence` 표시
- `MedicationPipeline`에서 MySQL 후보 검색, Rule Validation, Decision, LLM 설명 생성 흐름 일부 구현

---

## 2. 최종 목표

Lucia OCR API가 반환한 텍스트를 기반으로 약품 후보를 찾을 때, MySQL에서 매 요청마다 fuzzy / ngram / full scan성 검색을 수행하지 않도록 개선한다.

서버 시작 시 또는 최초 요청 시 MySQL의 약품 사전을 로컬 메모리에 로딩하고, 이후 요청에서는 HashMap / Trie / N-gram inverted index 기반으로 빠르게 후보를 찾는다.

최종 목표 흐름:

```text
Lucia OCR text
-> raw text 보존
-> TextNormalizer
-> entity extraction
-> LocalMemory DrugDictionaryIndex 후보 검색
   - exact
   - alias
   - error_alias
   - prefix
   - ngram
   - limited fuzzy
-> Candidate Merge
-> PseudoConfidenceScorer
-> RuleValidator
-> Decision
   - MATCHED
   - AMBIGUOUS
   - LOW_CONFIDENCE
   - NEED_USER_CONFIRMATION
   - NOT_FOUND
-> 필요한 경우 MySQL 상세 조회
-> 필요한 경우 VectorDB fallback
-> MATCHED일 때만 LLM Explanation
-> FE 사용자 확인 또는 복약 등록
```

---

## 3. 핵심 설계 원칙

1. MySQL은 source of truth로 유지한다.
2. 로컬 메모리 인덱스는 후보 검색 가속용으로만 사용한다.
3. Redis는 이번 단계에서 구현하지 않는다.
4. Redis는 향후 선택적 캐시 / 동기화 보조 계층으로만 남긴다.
5. LLM은 약품을 최종 식별하면 안 된다.
6. RAG / VectorDB는 최종 판정자가 아니라 fallback 또는 설명 근거 보조 역할만 한다.
7. 최종 약품 판정은 후보 검색 + `PseudoConfidenceScorer` + `RuleValidator` + Decision 로직으로 결정한다.
8. 함량 / 단위 / 성분 충돌은 이름 유사도보다 훨씬 강하게 처리해야 한다.
9. fuzzy는 전체 DB 대상이 아니라, prefix / ngram으로 줄어든 후보군에만 적용해야 한다.
10. raw OCR text는 정규화 과정에서 덮어쓰거나 삭제하면 안 된다.

---

## 4. 구현 전 먼저 확인할 파일

코드를 수정하기 전에 현재 구조를 먼저 확인한다.

확인할 파일:

```text
app/ocr/services/medication_pipeline.py
app/ocr/services/mysql_matcher.py
app/ocr/services/text_normalizer.py
app/ocr/services/rule_validator.py
app/ocr/services/pseudo_confidence_scorer.py
app/ocr/services/drug_repository.py
tests/unit_tests/test_mysql_matcher.py
tests/unit_tests/test_medication_pipeline.py
tests/unit_tests/test_pseudo_confidence.py
```

`drug_repository.py`가 없거나 이름이 다르면 기존 repository 역할을 하는 파일을 찾아서 사용한다.

---

## 5. 구현 작업 1: 로컬 메모리 약품 사전 인덱스 추가

새 모듈을 추가한다.

예상 파일명:

```text
app/ocr/services/drug_dictionary_index.py
```

기존 구조에 더 적합한 이름이 있으면 변경해도 된다.

### 역할

MySQL의 `medications_master`, `medication_aliases`, `medication_error_aliases` 데이터를 서버 시작 시 또는 첫 요청 시 bulk loading한다.

이후 아래 인덱스를 메모리에 구성한다.

### 필요 인덱스

```text
exact_map:
  normalized_item_name -> List[DrugSummary]

alias_map:
  normalized_alias -> List[DrugSummary]

error_alias_map:
  normalized_error_alias -> List[DrugSummary]

prefix_index:
  prefix search용 자료구조

ngram_index:
  ngram token -> Set[item_seq 또는 drug_id]

drug_summary_map:
  item_seq 또는 drug_id -> DrugSummary
```

### DrugSummary 최소 필드

`DrugSummary`에는 최소한 다음 필드가 들어가야 한다.

```text
item_seq 또는 drug_id
item_name
normalized_item_name
ingredient 또는 main_ingredient
strength
unit
dosage_form
manufacturer
source fields
```

### 구현 주의사항

- exact / alias / error_alias는 Map lookup으로 처리한다.
- prefix는 단순 전체 순회보다 효율적인 구조를 사용한다.
- 구현 복잡도가 크면 우선 sorted list 또는 prefix map으로 시작해도 된다.
- ngram은 한국어 기준 2-gram을 기본으로 사용한다.
- 문자열 정규화는 기존 `TextNormalizer`의 규칙과 충돌하지 않게 공통 함수를 사용하거나 동일한 규칙을 재사용한다.
- raw OCR text는 절대 삭제하지 않는다.

---

## 6. 구현 작업 2: DrugRepository에 bulk loading 메서드 추가

기존 DB 접근 계층을 확인하고, 로컬 인덱스 초기화에 필요한 bulk fetch 메서드를 추가한다.

필요 메서드 예시:

```python
fetch_all_medications_for_index()
fetch_all_aliases_for_index()
fetch_all_error_aliases_for_index()
```

### 주의사항

- 요청마다 전체 fetch가 발생하면 안 된다.
- 서버 시작 시 1회 또는 lazy initialization 1회만 수행해야 한다.
- 테스트에서는 mock repository로 대체 가능해야 한다.
- API key가 필요한 공공데이터 실시간 호출은 요청 경로에 넣지 않는다.

---

## 7. 구현 작업 3: MySQLMatcher 검색 구조 개선

현재 `MySQLMatcher`가 다음 순서로 검색하고 있다면, 순서는 유지하되 실제 후보 검색을 가능한 한 LocalMemoryIndex에서 수행하도록 바꾼다.

기존/유지 검색 순서:

```text
exact
alias
error_alias
prefix
ngram
limited fuzzy
```

변경 후 검색 순서:

```text
1. local exact lookup
2. local alias lookup
3. local error_alias lookup
4. local prefix lookup
5. local ngram lookup
6. ngram / prefix로 축소된 후보군에 대해서만 rapidfuzz 또는 Levenshtein fuzzy scoring
7. 부족한 경우에만 MySQL fallback
8. 그래도 부족하면 기존 VectorDB fallback
```

### 중요 제한

- `fetch_all_for_fuzzy(limit=5000)`처럼 매 요청마다 DB에서 대량 후보를 가져오는 방식은 제거하거나 fallback 전용으로 제한한다.
- fuzzy 대상 후보 수는 기본 20~50개 수준으로 제한한다.
- 후보 수 제한값은 상수 또는 설정값으로 분리한다.
- MySQL full scan성 검색이 요청마다 발생하지 않도록 한다.

---

## 8. 구현 작업 4: candidate source / evidence 유지

기존 응답과 테스트가 깨지지 않게 candidate source를 명확히 남긴다.

source 예시:

```text
local_exact
local_alias
local_error_alias
local_prefix
local_ngram
local_fuzzy
mysql_fallback
vector_fallback
```

각 후보의 evidence에는 다음 정보가 들어가면 좋다.

```text
match_method
normalized_query
matched_text
ngram_overlap
fuzzy_score
strength_match
unit_match
alias_conflict 여부
source
```

기존 `PseudoConfidenceScorer`와 `RuleValidator`가 이 evidence를 사용할 수 있어야 한다.

---

## 9. 구현 작업 5: 인덱스 reload 전략 추가

### 최소 구현

- 서버 시작 시 또는 첫 요청 시 인덱스 로딩
- 이미 로딩된 경우 재사용
- 인덱스 로딩 실패 시 기존 MySQL 경로로 fallback

### 가능하면 추가

- `reload_dictionary_index()` 메서드 추가
- atomic swap 방식으로 새 인덱스를 만든 뒤 current index 참조를 교체
- `dictionary_version`은 지금 당장 DB에 없으면 TODO 주석으로 남김

### 주의사항

- 로딩 중 요청이 실패하지 않도록 처리한다.
- 인덱스가 비어 있거나 로딩 실패하면 안전하게 기존 MySQLMatcher 경로로 fallback한다.

---

## 10. 구현 작업 6: 공공데이터 API 동기화는 요청 경로에 넣지 말 것

식약처 의약품 제품 허가정보 API는 사용자 요청 때마다 호출하면 안 된다.

이번 작업에서 공공데이터 API 동기화까지 구현할 수 있다면 별도 script로 분리한다.

예상 파일:

```text
scripts/sync_medications_from_public_api.py
```

단, API key나 실제 인증 정보가 없으면 다음만 구현한다.

```text
환경변수 기반 설정 구조
page 기반 fetch 구조 skeleton
MySQL upsert 구조
실행 방법 문서화
외부 호출이 테스트에 필요하지 않게 구성
```

사용자 요청 처리 파이프라인에서는 반드시 MySQL / 로컬 인덱스만 사용한다.

---

## 11. 테스트 추가

기존 테스트를 깨지 않으면서 다음 테스트를 추가한다.

필수 테스트:

1. local exact lookup으로 후보가 반환되는지
2. local alias lookup으로 후보가 반환되는지
3. local error_alias lookup으로 OCR 오인식 후보가 반환되는지
4. local ngram lookup이 후보군을 반환하는지
5. fuzzy가 전체 DB가 아니라 축소 후보군에만 적용되는지
6. `5mg` vs `50mg` vs `500mg` 충돌에서 `MATCHED`가 아니라 `NEED_USER_CONFIRMATION` 또는 `AMBIGUOUS`가 되는지
7. alias conflict 발생 시 `AMBIGUOUS`가 유지되는지
8. 인덱스 로딩 실패 시 MySQL fallback이 동작하는지
9. 기존 `medication_pipeline` 테스트가 통과하는지
10. 기존 `pseudo_confidence` 테스트가 통과하는지

검증 명령:

```bash
python -m pytest tests/unit_tests/test_pseudo_confidence.py tests/unit_tests/test_mysql_matcher.py tests/unit_tests/test_medication_pipeline.py -v
python -m compileall -f app/ocr scripts
```

새 테스트 파일을 추가했다면 검증 명령에 함께 포함한다.

Java 코드를 수정했다면 다음도 실행한다.

```bash
./gradlew.bat compileJava
```

---

## 12. 기존 API 응답 구조 유지

기존 API 응답 구조를 깨지 않는다.

유지해야 하는 응답 필드:

```text
decision_status
match_confidence
requires_user_confirmation
decision_reasons
evidence
validation_messages
candidates
selected candidate 정보
```

FE가 이미 이 필드들을 사용하고 있으므로 필드명을 임의로 바꾸지 않는다.

---

## 13. 하지 말아야 할 것

다음 작업은 하지 않는다.

```text
Redis를 필수 의존성으로 추가하지 않는다.
Spring Boot로 전체 약품 매칭 로직을 옮기지 않는다.
요청마다 공공데이터 API를 호출하지 않는다.
요청마다 MySQL에서 전체 약품 데이터를 가져오지 않는다.
fuzzy를 전체 43,293건 대상으로 매번 수행하지 않는다.
LLM 결과만으로 MATCHED 처리하지 않는다.
VectorDB/RAG 결과만으로 약품을 확정하지 않는다.
함량/단위 충돌이 있는데 이름이 비슷하다는 이유로 자동 확정하지 않는다.
raw OCR text를 정규화 과정에서 덮어쓰거나 삭제하지 않는다.
```

---

## 14. 완료 후 보고할 내용

작업 완료 후 다음을 요약해서 보고한다.

```text
1. 수정/추가한 파일 목록
2. 새로 추가한 로컬 메모리 인덱스 구조
3. 검색 순서 변경 내용
4. MySQL full scan 방지 방식
5. 테스트 결과
6. 남은 TODO
```

---

## 15. 최종 기대 결과

이번 작업의 최종 결과는 다음과 같아야 한다.

```text
MySQL = source of truth
Python AI Local Memory Index = 초고속 후보 검색 엔진
Redis = 이번 단계에서는 미사용, 향후 선택적 캐시
VectorDB/RAG = fallback 및 설명 근거 보조
LLM = 구조화 보조 또는 최종 설명 생성만 수행
Rule Validation = 최종 안전판
```

핵심 변경점:

```text
기존 Phase 4 Redis fast lookup
↓
수정 Phase 4 Local Memory Drug Dictionary Index
```

데이터 규모가 약 43,293건이므로, 우선 로컬 메모리 인덱스 방식으로 MySQL fuzzy / ngram full scan 부하를 줄이는 것이 목표다.
