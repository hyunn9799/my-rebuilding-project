package com.aicc.silverlink.domain.emergency.controller;

import com.aicc.silverlink.domain.emergency.dto.EmergencyAlertDto.*;
import com.aicc.silverlink.domain.emergency.service.EmergencyAlertService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import com.aicc.silverlink.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 긴급 알림 API 컨트롤러
 * 상담사, 관리자, 보호자용 긴급 알림 조회 및 처리
 */
@Slf4j
@RestController
@RequestMapping("/api/emergency-alerts")
@RequiredArgsConstructor
@Tag(name = "긴급 알림", description = "긴급 알림 조회 및 처리 API")
public class EmergencyAlertController {

    private final EmergencyAlertService alertService;

    // ========== 공통 API ==========

    @GetMapping("/{alertId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'GUARDIAN')")
    @Operation(summary = "긴급 알림 상세 조회", description = "긴급 알림의 상세 정보를 조회합니다.")
    public ResponseEntity<ApiResponse<DetailResponse>> getAlertDetail(
            @Parameter(description = "알림 ID") @PathVariable Long alertId) {

        Long userId = SecurityUtils.currentUserId();
        log.info("GET /api/emergency-alerts/{} - 상세 조회 (userId: {})", alertId, userId);

        DetailResponse response = alertService.getAlertDetail(alertId, userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'GUARDIAN')")
    @Operation(summary = "미확인 알림 목록", description = "현재 사용자의 미확인 긴급 알림 목록을 조회합니다. (실시간 표시용)")
    public ResponseEntity<ApiResponse<List<RecipientAlertResponse>>> getUnreadAlerts() {
        Long userId = SecurityUtils.currentUserId();
        log.info("GET /api/emergency-alerts/unread - 미확인 알림 목록 (userId: {})", userId);

        List<RecipientAlertResponse> alerts = alertService.getUnreadAlertsForUser(userId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'GUARDIAN')")
    @Operation(summary = "미확인 알림 수", description = "현재 사용자의 미확인 긴급 알림 수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        Long userId = SecurityUtils.currentUserId();
        long count = alertService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @PostMapping("/{alertId}/read")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'GUARDIAN')")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @Parameter(description = "알림 ID") @PathVariable Long alertId) {

        Long userId = SecurityUtils.currentUserId();
        log.info("POST /api/emergency-alerts/{}/read - 읽음 처리 (userId: {})", alertId, userId);

        alertService.markAsRead(alertId, userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/read-all")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR', 'GUARDIAN')")
    @Operation(summary = "모든 알림 읽음 처리", description = "현재 사용자의 모든 미확인 알림을 읽음 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long userId = SecurityUtils.currentUserId();
        log.info("POST /api/emergency-alerts/read-all - 전체 읽음 처리 (userId: {})", userId);

        alertService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ========== 상담사용 API ==========

    @GetMapping("/counselor")
    @PreAuthorize("hasRole('COUNSELOR')")
    @Operation(summary = "상담사용 알림 목록", description = "담당 어르신의 긴급 알림 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<SummaryResponse>>> getAlertsForCounselor(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Long counselorId = SecurityUtils.currentUserId();
        log.info("GET /api/emergency-alerts/counselor - 상담사용 목록 (counselorId: {})", counselorId);

        Page<SummaryResponse> alerts = alertService.getAlertsForCounselor(counselorId, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/counselor/pending")
    @PreAuthorize("hasRole('COUNSELOR')")
    @Operation(summary = "상담사용 미처리 알림", description = "담당 어르신의 미처리 긴급 알림 목록을 조회합니다. (화면 상단 표시용)")
    public ResponseEntity<ApiResponse<List<SummaryResponse>>> getPendingAlertsForCounselor() {
        Long counselorId = SecurityUtils.currentUserId();
        log.info("GET /api/emergency-alerts/counselor/pending - 상담사용 미처리 목록 (counselorId: {})", counselorId);

        List<SummaryResponse> alerts = alertService.getPendingAlertsForCounselor(counselorId);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/counselor/stats")
    @PreAuthorize("hasRole('COUNSELOR')")
    @Operation(summary = "상담사용 통계", description = "담당 어르신의 긴급 알림 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<StatsResponse>> getStatsForCounselor() {
        Long counselorId = SecurityUtils.currentUserId();
        StatsResponse stats = alertService.getStatsForCounselor(counselorId);
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ========== 관리자용 API ==========

    @GetMapping("/admin")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자용 알림 목록", description = "전체 긴급 알림 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<SummaryResponse>>> getAlertsForAdmin(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Long adminId = SecurityUtils.currentUserId();
        log.info("GET /api/emergency-alerts/admin - 관리자용 목록 (adminId: {})", adminId);

        Page<SummaryResponse> alerts = alertService.getAlertsForAdmin(pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    @GetMapping("/admin/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "관리자용 통계", description = "전체 긴급 알림 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<StatsResponse>> getStatsForAdmin() {
        StatsResponse stats = alertService.getStats();
        return ResponseEntity.ok(ApiResponse.success(stats));
    }

    // ========== 보호자용 API ==========

    @GetMapping("/guardian")
    @PreAuthorize("hasRole('GUARDIAN')")
    @Operation(summary = "보호자용 알림 목록", description = "담당 어르신의 긴급 알림 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<Page<SummaryResponse>>> getAlertsForGuardian(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC)
            Pageable pageable) {

        Long guardianId = SecurityUtils.currentUserId();
        log.info("GET /api/emergency-alerts/guardian - 보호자용 목록 (guardianId: {})", guardianId);

        Page<SummaryResponse> alerts = alertService.getAlertsForGuardian(guardianId, pageable);
        return ResponseEntity.ok(ApiResponse.success(alerts));
    }

    // ========== 알림 처리 API ==========

    @PostMapping("/{alertId}/process")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR')")
    @Operation(summary = "알림 처리", description = "긴급 알림을 처리 완료 또는 상위 보고합니다.")
    public ResponseEntity<ApiResponse<DetailResponse>> processAlert(
            @Parameter(description = "알림 ID") @PathVariable Long alertId,
            @Valid @RequestBody ProcessRequest request) {

        Long userId = SecurityUtils.currentUserId();
        log.info("POST /api/emergency-alerts/{}/process - 알림 처리 (userId: {}, status: {})",
                alertId, userId, request.getStatus());

        DetailResponse response = alertService.processAlert(alertId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/{alertId}/start")
    @PreAuthorize("hasAnyRole('ADMIN', 'COUNSELOR')")
    @Operation(summary = "처리 시작", description = "긴급 알림 처리를 시작합니다.")
    public ResponseEntity<ApiResponse<DetailResponse>> startProcessing(
            @Parameter(description = "알림 ID") @PathVariable Long alertId) {

        Long userId = SecurityUtils.currentUserId();
        log.info("POST /api/emergency-alerts/{}/start - 처리 시작 (userId: {})", alertId, userId);

        ProcessRequest request = new ProcessRequest();
        request.setStatus(com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.AlertStatus.IN_PROGRESS);

        DetailResponse response = alertService.processAlert(alertId, userId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
