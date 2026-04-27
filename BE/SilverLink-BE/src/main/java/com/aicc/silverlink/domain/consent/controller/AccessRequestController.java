package com.aicc.silverlink.domain.consent.controller;

import com.aicc.silverlink.domain.consent.dto.AccessRequestDto.*;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope;
import com.aicc.silverlink.domain.consent.service.AccessRequestService;
import com.aicc.silverlink.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 민감정보 접근 권한 요청 API
 *
 * 보호자가 어르신의 민감정보(건강정보, 복약정보, 통화기록)를
 * 열람하기 위한 권한 요청/승인/거절/철회 API를 제공합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/access-requests")
@RequiredArgsConstructor
@Tag(name = "민감정보 접근 권한 API", description = "보호자의 어르신 민감정보 열람 권한 관리")
public class AccessRequestController {

    private final AccessRequestService accessRequestService;

    // ========== 보호자용 API ==========

    /**
     * 접근 권한 요청 생성
     * POST /api/access-requests
     */
    @PostMapping
    @PreAuthorize("hasAnyRole('GUARDIAN', 'COUNSELOR')")
    @Operation(summary = "접근 권한 요청", description = "보호자가 어르신의 민감정보 열람 권한을 요청합니다.")
    public ResponseEntity<AccessRequestResponse> createRequest(
            @Valid @RequestBody CreateRequest request) {
        Long guardianUserId = SecurityUtils.currentUserId();
        log.info("POST /api/access-requests - 접근 권한 요청 (guardianId: {})", guardianUserId);

        AccessRequestResponse response = accessRequestService.createAccessRequest(guardianUserId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    /**
     * 내 요청 목록 조회 (보호자)
     * GET /api/access-requests/my
     */
    @GetMapping("/my")
    @PreAuthorize("hasAnyRole('GUARDIAN', 'COUNSELOR')")
    @Operation(summary = "내 요청 목록 조회", description = "보호자가 자신이 신청한 접근 권한 요청 목록을 조회합니다.")
    public ResponseEntity<List<AccessRequestSummary>> getMyRequests() {
        Long guardianUserId = SecurityUtils.currentUserId();
        log.info("GET /api/access-requests/my - 내 요청 목록 (guardianId: {})", guardianUserId);

        List<AccessRequestSummary> responses = accessRequestService.getMyRequests(guardianUserId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 요청 취소 (보호자)
     * DELETE /api/access-requests/{requestId}
     */
    @DeleteMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('GUARDIAN', 'COUNSELOR')")
    @Operation(summary = "요청 취소", description = "보호자가 대기 중인 요청을 취소합니다.")
    public ResponseEntity<Void> cancelRequest(@PathVariable Long requestId) {
        Long guardianUserId = SecurityUtils.currentUserId();
        log.info("DELETE /api/access-requests/{} - 요청 취소 (guardianId: {})", requestId, guardianUserId);

        accessRequestService.cancelRequest(guardianUserId, requestId);
        return ResponseEntity.noContent().build();
    }

    // ========== 관리자용 API ==========

    /**
     * 대기 중인 요청 목록 조회 (관리자)
     * GET /api/access-requests/pending
     */
    @GetMapping("/pending")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "대기 중인 요청 목록", description = "관리자가 승인 대기 중인 요청 목록을 조회합니다.")
    public ResponseEntity<List<AccessRequestSummary>> getPendingRequests() {
        log.info("GET /api/access-requests/pending - 대기 중인 요청 목록");

        List<AccessRequestSummary> responses = accessRequestService.getPendingRequests();
        return ResponseEntity.ok(responses);
    }

    /**
     * 서류 확인 완료된 대기 요청 목록 (관리자)
     * GET /api/access-requests/pending/verified
     */
    @GetMapping("/pending/verified")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "서류 확인 완료된 대기 요청", description = "서류 확인이 완료되어 승인/거절 결정만 남은 요청 목록입니다.")
    public ResponseEntity<List<AccessRequestSummary>> getVerifiedPendingRequests() {
        log.info("GET /api/access-requests/pending/verified - 서류 확인 완료 대기 요청");

        List<AccessRequestSummary> responses = accessRequestService.getVerifiedPendingRequests();
        return ResponseEntity.ok(responses);
    }

    /**
     * 대기 중인 요청 통계 (관리자)
     * GET /api/access-requests/pending/stats
     */
    @GetMapping("/pending/stats")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "대기 요청 통계", description = "대기 중인 요청의 서류 확인 상태별 통계입니다.")
    public ResponseEntity<PendingRequestStats> getPendingStats() {
        log.info("GET /api/access-requests/pending/stats - 대기 요청 통계");

        PendingRequestStats stats = accessRequestService.getPendingStats();
        return ResponseEntity.ok(stats);
    }

    /**
     * 요청 상세 조회 (관리자)
     * GET /api/access-requests/{requestId}
     */
    @GetMapping("/{requestId}")
    @PreAuthorize("hasAnyRole('ADMIN', 'GUARDIAN', 'ELDERLY')")
    @Operation(summary = "요청 상세 조회", description = "접근 권한 요청의 상세 정보를 조회합니다.")
    public ResponseEntity<AccessRequestResponse> getRequestDetail(@PathVariable Long requestId) {
        log.info("GET /api/access-requests/{} - 요청 상세 조회", requestId);

        AccessRequestResponse response = accessRequestService.getRequestDetail(requestId);
        return ResponseEntity.ok(response);
    }

    /**
     * 서류 확인 완료 처리 (관리자)
     * POST /api/access-requests/{requestId}/verify-documents
     */
    @PostMapping("/{requestId}/verify-documents")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "서류 확인 완료", description = "관리자가 동의서와 가족관계증명서 확인을 완료했음을 표시합니다.")
    public ResponseEntity<AccessRequestResponse> verifyDocuments(@PathVariable Long requestId) {
        Long adminUserId = SecurityUtils.currentUserId();
        log.info("POST /api/access-requests/{}/verify-documents - 서류 확인 (adminId: {})", requestId, adminUserId);

        VerifyDocumentsRequest request = new VerifyDocumentsRequest(requestId);
        AccessRequestResponse response = accessRequestService.verifyDocuments(adminUserId, request);
        return ResponseEntity.ok(response);
    }

    /**
     * 접근 권한 승인 (관리자)
     * POST /api/access-requests/{requestId}/approve
     */
    @PostMapping("/{requestId}/approve")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "요청 승인", description = "서류 확인이 완료된 요청을 승인합니다. 승인 시 보호자는 해당 범위의 민감정보를 열람할 수 있습니다.")
    public ResponseEntity<AccessRequestResponse> approveRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) ApproveRequest request) {
        Long adminUserId = SecurityUtils.currentUserId();
        log.info("POST /api/access-requests/{}/approve - 요청 승인 (adminId: {})", requestId, adminUserId);

        ApproveRequest approveRequest = (request != null)
                ? new ApproveRequest(requestId, request.expiresAt(), request.note())
                : new ApproveRequest(requestId, null, null);

        AccessRequestResponse response = accessRequestService.approveRequest(adminUserId, approveRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 접근 권한 거절 (관리자)
     * POST /api/access-requests/{requestId}/reject
     */
    @PostMapping("/{requestId}/reject")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "요청 거절", description = "접근 권한 요청을 거절합니다. 거절 사유는 필수입니다.")
    public ResponseEntity<AccessRequestResponse> rejectRequest(
            @PathVariable Long requestId,
            @Valid @RequestBody RejectRequest request) {
        Long adminUserId = SecurityUtils.currentUserId();
        log.info("POST /api/access-requests/{}/reject - 요청 거절 (adminId: {})", requestId, adminUserId);

        RejectRequest rejectRequest = new RejectRequest(requestId, request.reason());
        AccessRequestResponse response = accessRequestService.rejectRequest(adminUserId, rejectRequest);
        return ResponseEntity.ok(response);
    }

    /**
     * 접근 권한 철회 (관리자)
     * POST /api/access-requests/{requestId}/revoke
     */
    @PostMapping("/{requestId}/revoke")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "권한 철회 (관리자)", description = "승인된 접근 권한을 철회합니다.")
    public ResponseEntity<AccessRequestResponse> revokeAccess(
            @PathVariable Long requestId,
            @RequestBody(required = false) RevokeRequest request) {
        Long adminUserId = SecurityUtils.currentUserId();
        log.info("POST /api/access-requests/{}/revoke - 권한 철회 (adminId: {})", requestId, adminUserId);

        RevokeRequest revokeRequest = (request != null)
                ? new RevokeRequest(requestId, request.reason())
                : new RevokeRequest(requestId, "관리자에 의한 철회");

        AccessRequestResponse response = accessRequestService.revokeAccess(adminUserId, revokeRequest);
        return ResponseEntity.ok(response);
    }

    // ========== 어르신용 API ==========

    /**
     * 나에 대한 접근 요청 목록 조회 (어르신)
     * GET /api/access-requests/for-me
     */
    @GetMapping("/for-me")
    @PreAuthorize("hasRole('ELDERLY')")
    @Operation(summary = "나에 대한 요청 목록", description = "어르신이 자신의 민감정보에 대한 접근 요청 목록을 조회합니다.")
    public ResponseEntity<List<AccessRequestSummary>> getRequestsForMe() {
        Long elderlyUserId = SecurityUtils.currentUserId();
        log.info("GET /api/access-requests/for-me - 나에 대한 요청 목록 (elderlyId: {})", elderlyUserId);

        List<AccessRequestSummary> responses = accessRequestService.getRequestsForElderly(elderlyUserId);
        return ResponseEntity.ok(responses);
    }

    /**
     * 접근 권한 철회 (어르신)
     * POST /api/access-requests/{requestId}/revoke-by-elderly
     */
    @PostMapping("/{requestId}/revoke-by-elderly")
    @PreAuthorize("hasRole('ELDERLY')")
    @Operation(summary = "권한 철회 (어르신)", description = "어르신이 승인된 접근 권한을 직접 철회합니다.")
    public ResponseEntity<AccessRequestResponse> revokeAccessByElderly(
            @PathVariable Long requestId,
            @RequestBody(required = false) RevokeRequest request) {
        Long elderlyUserId = SecurityUtils.currentUserId();
        log.info("POST /api/access-requests/{}/revoke-by-elderly - 권한 철회 (elderlyId: {})", requestId, elderlyUserId);

        RevokeRequest revokeRequest = (request != null)
                ? new RevokeRequest(requestId, request.reason())
                : new RevokeRequest(requestId, null);

        AccessRequestResponse response = accessRequestService.revokeAccessByElderly(elderlyUserId, revokeRequest);
        return ResponseEntity.ok(response);
    }

    // ========== 권한 확인 API ==========

    /**
     * 접근 권한 확인
     * GET /api/access-requests/check?elderlyUserId={id}&scope={scope}
     */
    @GetMapping("/check")
    @PreAuthorize("hasAnyRole('GUARDIAN', 'COUNSELOR', 'ADMIN')")
    @Operation(summary = "접근 권한 확인", description = "특정 어르신의 특정 범위 민감정보에 대한 접근 권한을 확인합니다.")
    public ResponseEntity<AccessCheckResult> checkAccess(
            @RequestParam Long elderlyUserId,
            @RequestParam AccessScope scope) {
        Long requesterId = SecurityUtils.currentUserId();
        log.info("GET /api/access-requests/check - 권한 확인 (requesterId: {}, elderlyId: {}, scope: {})",
                requesterId, elderlyUserId, scope);

        AccessCheckResult result = accessRequestService.checkAccess(requesterId, elderlyUserId, scope);
        return ResponseEntity.ok(result);
    }
}