package com.aicc.silverlink.domain.session.event;

import com.aicc.silverlink.domain.session.entity.AuditLog;
import com.aicc.silverlink.domain.session.repository.SessionAuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

/**
 * 세션 보안 이벤트 리스너.
 * 구조화된 보안 감사(Audit) 로그를 출력하고 DB에 영속화합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SessionEventListener {

    private final SessionAuditLogRepository sessionAuditLogRepository;

    @Async
    @EventListener
    public void handleSessionSecurityEvent(SessionSecurityEvent event) {
        String ip = event.deviceInfo() != null ? event.deviceInfo().ipAddress() : "unknown";
        String ua = event.deviceInfo() != null ? event.deviceInfo().deviceSummary() : "unknown";
        String deviceId = event.deviceInfo() != null ? event.deviceInfo().deviceId() : "unknown";

        // 1. 구조화된 로그 출력 (기존 유지)
        log.info("[SECURITY_AUDIT] type={} userId={} sessionId={} ip={} device={} deviceId={} timestamp={}",
                event.type(),
                event.userId(),
                event.sessionId(),
                ip,
                ua,
                deviceId,
                event.timestamp());

        // 2. DB 영속화 (신규)
        try {
            AuditLog auditLog = AuditLog.from(event);
            sessionAuditLogRepository.save(auditLog);
        } catch (Exception e) {
            log.warn("[SECURITY_AUDIT] 감사 로그 DB 저장 실패 (로그 출력은 완료): {}", e.getMessage());
        }

        // 3. 중복 로그인 감지 시 추가 경고 로그
        if (event.type() == SessionSecurityEvent.EventType.DUPLICATE_LOGIN_DETECTED) {
            log.warn("[SECURITY_WARN] 중복 로그인 감지! userId={} 새 기기에서 로그인 시도 ip={} device={}",
                    event.userId(), ip, ua);
        }

        // 4. 강제 로그인 실행 시 추가 정보 로그
        if (event.type() == SessionSecurityEvent.EventType.FORCE_LOGIN_EXECUTED) {
            log.info("[SECURITY_INFO] 강제 로그인 실행: userId={} 기존 세션 종료 후 새 세션 생성 sessionId={}",
                    event.userId(), event.sessionId());
        }
    }
}
