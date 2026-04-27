package com.aicc.silverlink.domain.notification.scheduler;

import com.aicc.silverlink.domain.notification.service.UnifiedSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/**
 * SSE 연결 유지 스케줄러
 *
 * 주기적으로 하트비트를 전송하여 SSE 연결을 유지하고
 * 끊어진 연결을 정리합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class SseHeartbeatScheduler {

    private final UnifiedSseService unifiedSseService;

    /**
     * 30초마다 하트비트 전송
     */
    @Scheduled(fixedRate = 30000)
    public void sendHeartbeat() {
        int connectedUsers = unifiedSseService.getConnectedUserCount();
        if (connectedUsers > 0) {
            unifiedSseService.sendHeartbeat();
            log.debug("[SSE Heartbeat] 전송 완료. 연결된 사용자 수: {}, 총 연결 수: {}",
                    connectedUsers, unifiedSseService.getTotalConnectionCount());
        }
    }

    /**
     * 1분마다 연결 통계 로깅
     */
    @Scheduled(fixedRate = 60000)
    public void logConnectionStats() {
        int connectedUsers = unifiedSseService.getConnectedUserCount();
        int totalConnections = unifiedSseService.getTotalConnectionCount();

        if (connectedUsers > 0) {
            log.info("[SSE Stats] 연결된 사용자: {}, 총 연결: {}", connectedUsers, totalConnections);
        }
    }
}
