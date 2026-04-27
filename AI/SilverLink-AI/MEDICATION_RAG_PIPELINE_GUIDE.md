# Medication RAG Pipeline Guide

**작성일**: 2026-04-27  
**프로젝트**: SilverLink AI 복약관리  
**목적**: OCR 기반 약품 인식 및 복약 설명 생성을 위한 RAG 파이프라인 설계 기준 정리

---

## 핵심 판단

복약관리 기능에서는 LLM이 약품을 직접 판단하게 만들면 안 된다.

LLM의 역할은 다음으로 제한한다.

- 검증된 약품 정보를 사용자가 이해하기 쉽게 설명한다.
- 확정된 데이터와 근거를 바탕으로 복약 설명을 생성한다.
- 약품 식별, 처방 변경, 복용 중단 판단, 새로운 의학적 조언 생성은 하지 않는다.

전체 구조는 **MySQL 기반 결정론적 후보 검색 + 규칙 검증**을 중심에 두고, Vector DB는 낮은 점수나 애매한 OCR 결과를 보완하는 fallback으로 사용한다.

---

## 권장 파이프라인

```text
이미지 / 처방전 / 약봉투 / 알약 사진
  -> OCR
  -> 텍스트 정규화
  -> MySQL 후보 검색 1차
       - exact
       - prefix
       - ngram
       - fuzzy
  -> 후보 점수 평가
  -> 점수 낮으면 Vector DB 검색 2차
  -> 규칙 검증
       - 성분
       - 함량
       - 업체
       - 제형
       - 모양
       - 색상
       - 식별문자
  -> 최종 매칭 판정
  -> LLM 설명 생성
```

---

## 단계별 설계

### 1. OCR Layer

OCR 결과는 단순 문자열만 저장하지 말고 confidence와 token 정보를 함께 보존한다.

```json
{
  "text": "아모디핀정 5mg",
  "tokens": [
    {
      "value": "아모디핀정",
      "confidence": 0.91
    },
    {
      "value": "5mg",
      "confidence": 0.88
    }
  ]
}
```

활용 기준:

- OCR confidence가 낮으면 후보 검색 점수를 낮춘다.
- OCR confidence가 낮고 후보가 여러 개면 사용자 확인 상태로 보낸다.
- OCR 원문, 정규화 결과, 후보 검색 결과를 함께 로그로 남긴다.

### 2. Normalization Layer

OCR 결과를 검색에 적합한 형태로 정규화한다.

정규화 대상:

- 한글/영문 대소문자 정규화
- 공백, 괄호, 특수문자 제거 또는 표준화
- 단위 정규화: `mg`, `밀리그램`, `MG`
- 제형 정규화: `정`, `정제`, `캡슐`, `시럽`, `서방정`
- OCR 혼동 문자 보정
- 제품명 alias 매핑
- 성분명 alias 매핑

예시:

```text
타이레놀 이알 서방정
타이레놀 ER
타이레놀8시간이알서방정
acetaminophen extended release
```

위 표현들은 같은 후보군으로 묶일 수 있어야 한다.

### 3. Candidate Retrieval Layer

1차 검색은 MySQL 중심으로 수행한다.

검색 방식:

- exact match: 제품명, 품목기준코드, 보험코드 등
- prefix match: 제품명 앞부분 일치
- ngram match: OCR 누락/분리 대응
- fuzzy match: OCR 오타 대응
- synonym table: 제품명, 성분명, 영문명 alias 대응

점수 산정에 반영할 요소:

- 제품명 일치도
- 성분명 일치도
- 함량 일치 여부
- 업체명 일치 여부
- 제형 일치 여부
- OCR confidence
- 후보 간 점수 차이

### 4. Vector DB Fallback Layer

Vector DB는 1차 후보 점수가 낮거나 후보가 애매할 때만 사용한다.

적합한 사용처:

- 제품명 alias 검색
- 성분명 alias 검색
- OCR noisy query embedding
- 제품 설명 기반 유사 검색
- 한글/영문/약어 표현 차이 보완

부적합한 사용처:

- 품목코드 식별
- 함량 확정
- 업체 확정
- 보험코드 기반 검색
- 최종 약품 결정

Vector DB 결과는 최종 답이 아니라 후보 확장용으로만 사용한다.

### 5. Validation Layer

후보를 규칙 기반으로 검증한다.

검증 항목:

- 성분 일치 여부
- 함량 일치 여부
- 업체 일치 여부
- 제형 일치 여부
- 모양 일치 여부
- 색상 일치 여부
- 식별문자 일치 여부
- 분할선, 크기 등 알약 이미지 기반 속성 일치 여부

처방전/약봉투 OCR과 알약 이미지 인식은 scoring 기준을 분리한다.

```text
처방전/약봉투 OCR:
  약품명, 성분명, 함량, 업체, 투약량, 복용법 중심

알약 이미지:
  식별문자, 모양, 색상, 제형, 분할선, 크기 중심
```

### 6. Decision Layer

최종 결과는 하나의 약품을 무조건 반환하지 않는다.

권장 상태값:

```text
MATCHED
AMBIGUOUS
LOW_CONFIDENCE
NOT_FOUND
NEED_USER_CONFIRMATION
```

상태 기준 예시:

- `MATCHED`: 점수가 충분히 높고 검증 항목이 충돌하지 않음
- `AMBIGUOUS`: 상위 후보가 2개 이상이고 점수 차이가 작음
- `LOW_CONFIDENCE`: OCR confidence 또는 검색 점수가 낮음
- `NOT_FOUND`: 유효 후보 없음
- `NEED_USER_CONFIRMATION`: 함량, 업체, 제형 등 중요한 속성이 불확실함

특히 동일 제품군에서 함량만 다른 경우는 사용자 확인이 필요하다.

### 7. LLM Generation Layer

LLM에는 확정된 구조화 데이터만 전달한다.

LLM 입력에 포함할 수 있는 정보:

- 확정된 약품명
- 성분
- 함량
- 업체
- 제형
- 복용법
- 주의사항
- 병용금기/주의 데이터
- 사용자 복약 스케줄
- 검색 및 검증 근거

LLM이 하면 안 되는 일:

- 약품 식별 결정
- 처방 변경 권고
- 복용 중단 판단
- 근거 없는 부작용 또는 효능 생성
- 데이터에 없는 의학 정보 추론

LLM 응답에는 사용자 안전을 위한 안내를 포함한다.

```text
복용 중 이상 증상이 있거나 처방 내용과 다르게 느껴진다면 의사 또는 약사에게 확인하세요.
```

---

## 구현 시 체크리스트

- OCR 원문과 정규화 결과를 모두 저장한다.
- OCR token별 confidence를 보존한다.
- MySQL 1차 검색 결과와 점수를 로그로 남긴다.
- Vector DB는 fallback 또는 후보 확장 용도로만 사용한다.
- 최종 매칭 전에 성분/함량/업체/제형/식별정보를 규칙 검증한다.
- 후보가 애매하면 무조건 사용자 확인 상태로 전환한다.
- LLM은 최종 매칭된 데이터 설명에만 사용한다.
- LLM 프롬프트에는 "데이터에 없는 정보는 생성하지 말라"는 제약을 둔다.
- 설명 생성 결과에는 근거 데이터 또는 출처 식별자를 함께 연결한다.
- 테스트셋에는 OCR 오타, 함량 차이, 유사 제품명, 동일 성분 다른 업체 케이스를 포함한다.

---

## 추천 데이터 구조

### OCR Result

```json
{
  "raw_text": "아모디핀정 5mg",
  "normalized_text": "아모디핀정 5mg",
  "tokens": [
    {
      "value": "아모디핀정",
      "normalized_value": "아모디핀정",
      "confidence": 0.91
    },
    {
      "value": "5mg",
      "normalized_value": "5mg",
      "confidence": 0.88
    }
  ]
}
```

### Candidate

```json
{
  "drug_id": "example-drug-id",
  "product_name": "아모디핀정 5mg",
  "ingredient": "암로디핀",
  "strength": "5mg",
  "manufacturer": "예시제약",
  "dosage_form": "정제",
  "source": "mysql_exact",
  "score": 0.94,
  "evidence": {
    "name_match": 0.96,
    "strength_match": true,
    "manufacturer_match": null,
    "ocr_confidence": 0.89
  }
}
```

### Decision

```json
{
  "status": "MATCHED",
  "selected_drug_id": "example-drug-id",
  "confidence": 0.94,
  "candidates": [],
  "requires_user_confirmation": false,
  "reasons": [
    "제품명 일치",
    "함량 일치",
    "OCR confidence 양호"
  ]
}
```

---

## 테스트 케이스 방향

반드시 포함할 케이스:

- OCR이 정확한 약품명
- OCR이 일부 글자를 잘못 읽은 약품명
- 공백/괄호/특수문자가 섞인 약품명
- 동일 제품명에 함량만 다른 후보
- 동일 성분이지만 업체가 다른 후보
- 제품명은 비슷하지만 성분이 다른 후보
- Vector DB fallback이 필요한 alias 표현
- 알약 식별문자는 같지만 색상 또는 모양이 다른 후보
- 후보 점수가 낮아 사용자 확인이 필요한 케이스
- 매칭 실패 케이스

---

## 최종 원칙

```text
LLM은 약을 맞히는 역할이 아니라,
검증된 약품 정보를 사용자가 이해하기 쉽게 설명하는 역할이다.
```
