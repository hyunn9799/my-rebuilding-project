package com.aicc.silverlink.domain.session.event;

import com.aicc.silverlink.domain.session.dto.DeviceInfo;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;

/**
 * 세션 보안 이벤트 퍼블리셔.
 * ApplicationEventPublisher를 래핑하여 타입별 publish 메서드를 제공합니다.
 */
@Component
@RequiredArgsConstructor
public class SessionEventPublisher {

    private final ApplicationEventPublisher eventPublisher;

    /** 세션 생성 이벤트 */
    public void publishSessionCreated(Long userId, String sessionId, DeviceInfo deviceInfo) {
        eventPublisher.publishEvent(
                SessionSecurityEvent.of(SessionSecurityEvent.EventType.SESSION_CREATED, userId, sessionId, deviceInfo));
    }

    /** 세션 무효화 이벤트 */
    public void publishSessionInvalidated(Long userId, String sessionId, DeviceInfo deviceInfo) {
        eventPublisher.publishEvent(
                SessionSecurityEvent.of(SessionSecurityEvent.EventType.SESSION_INVALIDATED, userId, sessionId,
                        deviceInfo));
    }

    /** 중복 로그인 감지 이벤트 */
    public void publishDuplicateLoginDetected(Long userId, String existingSessionId, DeviceInfo newDeviceInfo) {
        eventPublisher.publishEvent(
                SessionSecurityEvent.of(SessionSecurityEvent.EventType.DUPLICATE_LOGIN_DETECTED, userId,
                        existingSessionId, newDeviceInfo));
    }

    /** 강제 로그인 실행 이벤트 */
    public void publishForceLoginExecuted(Long userId, String newSessionId, DeviceInfo deviceInfo) {
        eventPublisher.publishEvent(
                SessionSecurityEvent.of(SessionSecurityEvent.EventType.FORCE_LOGIN_EXECUTED, userId, newSessionId,
                        deviceInfo));
    }
}
