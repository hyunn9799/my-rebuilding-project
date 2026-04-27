package com.aicc.silverlink.domain.notification.controller;

import com.aicc.silverlink.domain.notification.dto.NotificationDto.*;
import com.aicc.silverlink.domain.notification.service.NotificationService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import com.aicc.silverlink.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 일반 알림 Controller
 */
@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@Tag(name = "일반 알림", description = "문의/민원 답변, 접근권한, 담당 배정 등 일반 알림 API")
public class NotificationController {

    private final NotificationService notificationService;

    // ========== 알림 조회 ==========

    @GetMapping
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "알림 목록 조회", description = "사용자의 알림 목록을 페이징하여 조회합니다.")
    public ResponseEntity<ApiResponse<Page<SummaryResponse>>> getNotifications(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {

        Long userId = SecurityUtils.currentUserId();
        Page<SummaryResponse> notifications = notificationService.getNotifications(userId, pageable);

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "미확인 알림 목록", description = "읽지 않은 알림 목록을 조회합니다.")
    public ResponseEntity<ApiResponse<List<SummaryResponse>>> getUnreadNotifications() {
        Long userId = SecurityUtils.currentUserId();
        List<SummaryResponse> notifications = notificationService.getUnreadNotifications(userId);

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "미확인 알림 수", description = "읽지 않은 알림 개수를 조회합니다.")
    public ResponseEntity<ApiResponse<Long>> getUnreadCount() {
        Long userId = SecurityUtils.currentUserId();
        long count = notificationService.getUnreadCount(userId);

        return ResponseEntity.ok(ApiResponse.success(count));
    }

    @GetMapping("/recent")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "최근 알림 조회", description = "최근 N개의 알림을 조회합니다. (팝업용)")
    public ResponseEntity<ApiResponse<List<SummaryResponse>>> getRecentNotifications(
            @RequestParam(defaultValue = "5") int limit) {

        Long userId = SecurityUtils.currentUserId();
        List<SummaryResponse> notifications = notificationService.getRecentNotifications(userId, limit);

        return ResponseEntity.ok(ApiResponse.success(notifications));
    }

    @GetMapping("/{notificationId}")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "알림 상세 조회", description = "알림 상세 정보를 조회합니다. (자동 읽음 처리)")
    public ResponseEntity<ApiResponse<DetailResponse>> getNotificationDetail(
            @PathVariable Long notificationId) {

        Long userId = SecurityUtils.currentUserId();
        DetailResponse notification = notificationService.getNotificationDetail(notificationId, userId);

        return ResponseEntity.ok(ApiResponse.success(notification));
    }

    // ========== 읽음 처리 ==========

    @PostMapping("/{notificationId}/read")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "알림 읽음 처리", description = "특정 알림을 읽음 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> markAsRead(@PathVariable Long notificationId) {
        Long userId = SecurityUtils.currentUserId();
        notificationService.markAsRead(notificationId, userId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/read-all")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "전체 읽음 처리", description = "모든 알림을 읽음 처리합니다.")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead() {
        Long userId = SecurityUtils.currentUserId();
        notificationService.markAllAsRead(userId);

        return ResponseEntity.ok(ApiResponse.success(null));
    }

    // ========== 통계 ==========

    @GetMapping("/stats")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "알림 통계", description = "사용자의 알림 통계를 조회합니다.")
    public ResponseEntity<ApiResponse<StatsResponse>> getStats() {
        Long userId = SecurityUtils.currentUserId();
        StatsResponse stats = notificationService.getStats(userId);

        return ResponseEntity.ok(ApiResponse.success(stats));
    }
}
