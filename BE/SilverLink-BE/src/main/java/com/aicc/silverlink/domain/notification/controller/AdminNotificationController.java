package com.aicc.silverlink.domain.notification.controller;

import com.aicc.silverlink.domain.notification.dto.NotificationDto.SystemNotificationRequest;
import com.aicc.silverlink.domain.notification.entity.Notification;
import com.aicc.silverlink.domain.notification.service.NotificationService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 관리자용 알림 Controller
 */
@RestController
@RequestMapping("/api/admin/notifications")
@RequiredArgsConstructor
@Tag(name = "관리자 알림", description = "관리자용 시스템 알림 발송 API")
public class AdminNotificationController {

    private final NotificationService notificationService;

    /**
     * 시스템 알림 발송
     *
     * 관리자가 직접 특정 사용자들 또는 전체 사용자에게 알림을 발송합니다.
     */
    @PostMapping("/system")
    @PreAuthorize("hasRole('ADMIN')")
    @Operation(summary = "시스템 알림 발송", description = "특정 사용자들 또는 전체 사용자에게 시스템 알림을 발송합니다.")
    public ResponseEntity<ApiResponse<Integer>> sendSystemNotification(
            @Valid @RequestBody SystemNotificationRequest request) {

        List<Notification> notifications = notificationService.createSystemNotifications(request);

        return ResponseEntity.ok(ApiResponse.success(notifications.size()));
    }
}
