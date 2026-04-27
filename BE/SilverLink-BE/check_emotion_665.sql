-- 어르신 665의 감정 데이터 확인
SELECT ce.emotion_id, ce.call_id, ce.emotion_level, ce.created_at
FROM call_emotions ce
JOIN call_records cr ON ce.call_id = cr.call_id
WHERE cr.elderly_user_id = 665
LIMIT 10;
