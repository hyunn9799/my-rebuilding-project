package com.aicc.silverlink.domain.notification.controller;

import com.aicc.silverlink.domain.emergency.repository.EmergencyAlertRecipientRepository;
import com.aicc.silverlink.domain.notification.dto.NotificationDto.UnreadCountResponse;
import com.aicc.silverlink.domain.notification.repository.NotificationRepository;
import com.aicc.silverlink.domain.notification.service.UnifiedSseService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import com.aicc.silverlink.global.util.SecurityUtils;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

/**
 * 통합 SSE Controller
 *
 * 긴급 알림 + 일반 알림 모두 수신하는 단일 SSE 엔드포인트
 */
@Slf4j
@RestController
@RequestMapping("/api/sse")
@RequiredArgsConstructor
@Tag(name = "실시간 알림 (SSE)", description = "Server-Sent Events 기반 실시간 알림")
public class UnifiedSseController {

    private final UnifiedSseService unifiedSseService;
    private final EmergencyAlertRecipientRepository emergencyRecipientRepository;
    private final NotificationRepository notificationRepository;

    /**
     * SSE 연결
     *
     * 프론트엔드에서 이 엔드포인트에 연결하면 실시간 알림을 수신합니다.
     *
     * 수신 가능한 이벤트:
     * - connected: 연결 성공
     * - emergency-alert: 긴급 알림 (CRITICAL/WARNING)
     * - notification: 일반 알림 (문의답변, 민원답변, 접근권한 등)
     * - unread-count: 미확인 알림 수 업데이트
     * - alert-status-update: 긴급 알림 상태 변경
     * - heartbeat: 연결 유지 (30초 간격)
     *
     * 사용 예시 (JavaScript):
     * ```javascript
     * const eventSource = new EventSource('/api/sse/subscribe', {
     * withCredentials: true
     * });
     *
     * eventSource.addEventListener('connected', (e) => {
     * console.log('연결 성공:', JSON.parse(e.data));
     * });
     *
     * eventSource.addEventListener('emergency-alert', (e) => {
     * const alert = JSON.parse(e.data);
     * console.log('🚨 긴급 알림:', alert);
     * showEmergencyAlert(alert);
     * });
     *
     * eventSource.addEventListener('notification', (e) => {
     * const notification = JSON.parse(e.data);
     * console.log('🔔 일반 알림:', notification);
     * showNotification(notification);
     * });
     *
     * eventSource.addEventListener('unread-count', (e) => {
     * const data = JSON.parse(e.data);
     * updateBadge(data.totalUnread);
     * });
     *
     * eventSource.onerror = (error) => {
     * console.error('SSE 에러:', error);
     * // 재연결 로직
     * };
     * ```
     */
    @GetMapping(value = "/subscribe", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "SSE 연결 (통합)", description = "긴급 알림과 일반 알림을 모두 수신하는 SSE 연결을 생성합니다.")
    public SseEmitter subscribe() {
        Long userId = SecurityUtils.currentUserId();
        log.info("[SSE] 연결 요청. userId={}", userId);

        return unifiedSseService.subscribe(userId);
    }

    /**
     * SSE 연결 상태 확인
     */
    @GetMapping("/status")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "SSE 연결 상태", description = "현재 사용자의 SSE 연결 상태를 확인합니다.")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getStatus() {
        Long userId = SecurityUtils.currentUserId();

        Map<String, Object> status = Map.of(
                "userId", userId,
                "connected", unifiedSseService.isUserConnected(userId),
                "connectionCount", unifiedSseService.getConnectionCount(userId),
                "totalConnectedUsers", unifiedSseService.getConnectedUserCount(),
                "totalConnections", unifiedSseService.getTotalConnectionCount());

        return ResponseEntity.ok(ApiResponse.success(status));
    }

    /**
     * 통합 미확인 알림 수 조회
     *
     * 긴급 알림 + 일반 알림의 미확인 수를 한 번에 조회
     */
    @GetMapping("/unread-count")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "통합 미확인 알림 수", description = "긴급 알림과 일반 알림의 미확인 수를 조회합니다.")
    public ResponseEntity<ApiResponse<UnreadCountResponse>> getUnreadCount() {
        Long userId = SecurityUtils.currentUserId();

        long emergencyUnread = emergencyRecipientRepository.countByReceiverIdAndIsReadFalse(userId);
        long notificationUnread = notificationRepository.countUnreadByReceiverId(userId);

        UnreadCountResponse response = UnreadCountResponse.of(emergencyUnread, notificationUnread);

        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
