package com.aicc.silverlink.domain.session.controller;

import com.aicc.silverlink.domain.session.entity.AuditLog;
import com.aicc.silverlink.domain.session.repository.SessionAuditLogRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Tag(name = "세션 관리", description = "세션 및 보안 감사 로그 관리 API")
@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionController {

    private final SessionAuditLogRepository sessionAuditLogRepository;

    @Operation(summary = "사용자 감사 로그 조회")
    @GetMapping("/audit/{userId}")
    public ResponseEntity<List<AuditLog>> getUserAuditLogs(@PathVariable Long userId) {
        return ResponseEntity.ok(sessionAuditLogRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @Operation(summary = "이벤트 타입별 감사 로그 조회")
    @GetMapping("/audit")
    public ResponseEntity<Page<AuditLog>> getAuditLogsByType(
            @RequestParam String eventType, Pageable pageable) {
        return ResponseEntity.ok(sessionAuditLogRepository.findByEventTypeOrderByCreatedAtDesc(eventType, pageable));
    }

    @Operation(summary = "보안 통계 요약")
    @GetMapping("/audit/stats")
    public ResponseEntity<Map<String, Long>> getSecurityStats() {
        java.time.Instant last24h = java.time.Instant.now().minusSeconds(86400);
        return ResponseEntity.ok(Map.of(
                "duplicateLogins",
                sessionAuditLogRepository.countByEventTypeAndCreatedAtAfter("DUPLICATE_LOGIN_DETECTED", last24h),
                "forceLogins", sessionAuditLogRepository.countByEventTypeAndCreatedAtAfter("FORCE_LOGIN_EXECUTED", last24h),
                "sessionCreated", sessionAuditLogRepository.countByEventTypeAndCreatedAtAfter("SESSION_CREATED", last24h),
                "sessionInvalidated",
                sessionAuditLogRepository.countByEventTypeAndCreatedAtAfter("SESSION_INVALIDATED", last24h)));
    }
}
