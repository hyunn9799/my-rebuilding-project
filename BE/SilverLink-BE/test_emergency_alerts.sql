-- =====================================================
-- 긴급 알림 테스트 데이터 SQL
-- SilverLink Emergency Alert Test Data
-- =====================================================

-- 주의: 아래 데이터를 삽입하기 전에 기존 데이터 확인
-- SELECT * FROM emergency_alerts;
-- SELECT * FROM emergency_alert_recipients;

-- =====================================================
-- 1. 긴급 알림 데이터 삽입
-- elderly_user_id: 어르신 user_id (users 테이블 참조)
-- assigned_counselor_id: 담당 상담사 counselor_id (counselors 테이블 참조)
-- =====================================================

-- 먼저 어르신 ID와 상담사 ID 확인
-- SELECT u.user_id, e.elderly_id, u.name FROM users u JOIN elderly e ON u.user_id = e.user_id;
-- SELECT c.counselor_id, u.name FROM counselors c JOIN users u ON c.user_id = u.user_id;

-- [테스트 1] CRITICAL 심각도 - 건강 위험 알림
INSERT INTO emergency_alerts (
    elderly_user_id,
    call_id,
    severity,
    alert_type,
    title,
    description,
    danger_keywords,
    related_stt_content,
    status,
    assigned_counselor_id,
    processed_by_user_id,
    processed_at,
    resolution_note,
    created_at,
    updated_at
) VALUES (
    5,  -- elderly_user_id (어르신의 user_id, 실제 데이터에 맞게 수정 필요)
    NULL,  -- call_id
    'CRITICAL',  -- severity: CRITICAL, WARNING
    'HEALTH',  -- alert_type: HEALTH, MENTAL, NO_RESPONSE
    '건강 위험 감지: 김순자 어르신',
    '통화 중 "아파요", "숨이 차요" 등 건강 위험 키워드가 감지되었습니다. 즉시 확인이 필요합니다.',
    '["아파요", "숨이 차요", "어지러워요"]',  -- danger_keywords (JSON)
    '네... 오늘은 좀... 아파요... 숨이 차요... 어지러워요...',  -- related_stt_content
    'PENDING',  -- status: PENDING, IN_PROGRESS, RESOLVED, ESCALATED
    1,  -- assigned_counselor_id (담당 상담사 counselor_id)
    NULL,  -- processed_by_user_id
    NULL,  -- processed_at
    NULL,  -- resolution_note
    NOW(),  -- created_at
    NOW()   -- updated_at
);

-- [테스트 2] WARNING 심각도 - 정서 위험 알림
INSERT INTO emergency_alerts (
    elderly_user_id,
    call_id,
    severity,
    alert_type,
    title,
    description,
    danger_keywords,
    related_stt_content,
    status,
    assigned_counselor_id,
    processed_by_user_id,
    processed_at,
    resolution_note,
    created_at,
    updated_at
) VALUES (
    5,  -- elderly_user_id
    NULL,
    'WARNING',
    'MENTAL',
    '정서 위험 감지: 김순자 어르신',
    '통화 중 "외로워요", "힘들어요" 등 정서적 위험 키워드가 감지되었습니다.',
    '["외로워요", "힘들어요", "무서워요"]',
    '요즘... 너무 외로워요... 힘들어요... 무서워요...',
    'PENDING',
    1,
    NULL,
    NULL,
    NULL,
    NOW(),
    NOW()
);

-- =====================================================
-- 2. 긴급 알림 수신자 데이터 삽입
-- receiver_user_id: 알림을 받을 사용자 ID (상담사, 관리자, 보호자)
-- receiver_role: ADMIN, COUNSELOR, GUARDIAN
-- =====================================================

-- 상담사에게 알림 수신자 등록 (첫 번째 알림)
-- 알림 ID는 위에서 생성된 alert_id를 사용 (LAST_INSERT_ID() 또는 조회 후 입력)
SET @alert_id_1 = LAST_INSERT_ID() - 1;  -- 첫 번째 알림
SET @alert_id_2 = LAST_INSERT_ID();      -- 두 번째 알림

-- 상담사 user_id 확인 (counselors 테이블에서 user_id 참조)
-- SELECT c.counselor_id, c.user_id, u.name FROM counselors c JOIN users u ON c.user_id = u.user_id;

-- 첫 번째 알림 수신자 (상담사)
INSERT INTO emergency_alert_recipients (
    alert_id,
    receiver_user_id,
    receiver_role,
    is_read,
    read_at,
    sms_required,
    sms_sent,
    sms_sent_at,
    sms_delivery_status,
    created_at
) VALUES (
    @alert_id_1,
    2,  -- receiver_user_id (상담사의 user_id, 실제 데이터에 맞게 수정)
    'COUNSELOR',
    FALSE,  -- is_read: 미확인 상태
    NULL,
    FALSE,
    FALSE,
    NULL,
    NULL,
    NOW()
);

-- 두 번째 알림 수신자 (상담사)
INSERT INTO emergency_alert_recipients (
    alert_id,
    receiver_user_id,
    receiver_role,
    is_read,
    read_at,
    sms_required,
    sms_sent,
    sms_sent_at,
    sms_delivery_status,
    created_at
) VALUES (
    @alert_id_2,
    2,  -- receiver_user_id
    'COUNSELOR',
    FALSE,
    NULL,
    FALSE,
    FALSE,
    NULL,
    NULL,
    NOW()
);

-- =====================================================
-- 선택적: 관리자에게도 알림 수신자 등록
-- =====================================================
-- INSERT INTO emergency_alert_recipients (
--     alert_id,
--     receiver_user_id,
--     receiver_role,
--     is_read,
--     created_at
-- ) VALUES (
--     @alert_id_1,
--     1,  -- 관리자 user_id
--     'ADMIN',
--     FALSE,
--     NOW()
-- );

-- =====================================================
-- 확인용 쿼리
-- =====================================================
-- 생성된 알림 확인
SELECT * FROM emergency_alerts ORDER BY created_at DESC;

-- 수신자 확인
SELECT ear.*, u.name as receiver_name, ea.title as alert_title
FROM emergency_alert_recipients ear
JOIN users u ON ear.receiver_user_id = u.user_id
JOIN emergency_alerts ea ON ear.alert_id = ea.alert_id
ORDER BY ear.created_at DESC;

-- 특정 사용자의 미확인 알림 확인 (user_id = 2 예시)
SELECT ea.*, ear.is_read, ear.receiver_role
FROM emergency_alerts ea
JOIN emergency_alert_recipients ear ON ea.alert_id = ear.alert_id
WHERE ear.receiver_user_id = 2 AND ear.is_read = FALSE
ORDER BY ea.created_at DESC;
