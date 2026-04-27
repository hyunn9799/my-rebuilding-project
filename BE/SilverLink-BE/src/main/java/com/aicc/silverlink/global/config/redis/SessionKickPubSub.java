package com.aicc.silverlink.global.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.connection.Message;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Component;

/**
 * Redis Pub/Sub 기반 세션 킥 실시간 알림.
 * 세션 강제 종료 시 Redis 채널로 메시지를 발행하고, 구독하여 후처리합니다.
 *
 * <p>
 * 발행 형식: {@code userId:sessionId}
 * <p>
 * 채널: {@code session:kicked}
 */
@Slf4j
@Component
public class SessionKickPubSub implements MessageListener {

    public static final String CHANNEL = "session:kicked";

    private final StringRedisTemplate redis;

    public SessionKickPubSub(StringRedisTemplate redis,
            RedisMessageListenerContainer container) {
        this.redis = redis;
        container.addMessageListener(this, new ChannelTopic(CHANNEL));
        log.info("[REDIS_PUBSUB] 세션 킥 알림 구독 시작: channel={}", CHANNEL);
    }

    /**
     * 세션 강제 종료 메시지를 발행합니다.
     */
    public void publishSessionKicked(Long userId, String sid) {
        String message = userId + ":" + sid;
        redis.convertAndSend(CHANNEL, message);
        log.debug("[REDIS_PUBSUB] 세션 킥 알림 발행: {}", message);
    }

    /**
     * 다른 서버 인스턴스에서 발행된 세션 킥 메시지를 수신합니다.
     * 스케일 아웃 환경에서 모든 인스턴스가 세션 무효화를 인지할 수 있습니다.
     */
    @Override
    public void onMessage(Message message, byte[] pattern) {
        String payload = new String(message.getBody());
        String[] parts = payload.split(":");
        if (parts.length >= 2) {
            String userId = parts[0];
            String sid = parts[1];
            log.info("[SESSION_KICKED_NOTIFICATION] 다른 인스턴스에서 세션 강제 종료: userId={}, sid={}", userId, sid);
        }
    }
}
