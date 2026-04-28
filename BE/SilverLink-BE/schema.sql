-- ============================================================
-- SilverLink DB Schema (MySQL)
-- Generated from JPA Entity Code
-- Total: 54 tables
-- ============================================================

SET FOREIGN_KEY_CHECKS = 0;
SET CHARACTER SET utf8mb4;

-- ============================================================
-- Phase 1: 기반 테이블
-- ============================================================

-- 1. 행정구역 코드
CREATE TABLE IF NOT EXISTS `administrative_division` (
    `adm_code` BIGINT NOT NULL,
    `sido_code` VARCHAR(2) NOT NULL,
    `sigungu_code` VARCHAR(3) NULL,
    `dong_code` VARCHAR(3) NULL,
    `sido_name` VARCHAR(20) NOT NULL,
    `sigungu_name` VARCHAR(20) NULL,
    `dong_name` VARCHAR(20) NULL,
    `level` VARCHAR(20) NOT NULL COMMENT 'SIDO, SIGUNGU, DONG',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `established_at` DATE NULL,
    `abolished_at` DATE NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`adm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. 통합 사용자
CREATE TABLE IF NOT EXISTS `users` (
    `user_id` BIGINT NOT NULL AUTO_INCREMENT,
    `login_id` VARCHAR(50) NOT NULL,
    `password_hash` VARCHAR(255) NOT NULL,
    `role` VARCHAR(20) NOT NULL COMMENT 'ADMIN, COUNSELOR, GUARDIAN, ELDERLY',
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, INACTIVE, LOCKED, DELETED',
    `name` VARCHAR(50) NOT NULL,
    `phone` VARCHAR(20) NOT NULL,
    `email` VARCHAR(100) NULL,
    `phone_verified` TINYINT(1) NOT NULL DEFAULT 0,
    `phone_verified_at` DATETIME NULL,
    `created_by` BIGINT NULL,
    `last_login_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    `deleted_at` DATETIME NULL,
    PRIMARY KEY (`user_id`),
    UNIQUE KEY `uk_users_login_id` (`login_id`),
    UNIQUE KEY `uk_users_phone` (`phone`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Phase 2: 역할 서브테이블
-- ============================================================

-- 3. 관리자
CREATE TABLE IF NOT EXISTS `admin` (
    `user_id` BIGINT NOT NULL,
    `adm_code` BIGINT NOT NULL,
    `admin_level` VARCHAR(20) NOT NULL COMMENT 'NATIONAL, PROVINCIAL, CITY, DISTRICT',
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_admin_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_admin_adm_code` FOREIGN KEY (`adm_code`) REFERENCES `administrative_division` (`adm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 4. 상담사
CREATE TABLE IF NOT EXISTS `counselors` (
    `user_id` BIGINT NOT NULL,
    `employee_no` VARCHAR(20) NULL,
    `department` VARCHAR(100) NULL,
    `office_phone` VARCHAR(20) NULL,
    `joined_at` DATE NULL,
    `adm_code` BIGINT NOT NULL,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_counselor_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_counselor_adm_code` FOREIGN KEY (`adm_code`) REFERENCES `administrative_division` (`adm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 5. 어르신
CREATE TABLE IF NOT EXISTS `elderly` (
    `user_id` BIGINT NOT NULL,
    `adm_code` BIGINT NOT NULL,
    `birth_date` DATE NOT NULL,
    `gender` VARCHAR(5) NOT NULL COMMENT 'M, F',
    `address_line1` VARCHAR(200) NULL,
    `address_line2` VARCHAR(200) NULL,
    `zipcode` VARCHAR(10) NULL,
    `preferred_call_time` VARCHAR(5) NULL COMMENT 'HH:mm',
    `preferred_call_days` VARCHAR(30) NULL COMMENT 'MON,WED,FRI',
    `call_schedule_enabled` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_elderly_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_elderly_adm_code` FOREIGN KEY (`adm_code`) REFERENCES `administrative_division` (`adm_code`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 6. 보호자
CREATE TABLE IF NOT EXISTS `guardians` (
    `user_id` BIGINT NOT NULL,
    `address_line1` VARCHAR(200) NULL,
    `address_line2` VARCHAR(200) NULL,
    `zipcode` VARCHAR(10) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`user_id`),
    CONSTRAINT `fk_guardian_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 7. 어르신 건강정보
CREATE TABLE IF NOT EXISTS `elderly_health_info` (
    `elderly_user_id` BIGINT NOT NULL,
    `chronic_diseases` TEXT NULL,
    `mental_health_notes` TEXT NULL,
    `special_notes` TEXT NULL,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`elderly_user_id`),
    CONSTRAINT `fk_health_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 8. 보호자-어르신 매핑
CREATE TABLE IF NOT EXISTS `guardian_elderly` (
    `guardian_elderly_id` BIGINT NOT NULL AUTO_INCREMENT,
    `guardian_user_id` BIGINT NOT NULL,
    `elderly_user_id` BIGINT NOT NULL,
    `relation_type` VARCHAR(20) NOT NULL COMMENT 'CHILD, SPOUSE, RELATIVE, OTHER',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`guardian_elderly_id`),
    UNIQUE KEY `uk_guardian` (`guardian_user_id`),
    UNIQUE KEY `uk_elderly` (`elderly_user_id`),
    CONSTRAINT `fk_ge_guardian` FOREIGN KEY (`guardian_user_id`) REFERENCES `guardians` (`user_id`),
    CONSTRAINT `fk_ge_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Phase 3: 인증/보안
-- ============================================================

-- 9. 휴대폰 인증
CREATE TABLE IF NOT EXISTS `phone_verifications` (
    `verification_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NULL,
    `phone_e164` VARCHAR(20) NOT NULL,
    `purpose` VARCHAR(30) NOT NULL COMMENT 'SIGNUP, DEVICE_REGISTRATION, PASSWORD_RESET',
    `code_hash` VARCHAR(255) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'REQUESTED' COMMENT 'REQUESTED, VERIFIED, EXPIRED, FAILED',
    `expires_at` DATETIME NOT NULL,
    `verified_at` DATETIME NULL,
    `fail_count` INT NOT NULL DEFAULT 0,
    `request_ip` VARCHAR(45) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`verification_id`),
    CONSTRAINT `fk_phone_verify_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 10. WebAuthn 크레덴셜
CREATE TABLE IF NOT EXISTS `webauthn_credentials` (
    `webauthn_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `credential_id` VARCHAR(255) NOT NULL,
    `rp_id` VARCHAR(255) NOT NULL,
    `public_key` BLOB NOT NULL,
    `sign_count` BIGINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_used_at` DATETIME NULL,
    `revoked_at` DATETIME NULL,
    `registered_by` BIGINT NULL,
    PRIMARY KEY (`webauthn_id`),
    UNIQUE KEY `uk_credential_id` (`credential_id`),
    CONSTRAINT `fk_webauthn_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_webauthn_registered` FOREIGN KEY (`registered_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 11. 인증 세션
CREATE TABLE IF NOT EXISTS `auth_sessions` (
    `session_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `refresh_jti` VARCHAR(64) NOT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, REVOKED, EXPIRED',
    `issued_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `last_seen_at` DATETIME NULL,
    `expires_at` DATETIME NOT NULL,
    `revoked_at` DATETIME NULL,
    `ip_address` VARCHAR(45) NULL,
    `user_agent` VARCHAR(255) NULL,
    `active_user_id` BIGINT GENERATED ALWAYS AS (CASE WHEN `status` = 'ACTIVE' THEN `user_id` END) STORED,
    PRIMARY KEY (`session_id`),
    CONSTRAINT `fk_session_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 12. 세션 감사 로그
CREATE TABLE IF NOT EXISTS `audit_log` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `event_type` VARCHAR(50) NOT NULL,
    `user_id` BIGINT NOT NULL,
    `session_id` VARCHAR(64) NULL,
    `ip_address` VARCHAR(45) NULL,
    `user_agent` VARCHAR(500) NULL,
    `device_id` VARCHAR(64) NULL,
    `created_at` DATETIME(6) NOT NULL,
    `detail` VARCHAR(500) NULL,
    PRIMARY KEY (`id`),
    INDEX `idx_audit_user_id` (`user_id`),
    INDEX `idx_audit_event_type` (`event_type`),
    INDEX `idx_audit_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 13. 범용 감사 로그
CREATE TABLE IF NOT EXISTS `audit_logs` (
    `audit_log_id` BIGINT NOT NULL AUTO_INCREMENT,
    `actor_user_id` BIGINT NULL,
    `action` VARCHAR(100) NOT NULL,
    `target_entity` VARCHAR(100) NULL,
    `target_id` BIGINT NULL,
    `ip_address` VARCHAR(45) NULL,
    `user_agent` VARCHAR(255) NULL,
    `meta` JSON NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`audit_log_id`),
    CONSTRAINT `fk_audit_actor` FOREIGN KEY (`actor_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 14. 동의 이력
CREATE TABLE IF NOT EXISTS `consent_histories` (
    `consent_history_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `consent_type` VARCHAR(255) NOT NULL COMMENT 'PRIVACY, SENSITIVE_INFO, MEDICATION, THIRD_PARTY',
    `action_type` VARCHAR(255) NOT NULL COMMENT 'AGREE, DISAGREE, WITHDRAW',
    `ip_address` VARCHAR(255) NULL,
    `user_agent` VARCHAR(255) NULL,
    `consent_date` DATETIME NOT NULL,
    PRIMARY KEY (`consent_history_id`),
    CONSTRAINT `fk_consent_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Phase 4: 핵심 비즈니스 (통화/배정)
-- ============================================================

-- 15. 상담사-어르신 배정
CREATE TABLE IF NOT EXISTS `assignments` (
    `assignment_id` BIGINT NOT NULL AUTO_INCREMENT,
    `counselor_user_id` BIGINT NOT NULL,
    `elderly_user_id` BIGINT NOT NULL,
    `assigned_by_admin_user_id` BIGINT NOT NULL,
    `assigned_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `ended_at` DATETIME NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE' COMMENT 'ACTIVE, ENDED',
    PRIMARY KEY (`assignment_id`),
    CONSTRAINT `fk_assign_counselor` FOREIGN KEY (`counselor_user_id`) REFERENCES `counselors` (`user_id`),
    CONSTRAINT `fk_assign_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`),
    CONSTRAINT `fk_assign_admin` FOREIGN KEY (`assigned_by_admin_user_id`) REFERENCES `admin` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 16. 통화 기록
CREATE TABLE IF NOT EXISTS `call_records` (
    `call_id` BIGINT NOT NULL AUTO_INCREMENT,
    `elderly_user_id` BIGINT NOT NULL,
    `call_at` DATETIME NOT NULL,
    `call_time_sec` INT NOT NULL DEFAULT 0,
    `state` VARCHAR(20) NOT NULL COMMENT 'REQUESTED, ANSWERED, FAILED, COMPLETED, CANCELLED',
    `recording_url` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`call_id`),
    INDEX `idx_call_records_elderly_time` (`elderly_user_id`, `call_at`),
    INDEX `idx_call_records_state_time` (`state`, `call_at`),
    CONSTRAINT `fk_call_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 17. 감정 분석 결과
CREATE TABLE IF NOT EXISTS `call_emotions` (
    `emotion_id` BIGINT NOT NULL AUTO_INCREMENT,
    `call_id` BIGINT NOT NULL,
    `emotion_level` VARCHAR(20) NOT NULL COMMENT 'GOOD, NORMAL, BAD, DEPRESSED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`emotion_id`),
    INDEX `idx_emotion_call_time` (`call_id`, `created_at`),
    CONSTRAINT `fk_emotion_call` FOREIGN KEY (`call_id`) REFERENCES `call_records` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 18. 통화 요약
CREATE TABLE IF NOT EXISTS `call_summaries` (
    `summary_id` BIGINT NOT NULL AUTO_INCREMENT,
    `call_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`summary_id`),
    INDEX `idx_summary_call_time` (`call_id`, `created_at`),
    CONSTRAINT `fk_summary_call` FOREIGN KEY (`call_id`) REFERENCES `call_records` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 19. 일일 상태
CREATE TABLE IF NOT EXISTS `call_daily_status` (
    `status_id` BIGINT NOT NULL AUTO_INCREMENT,
    `call_id` BIGINT NOT NULL,
    `meal_taken` TINYINT(1) NULL,
    `health_status` VARCHAR(20) NULL COMMENT 'GOOD, NORMAL, BAD',
    `health_detail` TEXT NULL,
    `sleep_status` VARCHAR(20) NULL COMMENT 'GOOD, NORMAL, BAD',
    `sleep_detail` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`status_id`),
    UNIQUE KEY `uk_daily_call` (`call_id`),
    INDEX `idx_daily_status_call` (`call_id`),
    CONSTRAINT `fk_daily_call` FOREIGN KEY (`call_id`) REFERENCES `call_records` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 20. LLM 프롬프트 기록
CREATE TABLE IF NOT EXISTS `llm_models` (
    `model_id` BIGINT NOT NULL AUTO_INCREMENT,
    `call_id` BIGINT NOT NULL,
    `prompt` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`model_id`),
    INDEX `idx_models_call` (`call_id`, `created_at`),
    CONSTRAINT `fk_model_call` FOREIGN KEY (`call_id`) REFERENCES `call_records` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 21. 어르신 응답 (STT)
CREATE TABLE IF NOT EXISTS `elderly_responses` (
    `response_id` BIGINT NOT NULL AUTO_INCREMENT,
    `model_id` BIGINT NULL,
    `call_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `responded_at` DATETIME NOT NULL,
    `danger` TINYINT(1) NOT NULL DEFAULT 0,
    `danger_reason` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`response_id`),
    INDEX `idx_elderly_responses_call_time` (`call_id`, `responded_at`),
    INDEX `idx_elderly_responses_model_time` (`model_id`, `responded_at`),
    CONSTRAINT `fk_response_model` FOREIGN KEY (`model_id`) REFERENCES `llm_models` (`model_id`),
    CONSTRAINT `fk_response_call` FOREIGN KEY (`call_id`) REFERENCES `call_records` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 22. 상담사 통화 리뷰
CREATE TABLE IF NOT EXISTS `counselor_call_reviews` (
    `review_id` BIGINT NOT NULL AUTO_INCREMENT,
    `call_id` BIGINT NOT NULL,
    `counselor_user_id` BIGINT NOT NULL,
    `reviewed_at` DATETIME NOT NULL,
    `comment` TEXT NULL,
    `is_urgent` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`review_id`),
    UNIQUE KEY `uk_review_call_counselor` (`call_id`, `counselor_user_id`),
    CONSTRAINT `fk_review_call` FOREIGN KEY (`call_id`) REFERENCES `call_records` (`call_id`),
    CONSTRAINT `fk_review_counselor` FOREIGN KEY (`counselor_user_id`) REFERENCES `counselors` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
-- ============================================================
-- Phase 5: 긴급 알림
-- ============================================================

-- 23. 긴급 알림
CREATE TABLE IF NOT EXISTS `emergency_alerts` (
    `alert_id` BIGINT NOT NULL AUTO_INCREMENT,
    `elderly_user_id` BIGINT NOT NULL,
    `call_id` BIGINT NULL,
    `alert_type` VARCHAR(30) NOT NULL COMMENT 'DANGER_DETECTED, NO_RESPONSE, MANUAL_TRIGGER, ANOMALY_DETECTED',
    `severity` VARCHAR(20) NOT NULL DEFAULT 'MEDIUM' COMMENT 'HIGH, MEDIUM, LOW',
    `alert_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, PROCESSING, RESOLVED, ESCALATED',
    `message` TEXT NOT NULL,
    `context` TEXT NULL,
    `danger_keyword` VARCHAR(100) NULL,
    `caller_id` BIGINT NULL,
    `stt_text` TEXT NULL,
    `trigger_timestamp` DATETIME NOT NULL,
    `resolved_at` DATETIME NULL,
    `resolved_by` BIGINT NULL,
    `resolution_comment` TEXT NULL,
    `escalated_to` BIGINT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`alert_id`),
    INDEX `idx_emergency_elderly` (`elderly_user_id`),
    INDEX `idx_emergency_call` (`call_id`),
    INDEX `idx_emergency_status` (`alert_status`),
    INDEX `idx_emergency_type` (`alert_type`),
    INDEX `idx_emergency_created_at` (`created_at`),
    INDEX `idx_emergency_severity` (`severity`),
    INDEX `idx_emergency_resolved_by` (`resolved_by`),
    CONSTRAINT `fk_alert_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`),
    CONSTRAINT `fk_alert_call` FOREIGN KEY (`call_id`) REFERENCES `call_records` (`call_id`),
    CONSTRAINT `fk_alert_resolved` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 24. 긴급 알림 수신자
CREATE TABLE IF NOT EXISTS `emergency_alert_recipients` (
    `recipient_id` BIGINT NOT NULL AUTO_INCREMENT,
    `alert_id` BIGINT NOT NULL,
    `receiver_user_id` BIGINT NOT NULL,
    `receiver_role` VARCHAR(20) NOT NULL COMMENT 'ADMIN, COUNSELOR, GUARDIAN',
    `notification_channel` VARCHAR(20) NOT NULL DEFAULT 'SYSTEM' COMMENT 'SMS, PUSH, SYSTEM, CALL',
    `notification_sent_at` DATETIME NULL,
    `notification_read_at` DATETIME NULL,
    `notification_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SENT, FAILED, READ',
    `retry_count` INT NOT NULL DEFAULT 0,
    `action_taken` TEXT NULL,
    `action_taken_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`recipient_id`),
    UNIQUE KEY `uk_alert_receiver` (`alert_id`, `receiver_user_id`),
    INDEX `idx_recipient_alert` (`alert_id`),
    INDEX `idx_recipient_user` (`receiver_user_id`),
    INDEX `idx_recipient_status` (`notification_status`),
    INDEX `idx_recipient_channel` (`notification_channel`),
    CONSTRAINT `fk_recipient_alert` FOREIGN KEY (`alert_id`) REFERENCES `emergency_alerts` (`alert_id`),
    CONSTRAINT `fk_recipient_user` FOREIGN KEY (`receiver_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 25. SMS 발송 로그
CREATE TABLE IF NOT EXISTS `sms_logs` (
    `sms_log_id` BIGINT NOT NULL AUTO_INCREMENT,
    `alert_id` BIGINT NULL,
    `sender_user_id` BIGINT NULL,
    `receiver_user_id` BIGINT NULL,
    `receiver_phone` VARCHAR(20) NOT NULL,
    `receiver_name` VARCHAR(50) NULL,
    `sms_type` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'NORMAL, EMERGENCY',
    `message_content` TEXT NOT NULL,
    `template_code` VARCHAR(50) NULL,
    `sms_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, SENT, FAILED, CANCELLED',
    `external_message_id` VARCHAR(100) NULL,
    `provider` VARCHAR(30) NULL COMMENT 'TWILIO, NAVER',
    `retry_count` INT NOT NULL DEFAULT 0,
    `error_code` VARCHAR(50) NULL,
    `error_message` TEXT NULL,
    `sent_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`sms_log_id`),
    INDEX `idx_sms_alert` (`alert_id`),
    INDEX `idx_sms_sender` (`sender_user_id`),
    INDEX `idx_sms_receiver` (`receiver_user_id`),
    INDEX `idx_sms_receiver_phone` (`receiver_phone`),
    INDEX `idx_sms_status` (`sms_status`),
    INDEX `idx_sms_sent_at` (`sent_at`),
    CONSTRAINT `fk_sms_alert` FOREIGN KEY (`alert_id`) REFERENCES `emergency_alerts` (`alert_id`),
    CONSTRAINT `fk_sms_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_sms_receiver` FOREIGN KEY (`receiver_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Phase 6: 복약/파일
-- ============================================================

-- 26. 파일 첨부
CREATE TABLE IF NOT EXISTS `file_attachments` (
    `file_id` BIGINT NOT NULL AUTO_INCREMENT,
    `uploader_user_id` BIGINT NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_path` VARCHAR(500) NOT NULL,
    `file_type` VARCHAR(50) NOT NULL COMMENT 'MEDICATION_OCR_IMAGE, etc.',
    `file_size` BIGINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`file_id`),
    CONSTRAINT `fk_file_uploader` FOREIGN KEY (`uploader_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 27. 복약 OCR 로그 (BE 전용)
CREATE TABLE IF NOT EXISTS `medication_ocr_logs` (
    `ocr_log_id` BIGINT NOT NULL AUTO_INCREMENT,
    `elderly_user_id` BIGINT NOT NULL,
    `file_id` BIGINT NOT NULL,
    `ocr_text` TEXT NULL,
    `medicine_name` VARCHAR(200) NULL,
    `dosage_info` TEXT NULL,
    `ocr_status` VARCHAR(20) NOT NULL DEFAULT 'PROCESSING' COMMENT 'PROCESSING, SUCCESS, FAILED',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`ocr_log_id`),
    CONSTRAINT `fk_ocr_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`),
    CONSTRAINT `fk_ocr_file` FOREIGN KEY (`file_id`) REFERENCES `file_attachments` (`file_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 28. 복약 스케줄
CREATE TABLE IF NOT EXISTS `medication_schedules` (
    `schedule_id` BIGINT NOT NULL AUTO_INCREMENT,
    `elderly_user_id` BIGINT NOT NULL,
    `medicine_name` VARCHAR(200) NOT NULL,
    `dosage` VARCHAR(100) NULL,
    `frequency` VARCHAR(50) NULL COMMENT 'DAILY, TWICE_DAILY, THREE_TIMES_DAILY',
    `start_date` DATE NOT NULL,
    `end_date` DATE NULL,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `notes` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`schedule_id`),
    CONSTRAINT `fk_med_schedule_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 29. 복약 시간
CREATE TABLE IF NOT EXISTS `medication_schedule_times` (
    `time_id` BIGINT NOT NULL AUTO_INCREMENT,
    `schedule_id` BIGINT NOT NULL,
    `take_time` TIME NOT NULL,
    PRIMARY KEY (`time_id`),
    CONSTRAINT `fk_med_time_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `medication_schedules` (`schedule_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 30. 복약 기록
CREATE TABLE IF NOT EXISTS `medication_intake_logs` (
    `intake_id` BIGINT NOT NULL AUTO_INCREMENT,
    `schedule_id` BIGINT NOT NULL,
    `time_id` BIGINT NOT NULL,
    `intake_date` DATE NOT NULL,
    `taken` TINYINT(1) NOT NULL DEFAULT 0,
    `taken_at` DATETIME NULL,
    `skip_reason` VARCHAR(200) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`intake_id`),
    INDEX `idx_intake_date` (`schedule_id`, `intake_date`),
    CONSTRAINT `fk_intake_schedule` FOREIGN KEY (`schedule_id`) REFERENCES `medication_schedules` (`schedule_id`),
    CONSTRAINT `fk_intake_time` FOREIGN KEY (`time_id`) REFERENCES `medication_schedule_times` (`time_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Phase 7: 스케줄/일정
-- ============================================================

-- 31. 스케줄 변경 요청
CREATE TABLE IF NOT EXISTS `schedule_change_request` (
    `change_request_id` BIGINT NOT NULL AUTO_INCREMENT,
    `elderly_user_id` BIGINT NOT NULL,
    `requester_user_id` BIGINT NOT NULL,
    `request_type` VARCHAR(30) NOT NULL COMMENT 'TIME_CHANGE, DAY_CHANGE, SKIP, CANCEL',
    `old_value` VARCHAR(200) NULL,
    `new_value` VARCHAR(200) NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED',
    `reason` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`change_request_id`),
    CONSTRAINT `fk_change_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`),
    CONSTRAINT `fk_change_requester` FOREIGN KEY (`requester_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 32. 통화 스케줄 이력
CREATE TABLE IF NOT EXISTS `call_schedule_history` (
    `history_id` BIGINT NOT NULL AUTO_INCREMENT,
    `elderly_user_id` BIGINT NOT NULL,
    `changed_by` BIGINT NOT NULL,
    `change_type` VARCHAR(30) NOT NULL COMMENT 'TIME_CHANGE, DAY_CHANGE, PAUSE, RESUME',
    `old_call_time` VARCHAR(5) NULL,
    `new_call_time` VARCHAR(5) NULL,
    `old_call_days` VARCHAR(30) NULL,
    `new_call_days` VARCHAR(30) NULL,
    `old_enabled` TINYINT(1) NULL,
    `new_enabled` TINYINT(1) NULL,
    `reason` TEXT NULL,
    `effective_from` DATE NOT NULL,
    `effective_to` DATE NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`history_id`),
    INDEX `idx_schedule_elderly` (`elderly_user_id`, `effective_from`),
    CONSTRAINT `fk_schedule_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`),
    CONSTRAINT `fk_schedule_changed_by` FOREIGN KEY (`changed_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Phase 8: 접근 요청/고객지원
-- ============================================================

-- 33. 접근 요청
CREATE TABLE IF NOT EXISTS `access_requests` (
    `access_request_id` BIGINT NOT NULL AUTO_INCREMENT,
    `requester_user_id` BIGINT NOT NULL,
    `request_type` VARCHAR(30) NOT NULL COMMENT 'ELDERLY_ACCESS, DATA_VIEW, MEDICATION_ACCESS',
    `target_elderly_user_id` BIGINT NULL,
    `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING, APPROVED, REJECTED, EXPIRED',
    `reason` TEXT NULL,
    `approved_by` BIGINT NULL,
    `approved_at` DATETIME NULL,
    `rejected_reason` TEXT NULL,
    `expires_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`access_request_id`),
    CONSTRAINT `fk_access_requester` FOREIGN KEY (`requester_user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_access_target` FOREIGN KEY (`target_elderly_user_id`) REFERENCES `elderly` (`user_id`),
    CONSTRAINT `fk_access_approved` FOREIGN KEY (`approved_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 34. 문의 게시판
CREATE TABLE IF NOT EXISTS `inquiries` (
    `inquiry_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL, TECHNICAL, ACCOUNT',
    `status` VARCHAR(20) NOT NULL DEFAULT 'OPEN' COMMENT 'OPEN, IN_PROGRESS, RESOLVED, CLOSED',
    `is_private` TINYINT(1) NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`inquiry_id`),
    CONSTRAINT `fk_inquiry_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 35. 문의 답변
CREATE TABLE IF NOT EXISTS `inquiry_answers` (
    `answer_id` BIGINT NOT NULL AUTO_INCREMENT,
    `inquiry_id` BIGINT NOT NULL,
    `admin_user_id` BIGINT NOT NULL,
    `content` TEXT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`answer_id`),
    CONSTRAINT `fk_answer_inquiry` FOREIGN KEY (`inquiry_id`) REFERENCES `inquiries` (`inquiry_id`),
    CONSTRAINT `fk_answer_admin` FOREIGN KEY (`admin_user_id`) REFERENCES `admin` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 36. FAQ
CREATE TABLE IF NOT EXISTS `faqs` (
    `faq_id` BIGINT NOT NULL AUTO_INCREMENT,
    `question` VARCHAR(500) NOT NULL,
    `answer` TEXT NOT NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    `display_order` INT NOT NULL DEFAULT 0,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_by` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`faq_id`),
    CONSTRAINT `fk_faq_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 37. 민원
CREATE TABLE IF NOT EXISTS `complaints` (
    `complaint_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'GENERAL',
    `status` VARCHAR(20) NOT NULL DEFAULT 'SUBMITTED' COMMENT 'SUBMITTED, REVIEWING, RESOLVED, REJECTED',
    `response` TEXT NULL,
    `resolved_by` BIGINT NULL,
    `resolved_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`complaint_id`),
    CONSTRAINT `fk_complaint_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_complaint_resolver` FOREIGN KEY (`resolved_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 38. 상담 기록
CREATE TABLE IF NOT EXISTS `counseling_record` (
    `record_id` BIGINT NOT NULL AUTO_INCREMENT,
    `counselor_user_id` BIGINT NOT NULL,
    `elderly_user_id` BIGINT NOT NULL,
    `call_id` BIGINT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `counseling_type` VARCHAR(30) NOT NULL DEFAULT 'PHONE' COMMENT 'PHONE, VISIT, ONLINE',
    `mood` VARCHAR(20) NULL COMMENT 'GOOD, NORMAL, BAD, DEPRESSED',
    `health_note` TEXT NULL,
    `special_note` TEXT NULL,
    `follow_up_needed` TINYINT(1) NOT NULL DEFAULT 0,
    `follow_up_date` DATE NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`record_id`),
    INDEX `idx_counseling_counselor` (`counselor_user_id`),
    INDEX `idx_counseling_elderly` (`elderly_user_id`),
    INDEX `idx_counseling_call` (`call_id`),
    CONSTRAINT `fk_counseling_counselor` FOREIGN KEY (`counselor_user_id`) REFERENCES `counselors` (`user_id`),
    CONSTRAINT `fk_counseling_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`),
    CONSTRAINT `fk_counseling_call` FOREIGN KEY (`call_id`) REFERENCES `call_records` (`call_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Phase 9: 공지사항/알림
-- ============================================================

-- 39. 공지사항
CREATE TABLE IF NOT EXISTS `notices` (
    `notice_id` BIGINT NOT NULL AUTO_INCREMENT,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `notice_type` VARCHAR(30) NOT NULL DEFAULT 'GENERAL' COMMENT 'GENERAL, URGENT, SYSTEM, EVENT',
    `target_mode` VARCHAR(20) NOT NULL DEFAULT 'ALL' COMMENT 'ALL, ROLE_SET',
    `is_pinned` TINYINT(1) NOT NULL DEFAULT 0,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `author_user_id` BIGINT NOT NULL,
    `published_at` DATETIME NULL,
    `expired_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`notice_id`),
    CONSTRAINT `fk_notice_author` FOREIGN KEY (`author_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 40. 공지 첨부파일
CREATE TABLE IF NOT EXISTS `notice_attachments` (
    `attachment_id` BIGINT NOT NULL AUTO_INCREMENT,
    `notice_id` BIGINT NOT NULL,
    `file_name` VARCHAR(255) NOT NULL,
    `file_url` VARCHAR(500) NOT NULL,
    `file_size` BIGINT NOT NULL DEFAULT 0,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`attachment_id`),
    CONSTRAINT `fk_notice_attach` FOREIGN KEY (`notice_id`) REFERENCES `notices` (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 41. 공지 읽음 로그
CREATE TABLE IF NOT EXISTS `notice_read_logs` (
    `read_log_id` BIGINT NOT NULL AUTO_INCREMENT,
    `notice_id` BIGINT NOT NULL,
    `user_id` BIGINT NOT NULL,
    `read_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`read_log_id`),
    UNIQUE KEY `uk_notice_read` (`notice_id`, `user_id`),
    CONSTRAINT `fk_read_notice` FOREIGN KEY (`notice_id`) REFERENCES `notices` (`notice_id`),
    CONSTRAINT `fk_read_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 42. 공지 대상 역할
CREATE TABLE IF NOT EXISTS `notice_target_roles` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `notice_id` BIGINT NOT NULL,
    `role` VARCHAR(20) NOT NULL,
    PRIMARY KEY (`id`),
    CONSTRAINT `fk_target_notice` FOREIGN KEY (`notice_id`) REFERENCES `notices` (`notice_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 43. 알림
CREATE TABLE IF NOT EXISTS `notifications` (
    `notification_id` BIGINT NOT NULL AUTO_INCREMENT,
    `user_id` BIGINT NOT NULL,
    `title` VARCHAR(200) NOT NULL,
    `content` TEXT NOT NULL,
    `notification_type` VARCHAR(30) NOT NULL COMMENT 'CALL, EMERGENCY, MEDICATION, SYSTEM, NOTICE, INQUIRY',
    `priority` VARCHAR(20) NOT NULL DEFAULT 'NORMAL' COMMENT 'HIGH, NORMAL, LOW',
    `is_read` TINYINT(1) NOT NULL DEFAULT 0,
    `read_at` DATETIME NULL,
    `action_url` VARCHAR(500) NULL,
    `reference_type` VARCHAR(50) NULL COMMENT 'CALL, EMERGENCY_ALERT, MEDICATION, NOTICE, INQUIRY',
    `reference_id` BIGINT NULL,
    `sender_user_id` BIGINT NULL,
    `expires_at` DATETIME NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`notification_id`),
    INDEX `idx_notification_user` (`user_id`),
    INDEX `idx_notification_read` (`user_id`, `is_read`),
    INDEX `idx_notification_type` (`notification_type`),
    INDEX `idx_notification_created` (`created_at`),
    CONSTRAINT `fk_notification_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`),
    CONSTRAINT `fk_notification_sender` FOREIGN KEY (`sender_user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Phase 10: 복지/정책
-- ============================================================

-- 44. 복지 서비스
CREATE TABLE IF NOT EXISTS `welfare_services` (
    `service_id` BIGINT NOT NULL AUTO_INCREMENT,
    `service_name` VARCHAR(200) NOT NULL,
    `description` TEXT NULL,
    `category` VARCHAR(50) NOT NULL,
    `target_age_min` INT NULL,
    `target_age_max` INT NULL,
    `provider` VARCHAR(200) NULL,
    `contact_phone` VARCHAR(20) NULL,
    `contact_url` VARCHAR(500) NULL,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`service_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 45. 복지 시설
CREATE TABLE IF NOT EXISTS `welfare_facilities` (
    `facility_id` BIGINT NOT NULL AUTO_INCREMENT,
    `facility_name` VARCHAR(200) NOT NULL,
    `facility_type` VARCHAR(50) NOT NULL,
    `address` VARCHAR(300) NULL,
    `latitude` DECIMAL(10, 7) NULL,
    `longitude` DECIMAL(10, 7) NULL,
    `phone` VARCHAR(20) NULL,
    `operating_hours` VARCHAR(200) NULL,
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`facility_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 46. 정책
CREATE TABLE IF NOT EXISTS `policies` (
    `policy_id` BIGINT NOT NULL AUTO_INCREMENT,
    `policy_key` VARCHAR(100) NOT NULL,
    `policy_value` TEXT NOT NULL,
    `description` VARCHAR(500) NULL,
    `category` VARCHAR(50) NOT NULL DEFAULT 'SYSTEM',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_by` BIGINT NOT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`policy_id`),
    INDEX `idx_policy_key` (`policy_key`),
    CONSTRAINT `fk_policy_creator` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 47. 오프라인 등록 이력
CREATE TABLE IF NOT EXISTS `offline_registration_logs` (
    `reg_log_id` BIGINT NOT NULL AUTO_INCREMENT,
    `registrar_user_id` BIGINT NOT NULL,
    `target_user_id` BIGINT NOT NULL,
    `channel` VARCHAR(255) NOT NULL COMMENT 'CENTER_VISIT',
    `memo` VARCHAR(500) NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`reg_log_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- ============================================================
-- Phase 11: AI OCR 복약 파이프라인 (Python 코드 기준)
-- ============================================================
-- 주의:
-- - BE의 medication_ocr_logs (Phase 6 #27)와 별개의 AI 전용 테이블입니다.
-- - 컬럼명은 drug_repository.py, seed_aliases.py,
--   alias_suggestion_repository.py, ocr_result_repository.py의 실제 쿼리와 일치합니다.
-- - medications_master는 의약품 제품 허가정보 API(DrugPrdtPrmsnInfoService07) 기반 약품 마스터입니다.
-- - UNIQUE 기준은 정규화된 값(normalized)으로 통일했습니다.

-- 48. 의약품 마스터 (DrugPrdtPrmsnInfoService07 API)
CREATE TABLE IF NOT EXISTS `medications_master` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `item_seq` VARCHAR(20) NOT NULL COMMENT '품목기준코드 (ITEM_SEQ)',
    `item_name` VARCHAR(500) NOT NULL COMMENT '약품명 (ITEM_NAME)',
    `item_name_normalized` VARCHAR(500) NULL COMMENT '정규화된 약품명',
    `item_eng_name` VARCHAR(500) NULL COMMENT '영문 약품명 (ITEM_ENG_NAME)',
    `entp_name` VARCHAR(200) NULL COMMENT '업체명 (ENTP_NAME)',
    `entp_eng_name` VARCHAR(200) NULL COMMENT '영문 업체명 (ENTP_ENG_NAME)',
    `item_ingr_name` TEXT NULL COMMENT '주성분명 (ITEM_INGR_NAME)',
    `item_ingr_cnt` INT NULL COMMENT '성분 수 (ITEM_INGR_CNT)',
    `spclty_pblc` VARCHAR(50) NULL COMMENT '전문/일반 구분 (SPCLTY_PBLC)',
    `prduct_type` VARCHAR(100) NULL COMMENT '제품 유형 (PRDUCT_TYPE)',
    `item_permit_date` VARCHAR(20) NULL COMMENT '허가일자 (ITEM_PERMIT_DATE)',
    `cancel_date` VARCHAR(20) NULL COMMENT '취소일자 (CANCEL_DATE)',
    `cancel_name` VARCHAR(50) NULL COMMENT '취소 상태 (CANCEL_NAME)',
    `edi_code` VARCHAR(500) NULL COMMENT 'EDI 코드 (EDI_CODE)',
    `permit_kind_code` VARCHAR(50) NULL COMMENT '허가 종류 (PERMIT_KIND_CODE)',
    `efcy_qesitm` TEXT NULL COMMENT '효능효과 (e약은요 보강용, 새 API에는 없음)',
    `use_method_qesitm` TEXT NULL COMMENT '사용법 (e약은요 보강용)',
    `atpn_qesitm` TEXT NULL COMMENT '주의사항 (e약은요 보강용)',
    `intrc_qesitm` TEXT NULL COMMENT '상호작용 (e약은요 보강용)',
    `se_qesitm` TEXT NULL COMMENT '부작용 (e약은요 보강용)',
    `deposit_method_qesitm` TEXT NULL COMMENT '보관법 (e약은요 보강용)',
    `item_image` VARCHAR(500) NULL COMMENT '이미지URL (BIG_PRDT_IMG_URL)',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1 COMMENT '활성 여부 (CANCEL_DATE 있으면 0)',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_medications_master_item_seq` (`item_seq`),
    INDEX `idx_item_name` (`item_name`(200)),
    INDEX `idx_item_name_normalized` (`item_name_normalized`(200)),
    INDEX `idx_is_active` (`is_active`),
    FULLTEXT INDEX `ft_item_name` (`item_name`) WITH PARSER ngram
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='의약품 허가정보 API 기반 의약품 마스터';

-- 49. 의약품 별칭
-- [FIX #2] UNIQUE를 alias_normalized 기준으로 변경
CREATE TABLE IF NOT EXISTS `medication_aliases` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `item_seq` VARCHAR(20) NOT NULL COMMENT '품목기준코드',
    `alias_name` VARCHAR(200) NOT NULL COMMENT '약품 별칭',
    `alias_normalized` VARCHAR(200) NOT NULL COMMENT '정규화된 별칭',
    `source` VARCHAR(50) NOT NULL DEFAULT 'manual' COMMENT 'manual/user/system/seed',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_medication_alias_norm` (`item_seq`, `alias_normalized`),
    INDEX `idx_alias_name` (`alias_name`),
    INDEX `idx_alias_normalized` (`alias_normalized`),
    INDEX `idx_alias_item_seq` (`item_seq`),
    CONSTRAINT `fk_medication_alias_item_seq` FOREIGN KEY (`item_seq`) REFERENCES `medications_master` (`item_seq`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='의약품 별칭 테이블';

-- 50. OCR 오인식 별칭
-- [FIX #2] UNIQUE를 normalized_error_text 기준으로 변경
CREATE TABLE IF NOT EXISTS `medication_error_aliases` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `item_seq` VARCHAR(20) NOT NULL COMMENT '품목기준코드',
    `error_text` VARCHAR(200) NOT NULL COMMENT 'OCR 오인식 문자열',
    `normalized_error_text` VARCHAR(200) NOT NULL COMMENT '정규화된 OCR 오인식 문자열',
    `correction_reason` VARCHAR(200) NULL COMMENT '보정 사유',
    `confidence` DECIMAL(4, 3) NOT NULL DEFAULT 0.900 COMMENT '오인식 alias 신뢰도',
    `source` VARCHAR(50) NOT NULL DEFAULT 'manual' COMMENT 'manual/user/system/seed',
    `is_active` TINYINT(1) NOT NULL DEFAULT 1,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_medication_error_alias_norm` (`item_seq`, `normalized_error_text`),
    INDEX `idx_error_text` (`error_text`),
    INDEX `idx_normalized_error_text` (`normalized_error_text`),
    INDEX `idx_error_alias_item_seq` (`item_seq`),
    CONSTRAINT `fk_medication_error_alias_item_seq` FOREIGN KEY (`item_seq`) REFERENCES `medications_master` (`item_seq`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='OCR 오인식 alias 테이블';

-- 51. AI OCR 파이프라인 결과 로그
-- [FIX #1] 테이블명: medication_ocr_results (ocr_result_repository.py 기준)
CREATE TABLE IF NOT EXISTS `medication_ocr_results` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `request_id` VARCHAR(36) NOT NULL COMMENT 'UUID',
    `elderly_user_id` BIGINT NULL COMMENT '어르신 사용자 ID',
    `raw_ocr_text` TEXT NOT NULL COMMENT 'Luxia OCR 원문',
    `normalized_names` JSON NULL COMMENT '정규화된 약품명 후보',
    `candidates` JSON NULL COMMENT '매칭 후보 전체',
    `pipeline_stages` JSON NULL COMMENT '파이프라인 단계별 소요시간',
    `decision_status` VARCHAR(30) NOT NULL DEFAULT 'NOT_FOUND' COMMENT 'MATCHED, AMBIGUOUS, LOW_CONFIDENCE, NOT_FOUND, NEED_USER_CONFIRMATION',
    `match_confidence` DECIMAL(5, 3) NOT NULL DEFAULT 0.000 COMMENT '최종 confidence score 0~1',
    `decision_reasons` JSON NULL COMMENT '판정 사유',
    `best_drug_item_seq` VARCHAR(20) NULL COMMENT '최고 후보 item_seq',
    `best_drug_name` VARCHAR(200) NULL COMMENT '최고 후보 약품명',
    `user_confirmed` TINYINT(1) NULL COMMENT 'NULL=미확인, 1=확정, 0=거부',
    `user_selected_seq` VARCHAR(20) NULL COMMENT '사용자가 최종 선택한 item_seq',
    `user_confirmed_at` DATETIME NULL COMMENT '사용자 확인 시각',
    `llm_description` TEXT NULL COMMENT 'LLM 약 설명',
    `warnings` JSON NULL COMMENT '파이프라인 경고',
    `total_duration_ms` DECIMAL(10, 2) NULL COMMENT '총 파이프라인 소요시간 ms',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_request_id` (`request_id`),
    INDEX `idx_elderly_user_id` (`elderly_user_id`),
    INDEX `idx_decision_status` (`decision_status`),
    INDEX `idx_user_confirmed` (`user_confirmed`),
    INDEX `idx_created_at` (`created_at`),
    INDEX `idx_best_drug_item_seq` (`best_drug_item_seq`),
    CONSTRAINT `fk_ocr_results_elderly` FOREIGN KEY (`elderly_user_id`) REFERENCES `elderly` (`user_id`) ON DELETE SET NULL,
    CONSTRAINT `fk_ocr_results_best_item` FOREIGN KEY (`best_drug_item_seq`) REFERENCES `medications_master` (`item_seq`) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='AI OCR 파이프라인 결과 로그';

-- 52. AI OCR 후보 약품 로그
CREATE TABLE IF NOT EXISTS `medication_ocr_candidates` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `ocr_log_id` BIGINT NOT NULL COMMENT 'medication_ocr_results.id',
    `item_seq` VARCHAR(20) NULL COMMENT '후보 item_seq',
    `item_name` VARCHAR(200) NULL COMMENT '후보 약품명',
    `match_method` VARCHAR(100) NOT NULL COMMENT 'local_exact, local_alias, local_error_alias, local_ngram, local_fuzzy, mysql_fallback, vector_fallback',
    `match_score` DECIMAL(8, 5) NULL COMMENT '후보 match score',
    `rank_no` INT NOT NULL DEFAULT 0 COMMENT '후보 순위',
    `evidence_json` JSON NULL COMMENT '후보별 evidence',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_ocr_cand_log_id` (`ocr_log_id`),
    INDEX `idx_ocr_cand_item_seq` (`item_seq`),
    INDEX `idx_ocr_cand_method` (`match_method`),
    CONSTRAINT `fk_ocr_cand_log` FOREIGN KEY (`ocr_log_id`) REFERENCES `medication_ocr_results` (`id`) ON DELETE CASCADE,
    CONSTRAINT `fk_ocr_cand_item` FOREIGN KEY (`item_seq`) REFERENCES `medications_master` (`item_seq`) ON UPDATE CASCADE ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='AI OCR 후보 약품 로그';

-- 53. 사용자 확인 기반 alias 제안
-- [FIX #2] UNIQUE를 alias_normalized 기준으로 변경
-- [FIX #3] suggestion_type 추가
-- [FIX #4] is_active 기본 0 추가
-- [FIX #5] source_request_id 길이를 100으로 통일
CREATE TABLE IF NOT EXISTS `medication_alias_suggestions` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `item_seq` VARCHAR(20) NOT NULL COMMENT '품목기준코드',
    `alias_name` VARCHAR(200) NOT NULL COMMENT '제안된 별칭',
    `alias_normalized` VARCHAR(200) NOT NULL COMMENT '정규화된 별칭',
    `suggestion_type` VARCHAR(30) NOT NULL DEFAULT 'error_alias' COMMENT 'alias 또는 error_alias',
    `source` VARCHAR(50) NOT NULL DEFAULT 'user_feedback' COMMENT 'user_feedback/system/ocr_learning',
    `source_request_id` VARCHAR(100) NULL COMMENT '원본 OCR request_id',
    `review_status` VARCHAR(20) NOT NULL DEFAULT 'PENDING' COMMENT 'PENDING/APPROVED/REJECTED',
    `frequency` INT NOT NULL DEFAULT 1 COMMENT '동일 제안 횟수 (자동 증가)',
    `is_active` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '관리자 승인 전 기본 0',
    `reviewed_by` VARCHAR(100) NULL COMMENT '검토자',
    `reviewed_at` DATETIME NULL COMMENT '검토 시점',
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    UNIQUE KEY `uk_suggestion_norm` (`item_seq`, `alias_normalized`),
    INDEX `idx_review_status` (`review_status`),
    INDEX `idx_source_request_id` (`source_request_id`),
    INDEX `idx_frequency` (`frequency` DESC),
    INDEX `idx_suggestion_active` (`is_active`),
    CONSTRAINT `fk_suggestion_item_seq` FOREIGN KEY (`item_seq`) REFERENCES `medications_master` (`item_seq`) ON UPDATE CASCADE ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='사용자 확인 기반 alias 개선 후보';

-- 54. LocalDrugIndex 로딩 로그
CREATE TABLE IF NOT EXISTS `medication_dictionary_load_logs` (
    `id` BIGINT NOT NULL AUTO_INCREMENT,
    `worker_id` VARCHAR(100) NULL COMMENT 'FastAPI worker/process 식별자',
    `medication_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `alias_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `error_alias_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `ngram_token_count` INT UNSIGNED NOT NULL DEFAULT 0,
    `elapsed_ms` INT UNSIGNED NULL COMMENT '로딩 소요 시간 ms',
    `fuzzy_candidate_limit` INT UNSIGNED NOT NULL DEFAULT 50,
    `load_status` VARCHAR(50) NOT NULL DEFAULT 'SUCCESS' COMMENT 'SUCCESS, FAILED, FALLBACK',
    `error_message` TEXT NULL,
    `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (`id`),
    INDEX `idx_dict_load_status` (`load_status`),
    INDEX `idx_dict_load_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci
  COMMENT='LocalDrugIndex 로딩 로그';

-- ============================================================
-- 완료: FOREIGN KEY 체크 복원
-- ============================================================
SET FOREIGN_KEY_CHECKS = 1;
