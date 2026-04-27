package com.aicc.silverlink.domain.elderly.controller;

import com.aicc.silverlink.domain.elderly.dto.ScheduleChangeRequestDto.*;
import com.aicc.silverlink.domain.elderly.service.ScheduleChangeRequestService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import com.aicc.silverlink.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 통화 스케줄 변경 요청 API
 */
@Tag(name = "스케줄 변경 요청", description = "어르신 통화 스케줄 변경 요청 관리 API")
@RestController
@RequestMapping("/api/schedule-change-requests")
@RequiredArgsConstructor
public class ScheduleChangeRequestController {

    private final ScheduleChangeRequestService changeRequestService;

    // ===== 어르신용 API =====

    @Operation(summary = "변경 요청 생성", description = "어르신이 통화 스케줄 변경을 요청합니다")
    @PreAuthorize("hasRole('ELDERLY')")
    @PostMapping
    public ResponseEntity<ApiResponse<Response>> createRequest(
            @Valid @RequestBody CreateRequest request) {

        Response response = changeRequestService.createRequest(SecurityUtils.currentUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "내 변경 요청 목록", description = "어르신이 본인의 변경 요청 내역을 조회합니다")
    @PreAuthorize("hasRole('ELDERLY')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<List<Response>>> getMyRequests() {

        List<Response> requests = changeRequestService.getMyRequests(SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    // ===== 상담사용 API =====

    @Operation(summary = "대기 중 변경 요청 목록", description = "상담사가 처리 대기 중인 변경 요청을 조회합니다")
    @PreAuthorize("hasRole('COUNSELOR')")
    @GetMapping("/pending")
    public ResponseEntity<ApiResponse<List<Response>>> getPendingRequests() {

        List<Response> requests = changeRequestService.getPendingRequests();
        return ResponseEntity.ok(ApiResponse.success(requests));
    }

    @Operation(summary = "변경 요청 승인", description = "상담사가 변경 요청을 승인합니다")
    @PreAuthorize("hasRole('COUNSELOR')")
    @PutMapping("/{requestId}/approve")
    public ResponseEntity<ApiResponse<Response>> approveRequest(
            @PathVariable Long requestId) {

        Response response = changeRequestService.approveRequest(requestId, SecurityUtils.currentUserId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "변경 요청 거절", description = "상담사가 변경 요청을 거절합니다")
    @PreAuthorize("hasRole('COUNSELOR')")
    @PutMapping("/{requestId}/reject")
    public ResponseEntity<ApiResponse<Response>> rejectRequest(
            @PathVariable Long requestId,
            @RequestBody(required = false) RejectRequest rejectRequest) {

        Response response = changeRequestService.rejectRequest(
                requestId,
                SecurityUtils.currentUserId(),
                rejectRequest != null ? rejectRequest : new RejectRequest());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
