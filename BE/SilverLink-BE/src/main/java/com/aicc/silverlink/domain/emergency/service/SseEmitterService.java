package com.aicc.silverlink.domain.emergency.service;

import com.aicc.silverlink.domain.emergency.dto.EmergencyAlertDto.RealtimeResponse;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * SSE (Server-Sent Events) 실시간 알림 서비스
 *
 * 클라이언트가 SSE 연결을 맺으면 서버에서 실시간으로 긴급 알림을 푸시
 */
@Slf4j
@Service
public class SseEmitterService {

    // 사용자별 SSE Emitter 관리
    // Key: userId, Value: 해당 사용자의 SSE Emitter 목록 (여러 탭/기기 지원)
    private final Map<Long, List<SseEmitter>> emittersByUserId = new ConcurrentHashMap<>();

    // SSE 타임아웃 (30분)
    private static final long SSE_TIMEOUT = 30 * 60 * 1000L;

    // ========== SSE 연결 관리 ==========

    /**
     * SSE 연결 생성
     *
     * @param userId 사용자 ID
     * @return SseEmitter
     */
    public SseEmitter createEmitter(Long userId) {
        SseEmitter emitter = new SseEmitter(SSE_TIMEOUT);

        // 사용자별 Emitter 목록에 추가
        emittersByUserId.computeIfAbsent(userId, k -> new CopyOnWriteArrayList<>()).add(emitter);

        log.info("[SSE] 연결 생성. userId={}, 현재 연결 수={}",
                userId, emittersByUserId.get(userId).size());

        // 연결 종료 시 정리
        emitter.onCompletion(() -> {
            removeEmitter(userId, emitter);
            log.info("[SSE] 연결 완료. userId={}", userId);
        });

        emitter.onTimeout(() -> {
            removeEmitter(userId, emitter);
            log.info("[SSE] 연결 타임아웃. userId={}", userId);
        });

        emitter.onError(e -> {
            removeEmitter(userId, emitter);
            log.warn("[SSE] 연결 오류. userId={}, error={}", userId, e.getMessage());
        });

        // 연결 즉시 연결 확인 이벤트 전송
        sendConnectedEvent(emitter, userId);

        return emitter;
    }

    /**
     * Emitter 제거
     */
    private void removeEmitter(Long userId, SseEmitter emitter) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters != null) {
            emitters.remove(emitter);
            if (emitters.isEmpty()) {
                emittersByUserId.remove(userId);
            }
        }
    }

    /**
     * 연결 확인 이벤트 전송
     */
    private void sendConnectedEvent(SseEmitter emitter, Long userId) {
        try {
            emitter.send(SseEmitter.event()
                    .name("connected")
                    .data(Map.of(
                            "message", "SSE 연결 성공",
                            "userId", userId,
                            "timestamp", System.currentTimeMillis()
                    )));
        } catch (IOException e) {
            log.error("[SSE] 연결 확인 이벤트 전송 실패. userId={}", userId, e);
        }
    }

    // ========== 알림 발송 ==========

    /**
     * 특정 사용자에게 긴급 알림 전송
     *
     * @param userId 수신자 ID
     * @param alert 긴급 알림
     */
    public void sendAlertToUser(Long userId, EmergencyAlert alert) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            log.debug("[SSE] 연결된 Emitter 없음. userId={}", userId);
            return;
        }

        RealtimeResponse response = RealtimeResponse.from(alert);

        // 해당 사용자의 모든 연결에 전송 (여러 탭/기기)
        List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("emergency-alert")
                        .data(response));

                log.info("[SSE] 긴급 알림 전송 성공. userId={}, alertId={}", userId, alert.getId());

            } catch (IOException e) {
                log.warn("[SSE] 긴급 알림 전송 실패. userId={}, error={}", userId, e.getMessage());
                deadEmitters.add(emitter);
            }
        }

        // 실패한 Emitter 정리
        deadEmitters.forEach(emitter -> removeEmitter(userId, emitter));
    }

    /**
     * 여러 사용자에게 긴급 알림 전송
     *
     * @param userIds 수신자 ID 목록
     * @param alert 긴급 알림
     */
    public void sendAlertToUsers(List<Long> userIds, EmergencyAlert alert) {
        for (Long userId : userIds) {
            sendAlertToUser(userId, alert);
        }
    }

    /**
     * 모든 연결된 사용자에게 알림 전송 (브로드캐스트)
     *
     * @param alert 긴급 알림
     */
    public void broadcastAlert(EmergencyAlert alert) {
        RealtimeResponse response = RealtimeResponse.from(alert);

        emittersByUserId.forEach((userId, emitters) -> {
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("emergency-alert")
                            .data(response));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }

            deadEmitters.forEach(emitter -> removeEmitter(userId, emitter));
        });

        log.info("[SSE] 브로드캐스트 완료. alertId={}, 대상 사용자 수={}",
                alert.getId(), emittersByUserId.size());
    }

    // ========== 상태 알림 ==========

    /**
     * 알림 상태 변경 전송 (처리 완료 등)
     */
    public void sendAlertStatusUpdate(Long userId, Long alertId, String status) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("alert-status-update")
                        .data(Map.of(
                                "alertId", alertId,
                                "status", status,
                                "timestamp", System.currentTimeMillis()
                        )));
            } catch (IOException e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    /**
     * 미확인 알림 수 업데이트 전송
     */
    public void sendUnreadCountUpdate(Long userId, long unreadCount) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        if (emitters == null || emitters.isEmpty()) {
            return;
        }

        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event()
                        .name("unread-count-update")
                        .data(Map.of(
                                "unreadCount", unreadCount,
                                "timestamp", System.currentTimeMillis()
                        )));
            } catch (IOException e) {
                removeEmitter(userId, emitter);
            }
        }
    }

    // ========== Heartbeat ==========

    /**
     * 하트비트 전송 (연결 유지용)
     * 스케줄러에서 주기적으로 호출
     */
    public void sendHeartbeat() {
        emittersByUserId.forEach((userId, emitters) -> {
            List<SseEmitter> deadEmitters = new CopyOnWriteArrayList<>();

            for (SseEmitter emitter : emitters) {
                try {
                    emitter.send(SseEmitter.event()
                            .name("heartbeat")
                            .data(Map.of("timestamp", System.currentTimeMillis())));
                } catch (IOException e) {
                    deadEmitters.add(emitter);
                }
            }

            deadEmitters.forEach(emitter -> removeEmitter(userId, emitter));
        });
    }

    // ========== 통계 ==========

    /**
     * 현재 연결된 사용자 수
     */
    public int getConnectedUserCount() {
        return emittersByUserId.size();
    }

    /**
     * 현재 총 연결 수
     */
    public int getTotalConnectionCount() {
        return emittersByUserId.values().stream()
                .mapToInt(List::size)
                .sum();
    }

    /**
     * 특정 사용자의 연결 여부 확인
     */
    public boolean isUserConnected(Long userId) {
        List<SseEmitter> emitters = emittersByUserId.get(userId);
        return emitters != null && !emitters.isEmpty();
    }
}
