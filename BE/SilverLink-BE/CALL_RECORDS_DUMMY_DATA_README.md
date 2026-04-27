# 통화 기록 더미 데이터 생성 가이드

## 개요
보호자가 어르신의 통화 기록을 확인할 수 있도록 더미 데이터를 생성합니다.

## 생성되는 데이터

### 1. 통화 기록 (call_records)
- **기간**: 최근 30일
- **총 통화 수**: 24건
- **완료된 통화**: 21건
- **실패한 통화**: 3건
- **통화 시간**: 5분~9분 (300초~540초)
- **어르신 ID**: 3 (이어르신)

### 2. 통화 요약 (call_summaries)
- 완료된 모든 통화에 대한 요약 생성
- 통화 시간에 따라 다른 요약 내용 제공

### 3. 감정 분석 (call_emotions)
- 완료된 통화에 대한 감정 분석 결과
- 감정 레벨: VERY_POSITIVE, POSITIVE, NEUTRAL, NEGATIVE, VERY_NEGATIVE

### 4. 일일 상태 (call_daily_status)
- 최근 7일간의 통화에 대한 일일 상태
- 건강, 식사, 수면 상태 포함

### 5. CallBot 질문 (llm_models)
- CallBot이 어르신에게 한 질문 기록
- 예: "안녕하세요, 오늘 기분은 어떠세요?"

### 6. 어르신 응답 (elderly_responses)
- 어르신의 답변 데이터
- 다양한 응답 패턴 포함

### 7. 상담사 통화 리뷰 (counselor_call_reviews) ⭐ 신규 추가
- 완료된 통화의 약 80%에 대한 상담사 리뷰
- 감정 상태에 따른 맞춤형 코멘트
- 부정적 감정일 경우 긴급 플래그 설정
- 상담사 ID: 1 (김상담)
- **이 데이터가 있어야 보호자 통화 기록 페이지에 표시됩니다**

## 실행 방법

### ⚠️ 중요: 사전 준비
먼저 `dummy_faq.sql`을 실행하여 사용자 데이터(보호자, 어르신)를 생성해야 합니다:
```cmd
cd SilverLink-BE
mysql -u root -p silverlink < dummy_faq.sql
```

### Windows (CMD) - 권장
```cmd
cd SilverLink-BE
mysql -u root -p --default-character-set=utf8mb4 silverlink < dummy_call_records.sql
```

### Windows (PowerShell)
```powershell
cd SilverLink-BE
Get-Content dummy_call_records.sql | mysql -u root -p --default-character-set=utf8mb4 silverlink
```

### 비밀번호 입력 없이 실행 (비밀번호가 없는 경우)
```cmd
mysql -u root --default-character-set=utf8mb4 silverlink < dummy_call_records.sql
```

## 실행 결과 확인

성공적으로 실행되면 다음과 같은 메시지가 출력됩니다:
```
통화 기록 더미 데이터 생성 완료!
total_call_records: 24
completed_calls: 21
failed_calls: 3
상담사 리뷰 생성 완료!
total_reviews: 약 17건 (완료된 통화의 80%)
urgent_reviews: 부정적 감정 통화 수
```

> **참고**: Windows CMD에서 한글이 깨져 보일 수 있지만, 실제 데이터는 정상적으로 생성됩니다.

## 데이터 확인 쿼리

### 1. 통화 기록 확인
```sql
SELECT 
    call_id,
    call_at,
    call_time_sec,
    state,
    DATE_FORMAT(call_at, '%Y-%m-%d %H:%i') as formatted_time
FROM call_records 
WHERE elderly_user_id = 3 
ORDER BY call_at DESC;
```

### 2. 통화 요약 확인
```sql
SELECT 
    cr.call_id,
    cr.call_at,
    cs.content
FROM call_records cr
LEFT JOIN call_summaries cs ON cr.call_id = cs.call_id
WHERE cr.elderly_user_id = 3 
  AND cr.state = 'COMPLETED'
ORDER BY cr.call_at DESC
LIMIT 5;
```

### 3. 감정 분석 확인
```sql
SELECT 
    cr.call_id,
    cr.call_at,
    ce.emotion_level
FROM call_records cr
LEFT JOIN call_emotions ce ON cr.call_id = ce.call_id
WHERE cr.elderly_user_id = 3 
  AND cr.state = 'COMPLETED'
ORDER BY cr.call_at DESC
LIMIT 5;
```

### 4. 상담사 리뷰 확인 (보호자 페이지에 표시되는 데이터)
```sql
SELECT 
    cr.call_id,
    cr.call_at,
    ccr.comment as counselor_comment,
    ccr.is_urgent,
    ccr.reviewed_at
FROM call_records cr
LEFT JOIN counselor_call_reviews ccr ON cr.call_id = ccr.call_id
WHERE cr.elderly_user_id = 3 
  AND cr.state = 'COMPLETED'
ORDER BY cr.call_at DESC
LIMIT 5;
```
LIMIT 5;
```

### 4. CallBot 대화 확인
```sql
SELECT 
    lm.prompt as '질문',
    er.content as '응답',
    cr.call_at as '통화시간'
FROM llm_models lm
JOIN elderly_responses er ON lm.model_id = er.model_id
JOIN call_records cr ON lm.call_id = cr.call_id
WHERE cr.elderly_user_id = 3
ORDER BY cr.call_at DESC
LIMIT 10;
```

## 주의사항

1. **자동 삭제**: 스크립트는 어르신 ID 3의 기존 통화 기록을 자동으로 삭제하고 새로 생성합니다.
2. **중복 실행**: 여러 번 실행해도 안전합니다. 기존 데이터를 삭제하고 새로 생성합니다.
3. **다른 데이터 보존**: 다른 어르신의 통화 기록은 영향받지 않습니다.

## 프론트엔드에서 확인

더미 데이터 생성 후:
1. 보호자 계정으로 로그인
   - 로그인 ID: `test_guardian`
   - 비밀번호: (설정된 비밀번호)
2. 보호자 대시보드 접속
3. "통화 기록" 메뉴 선택
4. 최근 30일간의 통화 기록 확인

## 문제 해결

### 1. MySQL 연결 오류
```
ERROR 2002 (HY000): Can't connect to MySQL server
```
**해결방법**: MySQL 서버가 실행 중인지 확인하세요.
```cmd
# MySQL 서비스 상태 확인
sc query MySQL80
```

### 2. 권한 오류
```
ERROR 1045 (28000): Access denied for user
```
**해결방법**: MySQL 사용자 이름과 비밀번호를 확인하세요.
```cmd
# 올바른 사용자명과 비밀번호로 실행
mysql -u root -p silverlink < dummy_call_records.sql
```

### 3. 외래 키 제약 오류
```
ERROR 1452 (23000): Cannot add or update a child row
```
**해결방법**: `dummy_faq.sql`을 먼저 실행하여 사용자 데이터를 생성하세요.
```cmd
mysql -u root -p silverlink < dummy_faq.sql
mysql -u root -p silverlink < dummy_call_records.sql
```

### 4. 문자 인코딩 오류
```
ERROR 3854 (HY000): Cannot convert string from euckr to utf8mb4
ERROR 1267 (HY000): Illegal mix of collations
```
**해결방법**: `--default-character-set=utf8mb4` 옵션을 추가하세요.
```cmd
mysql -u root -p --default-character-set=utf8mb4 silverlink < dummy_call_records.sql
```

### 5. 중복 키 오류
```
ERROR 1062 (23000): Duplicate entry for key
```
**해결방법**: 스크립트가 자동으로 기존 데이터를 삭제하므로, 이 오류는 발생하지 않아야 합니다. 
만약 발생한다면 수동으로 데이터를 삭제하세요:
```sql
SET FOREIGN_KEY_CHECKS = 0;
DELETE FROM counselor_call_reviews WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM call_daily_status WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM call_emotions WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM call_summaries WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM elderly_responses WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM llm_models WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM call_records WHERE elderly_user_id = 3;
SET FOREIGN_KEY_CHECKS = 1;
```

### 6. 한글 출력 깨짐
Windows CMD에서 실행 결과 메시지가 깨져 보이는 것은 정상입니다. 
실제 데이터베이스에 저장된 데이터는 정상적으로 UTF-8로 저장됩니다.

**확인 방법**:
```cmd
# MySQL 클라이언트로 직접 확인
mysql -u root -p silverlink
mysql> SELECT COUNT(*) FROM call_records WHERE elderly_user_id = 3;
```

## 데이터 완전 초기화 (필요시)

모든 통화 기록을 삭제하고 싶다면:

```sql
SET FOREIGN_KEY_CHECKS = 0;
TRUNCATE TABLE counselor_call_reviews;
TRUNCATE TABLE call_daily_status;
TRUNCATE TABLE call_emotions;
TRUNCATE TABLE call_summaries;
TRUNCATE TABLE elderly_responses;
TRUNCATE TABLE llm_models;
TRUNCATE TABLE call_records;
SET FOREIGN_KEY_CHECKS = 1;
```

그 후 `dummy_call_records.sql`을 다시 실행하세요.

## 추가 정보

- **테스트 계정**: 보호자 ID 2 (김보호) - 어르신 ID 3 (이어르신)
- **통화 패턴**: 하루 2회 통화 (오전, 오후)
- **실패율**: 약 12.5% (3건/24건)
- **데이터 기간**: 오늘부터 30일 전까지
