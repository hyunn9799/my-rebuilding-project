-- 어르신 ID 665에 대한 통화 기록 생성
USE silverlink;

SET FOREIGN_KEY_CHECKS = 0;

-- 0. 기존 통화 기록 데이터 초기화 (중복 방지)
DELETE FROM counselor_call_reviews WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 665);
DELETE FROM call_daily_status WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 665);
DELETE FROM call_emotions WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 665);
DELETE FROM call_summaries WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 665);
DELETE FROM elderly_responses WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 665);
DELETE FROM llm_models WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 665);
DELETE FROM call_records WHERE elderly_user_id = 665;

SET FOREIGN_KEY_CHECKS = 1;

-- 1. 통화 기록 생성 (최근 30일간 24건)
INSERT INTO call_records (elderly_user_id, call_at, call_time_sec, state, recording_url)
VALUES 
-- 오늘 통화
(665, DATE_SUB(NOW(), INTERVAL 2 HOUR), 420, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_001.mp3'),
(665, DATE_SUB(NOW(), INTERVAL 5 HOUR), 360, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_002.mp3'),
-- 어제 통화
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 10 HOUR), 480, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_003.mp3'),
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 1 DAY), INTERVAL 16 HOUR), 300, 'FAILED', NULL),
-- 2일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 9 HOUR), 390, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_004.mp3'),
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 2 DAY), INTERVAL 20 HOUR), 450, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_005.mp3'),
-- 3일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 3 DAY), INTERVAL 11 HOUR), 540, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_006.mp3'),
-- 4일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 4 DAY), INTERVAL 14 HOUR), 330, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_007.mp3'),
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 4 DAY), INTERVAL 19 HOUR), 420, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_008.mp3'),
-- 5일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 5 DAY), INTERVAL 10 HOUR), 360, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_009.mp3'),
-- 6일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 6 DAY), INTERVAL 15 HOUR), 480, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_010.mp3'),
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 6 DAY), INTERVAL 21 HOUR), 300, 'FAILED', NULL),
-- 7일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 7 DAY), INTERVAL 12 HOUR), 390, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_011.mp3'),
-- 10일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 10 DAY), INTERVAL 13 HOUR), 450, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_012.mp3'),
-- 12일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 12 DAY), INTERVAL 11 HOUR), 420, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_013.mp3'),
-- 15일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 15 DAY), INTERVAL 14 HOUR), 360, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_014.mp3'),
-- 18일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 18 DAY), INTERVAL 10 HOUR), 480, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_015.mp3'),
-- 20일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 20 DAY), INTERVAL 15 HOUR), 390, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_016.mp3'),
-- 22일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 22 DAY), INTERVAL 12 HOUR), 330, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_017.mp3'),
-- 24일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 24 DAY), INTERVAL 16 HOUR), 420, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_018.mp3'),
-- 26일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 26 DAY), INTERVAL 11 HOUR), 360, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_019.mp3'),
-- 28일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 28 DAY), INTERVAL 14 HOUR), 450, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_020.mp3'),
-- 29일 전
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 29 DAY), INTERVAL 10 HOUR), 300, 'FAILED', NULL),
(665, DATE_ADD(DATE_SUB(NOW(), INTERVAL 29 DAY), INTERVAL 18 HOUR), 480, 'COMPLETED', 'https://s3.amazonaws.com/silverlink/recordings/2024/01/call_021.mp3');

-- 2. 통화 요약 생성
INSERT INTO call_summaries (call_id, content, created_at)
SELECT 
    call_id,
    CASE 
        WHEN call_time_sec >= 450 THEN '오늘 어르신께서 건강 상태가 좋으시고 기분도 밝으셨습니다. 식사를 잘 하셨고 수면도 충분히 취하셨다고 하십니다. 특별한 불편함은 없으시며 내일 친구분과 산책 약속이 있다고 하셨습니다.'
        WHEN call_time_sec >= 400 THEN '어르신께서 오늘 하루 평온하게 보내셨습니다. 아침 식사를 잘 하셨고 낮에는 TV를 시청하며 휴식을 취하셨습니다. 건강 상태는 양호하시며 특별한 문제는 없으십니다.'
        WHEN call_time_sec >= 350 THEN '오늘 어르신께서 약간 피곤해 보이셨지만 전반적으로 괜찮으십니다. 식사는 평소보다 조금 적게 하셨고 낮잠을 주무셨다고 합니다. 내일은 더 활기차게 보내실 것 같습니다.'
        ELSE '어르신과 짧은 통화를 나눴습니다. 건강 상태를 확인했으며 특별한 문제는 없으십니다.'
    END as content,
    call_at as created_at
FROM call_records
WHERE elderly_user_id = 665 AND state = 'COMPLETED';

-- 3. 감정 분석 생성
INSERT INTO call_emotions (call_id, emotion_level, created_at)
SELECT 
    call_id,
    CASE 
        WHEN RAND() < 0.30 THEN 'GOOD'
        WHEN RAND() < 0.70 THEN 'NORMAL'
        WHEN RAND() < 0.90 THEN 'BAD'
        ELSE 'DEPRESSED'
    END as emotion_level,
    call_at as created_at
FROM call_records
WHERE elderly_user_id = 665 AND state = 'COMPLETED';

-- 4. 일일 상태 생성 (최근 7일)
INSERT INTO call_daily_status (call_id, meal_taken, health_status, sleep_status, created_at)
SELECT 
    call_id,
    CASE WHEN RAND() < 0.8 THEN 1 ELSE 0 END as meal_taken,
    CASE FLOOR(RAND() * 3)
        WHEN 0 THEN 'GOOD'
        WHEN 1 THEN 'NORMAL'
        ELSE 'BAD'
    END as health_status,
    CASE FLOOR(RAND() * 3)
        WHEN 0 THEN 'GOOD'
        WHEN 1 THEN 'NORMAL'
        ELSE 'BAD'
    END as sleep_status,
    call_at as created_at
FROM call_records
WHERE elderly_user_id = 665 
  AND state = 'COMPLETED'
  AND call_at >= DATE_SUB(NOW(), INTERVAL 7 DAY);

-- 5. 어르신 응답 생성
INSERT INTO elderly_responses (call_id, model_id, content, responded_at, danger, danger_reason)
SELECT 
    cr.call_id,
    lm.model_id,
    '기분이 좋아요. 오늘 날씨가 참 좋네요.' as content,
    cr.call_at as responded_at,
    0 as danger,
    NULL as danger_reason
FROM call_records cr
JOIN llm_models lm ON cr.call_id = lm.call_id
WHERE cr.elderly_user_id = 665 AND cr.state = 'COMPLETED'
UNION ALL
SELECT 
    cr.call_id,
    lm.model_id,
    '식사는 잘 했어요. 아침에 죽을 먹었습니다.' as content,
    DATE_ADD(cr.call_at, INTERVAL 30 SECOND) as responded_at,
    0 as danger,
    NULL as danger_reason
FROM call_records cr
JOIN llm_models lm ON cr.call_id = lm.call_id
WHERE cr.elderly_user_id = 665 AND cr.state = 'COMPLETED';

-- 6. LLM 모델 사용 기록
INSERT INTO llm_models (call_id, prompt, created_at)
SELECT 
    call_id,
    '안녕하세요, 오늘 기분은 어떠세요?' as prompt,
    call_at as created_at
FROM call_records
WHERE elderly_user_id = 665 AND state = 'COMPLETED';

-- 7. 상담사 통화 리뷰 생성 (완료된 통화의 80%)
INSERT INTO counselor_call_reviews (call_id, counselor_user_id, reviewed_at, comment, is_urgent, created_at, updated_at)
SELECT 
    cr.call_id,
    1 as counselor_user_id,
    DATE_ADD(cr.call_at, INTERVAL FLOOR(1 + RAND() * 24) HOUR) as reviewed_at,
    CASE 
        WHEN ce.emotion_level = 'GOOD' THEN '오늘 어르신께서 매우 밝고 긍정적인 모습을 보이셨습니다. 건강 상태도 양호하시고 일상생활에 큰 문제가 없어 보입니다.'
        WHEN ce.emotion_level = 'NORMAL' THEN '평소와 비슷한 상태를 유지하고 계십니다. 특별한 문제는 없으나 지속적인 관심이 필요합니다.'
        WHEN ce.emotion_level = 'BAD' THEN '오늘 어르신께서 다소 우울하거나 불안한 모습을 보이셨습니다. 보호자님의 관심과 연락이 필요해 보입니다.'
        WHEN ce.emotion_level = 'DEPRESSED' THEN '어르신의 감정 상태가 좋지 않습니다. 가능한 빠른 시일 내에 직접 연락하시거나 방문하시는 것을 권장드립니다.'
        ELSE '통화 내용을 확인했습니다. 전반적으로 안정적인 상태입니다.'
    END as comment,
    CASE 
        WHEN ce.emotion_level IN ('BAD', 'DEPRESSED') THEN 1
        ELSE 0
    END as is_urgent,
    DATE_ADD(cr.call_at, INTERVAL FLOOR(1 + RAND() * 24) HOUR) as created_at,
    DATE_ADD(cr.call_at, INTERVAL FLOOR(1 + RAND() * 24) HOUR) as updated_at
FROM call_records cr
LEFT JOIN call_emotions ce ON cr.call_id = ce.call_id
WHERE cr.state = 'COMPLETED' 
  AND cr.elderly_user_id = 665
  AND RAND() < 0.8
ORDER BY cr.call_at DESC;

-- 성공 메시지
SELECT '통화 기록 생성 완료 (어르신 ID: 665)' as message;
SELECT COUNT(*) as total_call_records FROM call_records WHERE elderly_user_id = 665;
SELECT COUNT(*) as completed_calls FROM call_records WHERE elderly_user_id = 665 AND state = 'COMPLETED';
SELECT COUNT(*) as failed_calls FROM call_records WHERE elderly_user_id = 665 AND state = 'FAILED';
SELECT COUNT(*) as total_reviews FROM counselor_call_reviews WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 665);
