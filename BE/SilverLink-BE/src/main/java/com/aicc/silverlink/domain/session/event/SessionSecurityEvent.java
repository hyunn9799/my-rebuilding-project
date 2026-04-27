package com.aicc.silverlink.domain.session.event;

import com.aicc.silverlink.domain.session.dto.DeviceInfo;

import java.time.Instant;

/**
 * 세션 보안 이벤트.
 * Spring ApplicationEvent로 발행되어 감사(Audit) 로그에 기록됩니다.
 */
public record SessionSecurityEvent(
        EventType type,
        Long userId,
        String sessionId,
        DeviceInfo deviceInfo,
        Instant timestamp) {
    public enum EventType {
        /** 새 세션 생성 */
        SESSION_CREATED,
        /** 세션 강제 무효화 (다른 기기에서 로그인) */
        SESSION_INVALIDATED,
        /** 중복 로그인 감지 */
        DUPLICATE_LOGIN_DETECTED,
        /** 사용자 확인 후 강제 로그인 실행 */
        FORCE_LOGIN_EXECUTED,
        /** 세션 만료 */
        SESSION_EXPIRED
    }

    /**
     * 팩토리 메서드: 이벤트 생성
     */
    public static SessionSecurityEvent of(EventType type, Long userId, String sessionId, DeviceInfo deviceInfo) {
        return new SessionSecurityEvent(type, userId, sessionId, deviceInfo, Instant.now());
    }
}
