-- 어르신 665의 상담사 리뷰 개수 확인
SELECT 
    COUNT(*) as total_reviews,
    COUNT(*) / 10 as expected_pages
FROM counselor_call_reviews ccr
JOIN call_records cr ON ccr.call_id = cr.call_id
WHERE cr.elderly_user_id = 665;

-- 최근 리뷰 목록
SELECT 
    ccr.review_id,
    ccr.call_id,
    cr.call_at,
    ccr.comment
FROM counselor_call_reviews ccr
JOIN call_records cr ON ccr.call_id = cr.call_id
WHERE cr.elderly_user_id = 665
ORDER BY ccr.reviewed_at DESC
LIMIT 15;
