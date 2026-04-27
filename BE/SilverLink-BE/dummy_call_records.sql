-- mysql -h database-1-silverlink.c7uoqu4iemyb.ap-northeast-2.rds.amazonaws.com -P 3306 -u admin -p silverlink < dummy_call_records.sql
USE silverlink;

SET FOREIGN_KEY_CHECKS = 0;

-- 0. 기존 통화 기록 데이터 초기화 (중복 방지)
DELETE FROM counselor_call_reviews WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM call_daily_status WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM call_emotions WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM call_summaries WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM elderly_responses WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM llm_models WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
DELETE FROM call_records WHERE elderly_user_id = 3;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. 통화 기록 더미 데이터 생성
-- 어르신 user_id = 3 기준으로 생성
-- 최근 30일간의 통화 기록 생성

-- 오늘 통화 (완료)
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 2 HOUR), 420, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_001.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 5 HOUR), 360, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_002.mp3');

-- 어제 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 10 HOUR, 480, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_003.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 1 DAY) + INTERVAL 15 HOUR, 390, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_004.mp3');

-- 2일 전 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 11 HOUR, 540, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_005.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 2 DAY) + INTERVAL 16 HOUR, 300, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_006.mp3');

-- 3일 전 통화 (실패 케이스 포함)
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 10 HOUR, 0, 'FAILED', NULL),
(3, DATE_SUB(NOW(), INTERVAL 3 DAY) + INTERVAL 14 HOUR, 450, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_007.mp3');

-- 4일 전 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 9 HOUR, 510, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_008.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 4 DAY) + INTERVAL 17 HOUR, 330, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_009.mp3');

-- 5일 전 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 10 HOUR, 420, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_010.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 5 DAY) + INTERVAL 15 HOUR, 0, 'FAILED', NULL);

-- 6일 전 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 11 HOUR, 480, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_011.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 6 DAY) + INTERVAL 16 HOUR, 360, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_012.mp3');

-- 7일 전 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 7 DAY) + INTERVAL 10 HOUR, 390, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_013.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 7 DAY) + INTERVAL 14 HOUR, 450, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_014.mp3');

-- 10일 전 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 10 DAY) + INTERVAL 11 HOUR, 540, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_015.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 10 DAY) + INTERVAL 15 HOUR, 420, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_016.mp3');

-- 14일 전 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 14 DAY) + INTERVAL 10 HOUR, 480, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_017.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 14 DAY) + INTERVAL 16 HOUR, 0, 'FAILED', NULL);

-- 20일 전 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 20 DAY) + INTERVAL 11 HOUR, 510, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_018.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 20 DAY) + INTERVAL 14 HOUR, 390, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_019.mp3');

-- 30일 전 통화
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
(3, DATE_SUB(NOW(), INTERVAL 30 DAY) + INTERVAL 10 HOUR, 450, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_020.mp3'),
(3, DATE_SUB(NOW(), INTERVAL 30 DAY) + INTERVAL 15 HOUR, 420, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_021.mp3');


-- 2. 통화 요약 데이터 생성 (완료된 통화에 대해서만)
-- call_id는 실제 생성된 ID를 사용해야 하므로, 최근 통화 기록 기준으로 생성

INSERT INTO call_summaries (call_id, content, created_at)
SELECT 
    cr.call_id,
    CASE 
        WHEN cr.call_time_sec > 500 THEN '오늘 건강 상태가 양호하시며, 식사도 잘 하셨다고 합니다. 약 복용도 정상적으로 하고 계십니다.'
        WHEN cr.call_time_sec > 400 THEN '컨디션이 좋으시고 기분도 좋다고 하십니다. 산책도 다녀오셨다고 합니다.'
        WHEN cr.call_time_sec > 300 THEN '평소와 같이 잘 지내고 계십니다. 특별한 불편사항은 없으십니다.'
        ELSE '간단한 안부 통화를 진행했습니다. 건강 상태 양호합니다.'
    END as content,
    NOW()
FROM call_records cr
WHERE cr.elderly_user_id = 3 
  AND cr.state = 'COMPLETED'
  AND cr.call_time_sec > 0;


-- 3. 감정 분석 데이터 생성
INSERT INTO call_emotions (call_id, emotion_level, created_at)
SELECT 
    cr.call_id,
    CASE 
        WHEN RAND() > 0.8 THEN 'VERY_POSITIVE'
        WHEN RAND() > 0.5 THEN 'POSITIVE'
        WHEN RAND() > 0.3 THEN 'NEUTRAL'
        WHEN RAND() > 0.1 THEN 'NEGATIVE'
        ELSE 'VERY_NEGATIVE'
    END as emotion_level,
    NOW()
FROM call_records cr
WHERE cr.elderly_user_id = 3 
  AND cr.state = 'COMPLETED'
  AND cr.call_time_sec > 0;


-- 4. 일일 상태 데이터 생성 (최근 7일)
INSERT INTO call_daily_status (call_id, meal_taken, health_status, sleep_status, created_at)
SELECT 
    cr.call_id,
    CASE WHEN RAND() > 0.3 THEN true ELSE false END as meal_taken,
    CASE WHEN RAND() > 0.7 THEN 'GOOD' WHEN RAND() > 0.3 THEN 'NORMAL' ELSE 'BAD' END as health_status,
    CASE WHEN RAND() > 0.7 THEN 'GOOD' WHEN RAND() > 0.3 THEN 'NORMAL' ELSE 'BAD' END as sleep_status,
    NOW()
FROM call_records cr
WHERE cr.elderly_user_id = 3 
  AND cr.state = 'COMPLETED'
  AND cr.call_time_sec > 0
  AND cr.call_at >= DATE_SUB(NOW(), INTERVAL 7 DAY);


-- 5. LLM 모델 사용 기록 (CallBot 발화 - 질문)
INSERT INTO llm_models (call_id, prompt, created_at)
SELECT 
    cr.call_id,
    '안녕하세요, 오늘 기분은 어떠세요?' as prompt,
    NOW()
FROM call_records cr
WHERE cr.elderly_user_id = 3 
  AND cr.state = 'COMPLETED'
  AND cr.call_time_sec > 0;


-- 6. 어르신 응답 데이터 생성
INSERT INTO elderly_responses (model_id, call_id, content, responded_at, danger, danger_reason)
SELECT 
    lm.model_id,
    lm.call_id,
    CASE 
        WHEN RAND() > 0.7 THEN '아주 좋아요. 오늘 날씨도 좋고 기분이 상쾌합니다.'
        WHEN RAND() > 0.5 THEN '괜찮아요. 평소처럼 지내고 있어요.'
        WHEN RAND() > 0.3 THEN '그저 그래요. 조금 피곤하네요.'
        ELSE '네, 잘 먹었어요. 맛있게 먹었습니다.'
    END as content,
    DATE_ADD(cr.call_at, INTERVAL 5 SECOND) as responded_at,
    false as danger,
    NULL as danger_reason
FROM llm_models lm
JOIN call_records cr ON lm.call_id = cr.call_id
WHERE cr.elderly_user_id = 3 
  AND cr.state = 'COMPLETED'
  AND cr.call_time_sec > 0;


-- 완료 메시지
-- =====================================================
-- 7. 상담사 통화 리뷰 (counselor_call_reviews)
-- =====================================================
-- 기존 리뷰 데이터 삭제 (중복 실행 방지)
DELETE FROM counselor_call_reviews 
WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);

-- 완료된 통화 중 80%에 대해 상담사 리뷰 생성 (상담사 ID = 1)
INSERT INTO counselor_call_reviews (call_id, counselor_user_id, reviewed_at, comment, is_urgent, created_at, updated_at)
SELECT 
    cr.call_id,
    1 as counselor_user_id,
    DATE_ADD(cr.call_at, INTERVAL FLOOR(1 + RAND() * 24) HOUR) as reviewed_at,
    CASE 
        WHEN ce.emotion_level = 'VERY_POSITIVE' THEN '오늘 어르신께서 매우 밝고 긍정적인 모습을 보이셨습니다. 건강 상태도 양호하시고 일상생활에 큰 문제가 없어 보입니다.'
        WHEN ce.emotion_level = 'POSITIVE' THEN '전반적으로 좋은 상태를 유지하고 계십니다. 식사와 수면도 규칙적이시고 기분도 좋아 보이십니다.'
        WHEN ce.emotion_level = 'NEUTRAL' THEN '평소와 비슷한 상태를 유지하고 계십니다. 특별한 문제는 없으나 지속적인 관심이 필요합니다.'
        WHEN ce.emotion_level = 'NEGATIVE' THEN '오늘 어르신께서 다소 우울하거나 불안한 모습을 보이셨습니다. 보호자님의 관심과 연락이 필요해 보입니다.'
        WHEN ce.emotion_level = 'VERY_NEGATIVE' THEN '어르신의 감정 상태가 좋지 않습니다. 가능한 빠른 시일 내에 직접 연락하시거나 방문하시는 것을 권장드립니다.'
        ELSE '통화 내용을 확인했습니다. 전반적으로 안정적인 상태입니다.'
    END as comment,
    CASE 
        WHEN ce.emotion_level IN ('NEGATIVE', 'VERY_NEGATIVE') THEN 1
        ELSE 0
    END as is_urgent,
    DATE_ADD(cr.call_at, INTERVAL FLOOR(1 + RAND() * 24) HOUR) as created_at,
    DATE_ADD(cr.call_at, INTERVAL FLOOR(1 + RAND() * 24) HOUR) as updated_at
FROM call_records cr
LEFT JOIN call_emotions ce ON cr.call_id = ce.call_id
WHERE cr.state = 'COMPLETED' 
  AND cr.elderly_user_id = 3
  AND RAND() < 0.8  -- 80%의 통화에만 리뷰 생성
ORDER BY cr.call_at DESC;

-- 성공 메시지
SELECT '통화 기록 더미 데이터 생성 완료!' as message;
SELECT COUNT(*) as total_call_records FROM call_records WHERE elderly_user_id = 3;
SELECT COUNT(*) as completed_calls FROM call_records WHERE elderly_user_id = 3 AND state = 'COMPLETED';
SELECT COUNT(*) as failed_calls FROM call_records WHERE elderly_user_id = 3 AND state = 'FAILED';

SELECT '상담사 리뷰 생성 완료!' as message;
SELECT COUNT(*) as total_reviews FROM counselor_call_reviews WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3);
SELECT COUNT(*) as urgent_reviews FROM counselor_call_reviews WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3) AND is_urgent = 1;
