package com.aicc.silverlink.global.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Redis Keyspace Notification Listener.
 * 세션 키가 만료되면 자동으로 후처리(감사 로그, 통계 집계)를 수행합니다.
 *
 * <p>
 * Redis에서 keyspace notification이 활성화되어야 합니다:
 * 
 * <pre>
 * redis-cli config set notify-keyspace-events Ex
 * </pre>
 */
@Slf4j
@Component
public class SessionExpirationListener implements MessageListener {

    public SessionExpirationListener(RedisMessageListenerContainer container) {
        container.addMessageListener(this, new PatternTopic("__keyevent@*__:expired"));
        log.info("[REDIS_KEYSPACE] 세션 만료 알림 리스너 등록 완료");
    }

    @Override
    public void onMessage(Message message, byte[] pattern) {
        String expiredKey = new String(message.getBody());

        if (expiredKey.startsWith("sess:") && !expiredKey.contains("invalidated")) {
            String sid = expiredKey.substring("sess:".length());
            log.info("[SESSION_EXPIRED] 세션 자동 만료: sid={}", sid);

            // 통계 집계, 감사 로그 등 후처리 확장 포인트
            // TODO: auditLogRepository.save(AuditLog.expired(sid));
        }
    }
}
