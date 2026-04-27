package com.aicc.silverlink.domain.call.controller;

import com.aicc.silverlink.domain.call.dto.CallDetailDto.*;
import com.aicc.silverlink.domain.call.service.CallDetailService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import com.aicc.silverlink.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "통화 상세", description = "CallBot 통화 상세 조회 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-details")
public class CallDetailController {

    private final CallDetailService callDetailService;

    // ===== 상담사용 API =====

    @Operation(summary = "상담사 - 통화 상세 조회", description = "담당 어르신의 통화 상세 정보를 조회합니다")
    @PreAuthorize("hasRole('COUNSELOR')")
    @GetMapping("/counselor/calls/{callId}")
    public ResponseEntity<ApiResponse<CallDetailResponse>> getCallDetailForCounselor(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "통화 ID") @PathVariable Long callId) {

        Long userId = principal.getUserId();
        CallDetailResponse response = callDetailService.getCallDetailForCounselor(userId, callId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "상담사 - 대화 내용만 조회")
    @PreAuthorize("hasRole('COUNSELOR')")
    @GetMapping("/counselor/calls/{callId}/conversations")
    public ResponseEntity<ApiResponse<List<ConversationMessage>>> getConversationsForCounselor(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long callId) {

        Long userId = principal.getUserId();
        callDetailService.getCallDetailForCounselor(userId, callId); // 권한 검증용

        List<ConversationMessage> conversations = callDetailService.getConversations(callId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @Operation(summary = "상담사 - 오늘의 상태만 조회")
    @PreAuthorize("hasRole('COUNSELOR')")
    @GetMapping("/counselor/calls/{callId}/daily-status")
    public ResponseEntity<ApiResponse<DailyStatusResponse>> getDailyStatusForCounselor(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long callId) {

        Long userId = principal.getUserId();
        callDetailService.getCallDetailForCounselor(userId, callId); // 권한 검증용

        DailyStatusResponse dailyStatus = callDetailService.getDailyStatus(callId);
        return ResponseEntity.ok(ApiResponse.success(dailyStatus));
    }

    // ===== 보호자용 API =====

    @Operation(summary = "보호자 - 통화 상세 조회", description = "보호 관계인 어르신의 통화 상세 정보를 조회합니다")
    @PreAuthorize("hasRole('GUARDIAN')")
    @GetMapping("/guardian/elderly/{elderlyId}/calls/{callId}")
    public ResponseEntity<ApiResponse<CallDetailResponse>> getCallDetailForGuardian(
            @AuthenticationPrincipal UserPrincipal principal,
            @Parameter(description = "어르신 ID") @PathVariable Long elderlyId,
            @Parameter(description = "통화 ID") @PathVariable Long callId) {

        Long userId = principal.getUserId();
        CallDetailResponse response = callDetailService.getCallDetailForGuardian(userId, callId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "보호자 - 대화 내용만 조회")
    @PreAuthorize("hasRole('GUARDIAN')")
    @GetMapping("/guardian/elderly/{elderlyId}/calls/{callId}/conversations")
    public ResponseEntity<ApiResponse<List<ConversationMessage>>> getConversationsForGuardian(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long elderlyId,
            @PathVariable Long callId) {

        Long userId = principal.getUserId();
        callDetailService.getCallDetailForGuardian(userId, callId); // 권한 검증용

        List<ConversationMessage> conversations = callDetailService.getConversations(callId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @Operation(summary = "보호자 - 오늘의 상태만 조회")
    @PreAuthorize("hasRole('GUARDIAN')")
    @GetMapping("/guardian/elderly/{elderlyId}/calls/{callId}/daily-status")
    public ResponseEntity<ApiResponse<DailyStatusResponse>> getDailyStatusForGuardian(
            @AuthenticationPrincipal UserPrincipal principal,
            @PathVariable Long elderlyId,
            @PathVariable Long callId) {

        Long userId = principal.getUserId();
        callDetailService.getCallDetailForGuardian(userId, callId); // 권한 검증용

        DailyStatusResponse dailyStatus = callDetailService.getDailyStatus(callId);
        return ResponseEntity.ok(ApiResponse.success(dailyStatus));
    }

    // ===== 관리자용 API =====

    @Operation(summary = "관리자 - 통화 상세 조회", description = "모든 통화의 상세 정보를 조회합니다")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/calls/{callId}")
    public ResponseEntity<ApiResponse<CallDetailResponse>> getCallDetailForAdmin(
            @Parameter(description = "통화 ID") @PathVariable Long callId) {

        CallDetailResponse response = callDetailService.getCallDetailForAdmin(callId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @Operation(summary = "관리자 - 대화 내용만 조회")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/calls/{callId}/conversations")
    public ResponseEntity<ApiResponse<List<ConversationMessage>>> getConversationsForAdmin(
            @PathVariable Long callId) {

        List<ConversationMessage> conversations = callDetailService.getConversations(callId);
        return ResponseEntity.ok(ApiResponse.success(conversations));
    }

    @Operation(summary = "관리자 - 오늘의 상태만 조회")
    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/calls/{callId}/daily-status")
    public ResponseEntity<ApiResponse<DailyStatusResponse>> getDailyStatusForAdmin(
            @PathVariable Long callId) {

        DailyStatusResponse dailyStatus = callDetailService.getDailyStatus(callId);
        return ResponseEntity.ok(ApiResponse.success(dailyStatus));
    }
}