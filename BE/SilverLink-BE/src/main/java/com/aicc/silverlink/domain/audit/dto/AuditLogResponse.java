package com.aicc.silverlink.domain.audit.dto;

import com.aicc.silverlink.domain.audit.entity.AuditLog;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
public class AuditLogResponse {

    private Long id;
    private String action;      // 행위 (예: CREATE_POLICY)
    private String targetEntity; // 대상 (예: Policy)
    private Long targetId;      // 대상 ID

    // User 객체 통째로 안 주고, 필요한 것만 쏙!
    private Long actorId;
    private String actorName;   // "홍길동" (없으면 "시스템" or "알수없음")

    private String clientIp;
    private LocalDateTime timestamp; // 언제

    // Entity -> DTO 변환 메서드
    public static AuditLogResponse from(AuditLog log) {
        return AuditLogResponse.builder()
                .id(log.getId())
                .action(log.getAction())
                .targetEntity(log.getTargetEntity())
                .targetId(log.getTargetId())
                // User가 null일 수도 있음 (시스템 작업 등) -> null 체크 필수!
                .actorId(log.getActor() != null ? log.getActor().getId() : null)
                .actorName(log.getActor() != null ? log.getActor().getName() : "Unknown/System")
                .clientIp(log.getIpAddress())
                .timestamp(log.getCreatedAt())
                .build();
    }
}