package com.aicc.silverlink.domain.session.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

/**
 * 세션 보안 감사 로그 엔티티.
 * SessionSecurityEvent를 DB에 영속화하여 컴플라이언스 대응 및 보안 분석에 활용합니다.
 */
@Entity(name = "SessionAuditLog")
@Table(name = "audit_log", indexes = {
        @Index(name = "idx_audit_user_id", columnList = "userId"),
        @Index(name = "idx_audit_event_type", columnList = "eventType"),
        @Index(name = "idx_audit_created_at", columnList = "createdAt")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50)
    private String eventType;

    @Column(nullable = false)
    private Long userId;

    @Column(length = 64)
    private String sessionId;

    @Column(length = 45)
    private String ipAddress;

    @Column(length = 500)
    private String userAgent;

    @Column(length = 64)
    private String deviceId;

    @Column(nullable = false)
    private Instant createdAt;

    @Column(length = 500)
    private String detail;

    /**
     * SessionSecurityEvent로부터 AuditLog를 생성합니다.
     */
    public static AuditLog from(com.aicc.silverlink.domain.session.event.SessionSecurityEvent event) {
        return AuditLog.builder()
                .eventType(event.type().name())
                .userId(event.userId())
                .sessionId(event.sessionId())
                .ipAddress(event.deviceInfo() != null ? event.deviceInfo().ipAddress() : null)
                .userAgent(event.deviceInfo() != null ? event.deviceInfo().userAgent() : null)
                .deviceId(event.deviceInfo() != null ? event.deviceInfo().deviceId() : null)
                .createdAt(event.timestamp())
                .build();
    }

    /**
     * 세션 만료 이벤트 로그를 생성합니다.
     */
    public static AuditLog expired(String sid) {
        return AuditLog.builder()
                .eventType("SESSION_EXPIRED")
                .userId(0L)
                .sessionId(sid)
                .createdAt(Instant.now())
                .detail("Redis TTL에 의한 자동 만료")
                .build();
    }
}
