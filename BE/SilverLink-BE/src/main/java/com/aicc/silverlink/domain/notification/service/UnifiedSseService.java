package com.aicc.silverlink.domain.notification.service;

import com.aicc.silverlink.domain.emergency.dto.EmergencyAlertDto;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import com.aicc.silverlink.domain.notification.dto.NotificationDto;
import com.aicc.silverlink.domain.notification.entity.Notification;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * 통합 SSE 서비스
 *
 * 긴급 알림(EmergencyAlert)과 일반 알림(Notification) 모두 처리
 *
 * SSE 이벤트 종류:
 * - connected: 연결 성공
 * - emergency-alert: 긴급 알림 (CRITICAL/WARNING)
 * - notification: 일반 알림 (문의답변, 민원답변, 접근권한 등)
 * - unread-count: 미확인 알림 수 업데이트
 * - alert-status-update: 긴급 알림 상태 변경
 * - heartbeat: 연결 유지
 */
@Slf4j
@Service
public class UnifiedSseService {

    // 사용자별 SSE 연결 관리
    private final Map<Long, List<SseEmitter>> userEmitters = new ConcurrentHashMap<>();

    // SSE 연결 타임아웃 (30분)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    // ==================== SSE 연결 관리 ====================

    /**
     * SSE 연결 생성
     */
    public SseEmitter subscribe(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        userEmitters.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        log.info("[SSE] 연결 생성. userId={}, 현재 연결 수={}",
                userId, userEmitters.get(userId).size());

        // 연결 종료 시 정리
        emitter.onCompletion(() -> removeEmitter(userId, emitter));
        emitter.onTimeout(() -> {
            log.info("[SSE] 타임아웃. userId={}", userId);
            removeEmitter(userId, emitter);
        });
        emitter.onError(e -> {
            log.warn("[SSE] 에러. userId={}, error={}", userId, e.getMessage());
            removeEmitter(userId, emitter);
        });

        // 연결 확인 이벤트 전송
        sendEvent(emitter, "connected", Map.of(
                "message", "SSE 연결 성공",
                "userId", userId,
                "timestamp", System.currentTimeMillis()
        ));

        return emitter;
    }

    /**
     * SSE 연결 해제
     */
    public void unsubscribe(Long userId) {
        List<SseEmitter> emitters = userEmitters.remove(userId);
        if (emitters != null) {
            emitters.forEach(SseEmitter::complete);
            log.info("[SSE] 연결 해제. userId={}, 해제된 연결 수={}", userId, emitters.size());
        }
    }

    /**
     * Emitter 제거
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                userEmitters.remove(userId);
            }
        }
    }

    // ==================== 긴급 알림 전송 ====================

    /**
     * 긴급 알림 전송 (단일 사용자)
     */
    public void sendEmergencyAlert(Long userId, EmergencyAlert alert) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[SSE] 연결 없음 (긴급 알림). userId={}", userId);
            return;
        }

        EmergencyAlertDto.RealtimeResponse response = EmergencyAlertDto.RealtimeResponse.from(alert);

        for (SseEmitter emitter : emitters) {
            if (sendEvent(emitter, "emergency-alert", response)) {
                log.info("[SSE] 긴급 알림 전송 성공. userId={}, alertId={}", userId, alert.getId());
            } else {
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * 긴급 알림 전송 (여러 사용자)
     */
    public void sendEmergencyAlertToUsers(List<Long> userIds, EmergencyAlert alert) {
        for (Long userId : userIds) {
            sendEmergencyAlert(userId, alert);
        }
    }

    /**
     * 긴급 알림 상태 변경 전송
     */
    public void sendEmergencyAlertStatusUpdate(Long userId, Long alertId, String status) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        Map<String, Object> data = Map.of(
                "alertId", alertId,
                "status", status,
                "timestamp", System.currentTimeMillis()
        );

        for (SseEmitter emitter : emitters) {
            if (!sendEvent(emitter, "alert-status-update", data)) {
                removeEmitter(userId, emitter);
            }
        }
    }

    // ==================== 일반 알림 전송 ====================

    /**
     * 일반 알림 전송 (단일 사용자)
     */
    public void sendNotification(Long userId, Notification notification) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[SSE] 연결 없음 (일반 알림). userId={}", userId);
            return;
        }

        NotificationDto.RealtimeResponse response = NotificationDto.RealtimeResponse.from(notification);

        for (SseEmitter emitter : emitters) {
            if (sendEvent(emitter, "notification", response)) {
                log.info("[SSE] 일반 알림 전송 성공. userId={}, notificationId={}", userId, notification.getId());
            } else {
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * 일반 알림 전송 (여러 사용자)
     */
    public void sendNotificationToUsers(List<Long> userIds, Notification notification) {
        for (Long userId : userIds) {
            sendNotification(userId, notification);
        }
    }

    // ==================== 미확인 수 업데이트 ====================

    /**
     * 미확인 알림 수 업데이트 전송
     *
     * @param userId 사용자 ID
     * @param emergencyUnread 긴급 알림 미확인 수
     * @param notificationUnread 일반 알림 미확인 수
     */
    public void sendUnreadCountUpdate(Long userId, long emergencyUnread, long notificationUnread) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        Map<String, Object> data = Map.of(
                "emergencyUnread", emergencyUnread,
                "notificationUnread", notificationUnread,
                "totalUnread", emergencyUnread + notificationUnread,
                "timestamp", System.currentTimeMillis()
        );

        for (SseEmitter emitter : emitters) {
            if (!sendEvent(emitter, "unread-count", data)) {
                removeEmitter(userId, emitter);
            }
        }
    }

    // ==================== 하트비트 ====================

    /**
     * 하트비트 전송 (연결 유지용)
     * 스케줄러에서 주기적으로 호출
     */
    public void sendHeartbeat() {
        Map<String, Object> data = Map.of("timestamp", System.currentTimeMillis());

        userEmitters.forEach((userId, emitters) -> {
            for (SseEmitter emitter : emitters) {
                if (!sendEvent(emitter, "heartbeat", data)) {
                    removeEmitter(userId, emitter);
                }
            }
        });
    }

    // ==================== 유틸리티 ====================

    /**
     * 이벤트 전송 (공통)
     * @return 전송 성공 여부
     */
    private boolean sendEvent(SseEmitter emitter, String eventName, Object data) {
        try {
            emitter.send(SseEmitter.event()
                    .name(eventName)
                    .data(data));
            return true;
        } catch (IOException e) {
            log.warn("[SSE] 이벤트 전송 실패. event={}, error={}", eventName, e.getMessage());
            return false;
        }
    }

    /**
     * 현재 연결된 사용자 수
     */
    public int getConnectedUserCount() {
        return userEmitters.size();
    }

    /**
     * 현재 총 연결 수
     */
    public int getTotalConnectionCount() {
        return userEmitters.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * 특정 사용자의 연결 여부 확인
     */
    public boolean isUserConnected(Long userId) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        return emitters != null && !emitters.isEmpty();
    }

    /**
     * 특정 사용자의 연결 수
     */
    public int getConnectionCount(Long userId) {
        List<SseEmitter> emitters = userEmitters.get(userId);
        return emitters != null ? emitters.size() : 0;
    }
}
