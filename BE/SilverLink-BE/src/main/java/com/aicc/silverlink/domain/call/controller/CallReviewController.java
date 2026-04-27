package com.aicc.silverlink.domain.call.controller;

import com.aicc.silverlink.domain.call.dto.CallReviewDto.*;
import com.aicc.silverlink.domain.call.service.CallReviewService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import com.aicc.silverlink.global.common.response.PageResponse;
import com.aicc.silverlink.global.security.principal.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * 상담사 통화 리뷰 API
 * - 상담사: CallBot 통화 내용 확인 및 코멘트 작성
 * - 보호자: 통화 내용 및 상담사 코멘트 조회
 */
@Tag(name = "Call Review", description = "CallBot 통화 리뷰 API")
@RestController
@RequiredArgsConstructor
@RequestMapping("/api/call-reviews")
public class CallReviewController {

        private final CallReviewService callReviewService;

        // ===== 상담사용 API =====

        @Operation(summary = "담당 어르신 통화 목록 조회", description = "상담사가 담당하는 어르신들의 CallBot 통화 기록 목록을 조회합니다.")
        @GetMapping("/counselor/calls")
        @PreAuthorize("hasRole('COUNSELOR')")
        public ResponseEntity<ApiResponse<PageResponse<CallRecordSummaryResponse>>> getCallRecordsForCounselor(
                        @AuthenticationPrincipal Long userId,
                        @PageableDefault(size = 20, sort = "callAt", direction = Sort.Direction.DESC) Pageable pageable) {

                Page<CallRecordSummaryResponse> page = callReviewService.getCallRecordsForCounselor(
                                userId, pageable);

                return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
        }

        @Operation(summary = "통화 상세 조회", description = "특정 통화 기록의 상세 내용을 조회합니다. (대화 내용, 요약, 감정 분석, 리뷰 포함)")
        @GetMapping("/counselor/calls/{callId}")
        @PreAuthorize("hasRole('COUNSELOR')")
        public ResponseEntity<ApiResponse<CallRecordDetailResponse>> getCallRecordDetail(
                        @AuthenticationPrincipal Long userId,
                        @Parameter(description = "통화 기록 ID") @PathVariable Long callId) {

                CallRecordDetailResponse response = callReviewService.getCallRecordDetail(
                                callId, userId);

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        @Operation(summary = "통화 리뷰 작성", description = "상담사가 통화를 확인했음을 체크하고 보호자에게 전달할 코멘트를 작성합니다.")
        @PostMapping("/counselor/reviews")
        @PreAuthorize("hasRole('COUNSELOR')")
        public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
                        @AuthenticationPrincipal Long userId,
                        @Valid @RequestBody ReviewRequest request) {

                ReviewResponse response = callReviewService.createReview(userId, request);

                return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(response));
        }

        @Operation(summary = "통화 리뷰 수정", description = "기존에 작성한 통화 리뷰의 코멘트를 수정합니다.")
        @PutMapping("/counselor/reviews/{reviewId}")
        @PreAuthorize("hasRole('COUNSELOR')")
        public ResponseEntity<ApiResponse<ReviewResponse>> updateReview(
                        @AuthenticationPrincipal Long userId,
                        @Parameter(description = "리뷰 ID") @PathVariable Long reviewId,
                        @Valid @RequestBody ReviewRequest request) {

                ReviewResponse response = callReviewService.updateReview(
                                userId, reviewId, request);

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        @Operation(summary = "미확인 통화 건수 조회", description = "상담사가 아직 확인하지 않은 통화 건수를 조회합니다.")
        @GetMapping("/counselor/unreview-count")
        @PreAuthorize("hasRole('COUNSELOR')")
        public ResponseEntity<ApiResponse<UnreviewedCountResponse>> getUnreviewedCount(
                        @AuthenticationPrincipal Long userId) {

                UnreviewedCountResponse response = callReviewService.getUnreviewedCount(userId);

                return ResponseEntity.ok(ApiResponse.success(response));
        }

        @Operation(summary = "오늘의 통화 건수 조회", description = "상담사가 담당하는 어르신들의 오늘 통화 건수를 조회합니다.")
        @GetMapping("/counselor/today-count")
        @PreAuthorize("hasRole('COUNSELOR')")
        public ResponseEntity<ApiResponse<Long>> getTodayCallCount(
                        @AuthenticationPrincipal Long userId) {

                long todayCount = callReviewService.getTodayCallCount(userId);

                return ResponseEntity.ok(ApiResponse.success(todayCount));
        }

        // ===== 보호자용 API =====

        @Operation(summary = "어르신 통화 기록 목록 조회 (보호자)", description = "보호자가 연결된 어르신의 통화 기록 및 상담사 코멘트를 조회합니다.")
        @GetMapping("/guardian/elderly/{elderlyId}")
        @PreAuthorize("hasRole('GUARDIAN')")
        public ResponseEntity<ApiResponse<PageResponse<GuardianCallReviewResponse>>> getCallReviewsForGuardian(
                        @AuthenticationPrincipal Long userId,
                        @Parameter(description = "어르신 ID") @PathVariable Long elderlyId,
                        @PageableDefault(size = 20, sort = "callAt", direction = Sort.Direction.DESC) Pageable pageable) {

                Page<GuardianCallReviewResponse> page = callReviewService.getCallReviewsForGuardian(
                                userId, elderlyId, pageable);

                return ResponseEntity.ok(ApiResponse.success(PageResponse.from(page)));
        }

        @Operation(summary = "통화 상세 조회 (보호자)", description = "보호자가 어르신의 특정 통화 상세 내용과 상담사 코멘트를 조회합니다.")
        @GetMapping("/guardian/calls/{callId}")
        @PreAuthorize("hasRole('GUARDIAN')")
        public ResponseEntity<ApiResponse<GuardianCallReviewResponse>> getCallDetailForGuardian(
                        @AuthenticationPrincipal Long userId,
                        @Parameter(description = "통화 기록 ID") @PathVariable Long callId) {

                GuardianCallReviewResponse response = callReviewService.getCallDetailForGuardian(
                                userId, callId);

                return ResponseEntity.ok(ApiResponse.success(response));
        }
}