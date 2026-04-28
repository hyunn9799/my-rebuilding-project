# Codex 추가 수정 지시서: Phase 4-A LocalDrugIndex MVP 범위 축소

## 0. 적용 우선순위

이 문서는 기존 Phase 4 작업 지시보다 **우선 적용**한다.

이미 Phase 4 작업이 완료된 상태라면, 기존 구현 결과를 검토한 뒤 이 문서의 기준에 맞지 않는 부분만 수정한다.

---

## 1. 이번 Phase 4 작업 범위 축소

이번 Phase 4에서는 완성형 검색 엔진을 만들지 않는다.

목표는 오직 다음이다.

```text
MySQL full scan 위험을 줄이기 위한 LocalDrugIndex MVP 구현
```

즉, Redis, Trie, 멀티워커 공유 메모리, atomic reload, 공공데이터 API sync까지 한 번에 구현하지 않는다.

---

## 2. 반드시 지켜야 할 변경 사항

아래 사항을 반드시 지킨다.

1. Redis는 구현하지 않는다.
2. Trie는 구현하지 않는다.
3. Atomic swap 기반 reload 기능은 구현하지 않는다.
4. `dictionary_version` 기반 멀티 워커 reload도 구현하지 않는다.
5. 공공데이터 API sync script는 이번 작업에서 구현하지 않는다.
6. Spring Boot로 약품 매칭 로직을 옮기지 않는다.
7. FastAPI multi-worker 환경에서는 worker마다 `LocalDrugIndex`를 하나씩 로딩하는 것을 허용한다.
8. 프로세스 간 공유 메모리나 worker 간 동기화는 구현하지 않는다.
9. 대신 인덱스 로딩 시간, 약품 수, alias 수, error alias 수, ngram token 수를 로그로 남긴다.

---

## 3. 이번 작업의 목표

이번 작업의 목표는 다음 하나다.

```text
Phase 4-A: LocalDrugIndex MVP 구현
```

---

## 4. 구현 범위

다음 범위만 구현한다.

```text
- app/ocr/services/drug_dictionary_index.py 추가
- exact_map 구현
- alias_map 구현
- error_alias_map 구현
- 단순 2-gram inverted index 구현
- drug_summary_map 구현
- 서버 시작 시 또는 최초 요청 시 lazy loading
- 인덱스 로딩 실패 시 기존 MySQL 검색 경로로 fallback
- fuzzy는 전체 DB가 아니라 local ngram 후보 top 50개에만 적용
- 기존 API 응답 구조 유지
- 기존 PseudoConfidenceScorer, RuleValidator, MedicationPipeline 동작 유지
```

---

## 5. 절대 하지 말아야 할 것

다음 작업은 하지 않는다.

```text
- 요청마다 fetch_all_for_fuzzy(limit=5000) 같은 대량 DB fetch를 수행하지 않는다.
- 요청마다 전체 medications_master를 조회하지 않는다.
- 전체 43,293건 대상으로 매 요청 fuzzy scan을 하지 않는다.
- ngram index를 요청마다 생성하지 않는다.
- LLM 또는 VectorDB 결과만으로 MATCHED 처리하지 않는다.
- 함량/단위 충돌이 있는데 이름 유사도만으로 MATCHED 처리하지 않는다.
```

---

## 6. 검색 순서

검색 순서는 다음과 같이 구성한다.

```text
1. local exact
2. local alias
3. local error_alias
4. local ngram 후보 검색
5. local ngram 후보 top 50개에만 fuzzy 적용
6. 후보가 부족하거나 인덱스 사용 불가 시 기존 MySQL fallback
7. 그래도 부족하면 기존 VectorDB fallback
```

---

## 7. Candidate source 명명 규칙

후보의 source는 다음 값을 사용한다.

```text
local_exact
local_alias
local_error_alias
local_ngram
local_fuzzy
mysql_fallback
vector_fallback
```

기존 응답 구조를 깨지 않도록 `evidence`, `decision_status`, `match_confidence`, `requires_user_confirmation`, `validation_messages`, `candidates` 필드는 유지한다.

---

## 8. 테스트 요구사항

최소한 다음 테스트를 추가하거나 기존 테스트로 검증한다.

1. local exact lookup 테스트
2. local alias lookup 테스트
3. local error_alias lookup 테스트
4. local ngram 후보 검색 테스트
5. fuzzy가 ngram 후보군에만 적용되는지 테스트
6. 인덱스 로딩 실패 시 MySQL fallback 테스트
7. 기존 `pseudo_confidence` 테스트 통과
8. 기존 `medication_pipeline` 테스트 통과
9. 기존 `mysql_matcher` 테스트 통과

권장 검증 명령:

```bash
python -m pytest tests/unit_tests/test_pseudo_confidence.py tests/unit_tests/test_mysql_matcher.py tests/unit_tests/test_medication_pipeline.py -v
python -m compileall -f app/ocr scripts
```

새 테스트 파일을 추가했다면 위 명령에 함께 포함한다.

Java 코드를 수정했다면 다음도 실행한다.

```bash
./gradlew.bat compileJava
```

---

## 9. 작업 완료 후 보고할 내용

작업 완료 후 다음을 보고한다.

```text
1. 수정/추가한 파일
2. LocalDrugIndex 구조
3. 검색 순서
4. fetch_all_for_fuzzy 또는 전체 DB fuzzy scan 제거 여부
5. 테스트 결과
6. 남은 TODO
```

---

## 10. 중요한 판단 기준

이번 작업에서는 “완성형 검색 엔진”을 만들지 않는다.

핵심은 다음이다.

```text
MySQL full scan 위험을 줄이기 위한 LocalDrugIndex MVP만 구현한다.
```

현재 단계에서 구현하지 않고 TODO로 남길 수 있는 항목:

```text
- Redis cache
- Trie
- atomic reload
- dictionary_version 기반 multi-worker sync
- 공공데이터 API sync script
- Spring Boot로 매칭 로직 이관
```

---

## 11. 최종 기대 결과

최종 기대 구조는 다음과 같다.

```text
Lucia OCR text
-> raw text 보존
-> TextNormalizer
-> LocalDrugIndex 후보 검색
   - exact
   - alias
   - error_alias
   - ngram
   - limited fuzzy
-> MySQL fallback
-> VectorDB fallback
-> Candidate Merge
-> PseudoConfidenceScorer
-> RuleValidator
-> Decision
-> LLM Explanation
```

최종 원칙:

```text
MySQL = source of truth
LocalDrugIndex = 후보 검색 가속
Redis = 이번 단계 미사용
VectorDB/RAG = fallback 및 설명 근거 보조
LLM = 최종 약품 식별 금지
Rule Validation = 최종 안전판
```
