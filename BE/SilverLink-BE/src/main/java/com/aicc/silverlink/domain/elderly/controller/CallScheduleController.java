package com.aicc.silverlink.domain.elderly.controller;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.*;
import com.aicc.silverlink.domain.elderly.service.CallScheduleService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import com.aicc.silverlink.global.security.principal.UserPrincipal;
import com.aicc.silverlink.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 통화 스케줄 관리 API
 */
@Tag(name = "통화 스케줄", description = "어르신 통화 스케줄 관리 API")
@RestController
@RequestMapping("/api/call-schedules")
@RequiredArgsConstructor
public class CallScheduleController {

    private final CallScheduleService callScheduleService;

    // ===== 어르신용 API =====

    @Operation(summary = "본인 통화 스케줄 조회", description = "어르신이 본인의 통화 스케줄을 조회합니다")
    @PreAuthorize("hasRole('ELDERLY')")
    @GetMapping("/me")
    public ResponseEntity<ApiResponse<Response>> getMySchedule() {

        Long elderlyId = SecurityUtils.currentUserId();
        Response response = callScheduleService.getSchedule(elderlyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "본인 통화 스케줄 수정", description = "어르신이 본인의 통화 스케줄을 설정합니다")
    @PreAuthorize("hasRole('ELDERLY')")
    @PutMapping("/me")
    public ResponseEntity<ApiResponse<Response>> updateMySchedule(
            @Valid @RequestBody UpdateRequest request) {

        Long elderlyId = SecurityUtils.currentUserId();
        Response response = callScheduleService.updateSchedule(elderlyId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ===== 상담사용 API =====

    @Operation(summary = "담당 어르신 스케줄 조회", description = "상담사가 담당하는 특정 어르신의 통화 스케줄을 조회합니다")
    @PreAuthorize("hasRole('COUNSELOR')")
    @GetMapping("/counselor/elderly/{elderlyId}")
    public ResponseEntity<ApiResponse<Response>> getCounselorElderlySchedule(
            @PathVariable Long elderlyId) {

        Response response = callScheduleService.getSchedule(elderlyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "담당 어르신 스케줄 직접 수정", description = "상담사가 어르신의 구두 요청 등을 받아 직접 스케줄을 수정합니다")
    @PreAuthorize("hasRole('COUNSELOR')")
    @PutMapping("/counselor/elderly/{elderlyId}")
    public ResponseEntity<ApiResponse<Response>> directUpdateByCounselor(
            @PathVariable Long elderlyId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DirectUpdateRequest request) {

        Response response = callScheduleService.directUpdateSchedule(elderlyId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "담당 어르신 변경 이력", description = "상담사가 담당하는 어르신들의 스케줄 변경 이력을 조회합니다")
    @PreAuthorize("hasRole('COUNSELOR')")
    @GetMapping("/counselor/history")
    public ResponseEntity<ApiResponse<List<HistoryResponse>>> getCounselorHistory(
            @AuthenticationPrincipal UserPrincipal principal) {

        List<HistoryResponse> history = callScheduleService.getHistoryByCounselor(principal.getUserId());
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    // ===== 관리자용 API =====

    @Operation(summary = "특정 어르신 통화 스케줄 조회", description = "관리자가 특정 어르신의 통화 스케줄을 조회합니다")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/elderly/{elderlyId}")
    public ResponseEntity<ApiResponse<Response>> getSchedule(
            @PathVariable Long elderlyId) {

        Response response = callScheduleService.getSchedule(elderlyId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "특정 어르신 통화 스케줄 직접 수정", description = "관리자가 특정 어르신의 통화 스케줄을 직접 수정합니다")
    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/elderly/{elderlyId}")
    public ResponseEntity<ApiResponse<Response>> directUpdateByAdmin(
            @PathVariable Long elderlyId,
            @AuthenticationPrincipal UserPrincipal principal,
            @Valid @RequestBody DirectUpdateRequest request) {

        Response response = callScheduleService.directUpdateSchedule(elderlyId, principal.getUserId(), request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "특정 어르신 변경 이력", description = "관리자가 특정 어르신의 스케줄 변경 이력을 조회합니다")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/elderly/{elderlyId}/history")
    public ResponseEntity<ApiResponse<List<HistoryResponse>>> getElderlyHistory(
            @PathVariable Long elderlyId) {

        List<HistoryResponse> history = callScheduleService.getHistoryByElderly(elderlyId);
        return ResponseEntity.ok(ApiResponse.success(history));
    }

    @Operation(summary = "전체 통화 스케줄 목록", description = "관리자가 활성화된 모든 통화 스케줄을 조회합니다")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping
    public ResponseEntity<ApiResponse<List<Response>>> getAllSchedules() {

        List<Response> schedules = callScheduleService.getAllSchedules();
        return ResponseEntity.ok(ApiResponse.success(schedules));
    }

    @Operation(summary = "전체 변경 이력", description = "관리자가 모든 어르신의 스케줄 변경 이력을 조회합니다")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/history")
    public ResponseEntity<ApiResponse<List<HistoryResponse>>> getAllHistory() {

        List<HistoryResponse> history = callScheduleService.getAllHistory();
        return ResponseEntity.ok(ApiResponse.success(history));
    }
}
