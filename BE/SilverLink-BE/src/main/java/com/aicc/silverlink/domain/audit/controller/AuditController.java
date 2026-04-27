package com.aicc.silverlink.domain.audit.controller;

import com.aicc.silverlink.domain.audit.dto.AuditLogResponse;
import com.aicc.silverlink.domain.audit.entity.AuditLog;
import com.aicc.silverlink.domain.audit.service.AuditLogService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/audit-logs") // 관리자 전용 경로 명시
@RequiredArgsConstructor
@Tag(name = "감사 로그(Audit) API", description = "시스템 중요 행위 기록 조회 (관리자 전용)")
public class AuditController {

    private final AuditLogService auditLogService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')") // 🚨 관리자만 접근 가능 (이중 잠금)
    @Operation(summary = "전체 감사 로그 조회", description = "시스템의 모든 중요 행위 기록을 최신순으로 조회합니다.")
    public ResponseEntity<Page<AuditLogResponse>> getAuditLogs(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable
            ,@RequestParam(required = false) String action,
            @RequestParam(required = false) Long actorId
    ) {
        // 1. Service에서 Entity Page 조회
        Page<AuditLog> logPage = auditLogService.findAllLogs(pageable);

        // 2. Entity -> DTO 변환 (AuditLogResponse::from 메서드 사용)
        // 이 과정이 없으면 User 객체의 순환 참조로 인해 서버 에러 발생 가능
        Page<AuditLogResponse> responsePage = logPage.map(AuditLogResponse::from);

        return ResponseEntity.ok(responsePage);
    }
}