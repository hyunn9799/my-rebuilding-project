-- 통화 기록 확인
SELECT '=== 통화 기록 (call_records) ===' as info;
SELECT call_id, elderly_user_id, call_at, state 
FROM call_records 
WHERE elderly_user_id = 3 
ORDER BY call_at DESC 
LIMIT 5;

-- 상담사 리뷰 확인
SELECT '=== 상담사 리뷰 (counselor_call_reviews) ===' as info;
SELECT ccr.review_id, ccr.call_id, ccr.counselor_user_id, ccr.comment, ccr.reviewed_at
FROM counselor_call_reviews ccr
WHERE ccr.call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3)
ORDER BY ccr.reviewed_at DESC
LIMIT 5;

-- 보호자-어르신 연결 확인
SELECT '=== 보호자-어르신 연결 (guardian_elderly) ===' as info;
SELECT * FROM guardian_elderly WHERE elderly_user_id = 3;

-- 리뷰 카운트
SELECT '=== 데이터 카운트 ===' as info;
SELECT 
    (SELECT COUNT(*) FROM call_records WHERE elderly_user_id = 3) as total_calls,
    (SELECT COUNT(*) FROM call_records WHERE elderly_user_id = 3 AND state = 'COMPLETED') as completed_calls,
    (SELECT COUNT(*) FROM counselor_call_reviews WHERE call_id IN (SELECT call_id FROM call_records WHERE elderly_user_id = 3)) as total_reviews;
