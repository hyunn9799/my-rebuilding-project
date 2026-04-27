package com.aicc.silverlink.global.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

/**
 * Redis 연결 상태를 확인하는 Health Indicator.
 * Spring Actuator /actuator/health 엔드포인트에 Redis 상태를 노출합니다.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisHealthIndicator implements HealthIndicator {

    private final StringRedisTemplate redis;

    @Override
    public Health health() {
        try {
            var connection = redis.getConnectionFactory().getConnection();
            try {
                String pong = connection.ping();
                return Health.up()
                        .withDetail("status", "connected")
                        .withDetail("pong", pong)
                        .build();
            } finally {
                connection.close();
            }
        } catch (Exception e) {
            log.warn("[REDIS_HEALTH] Redis 연결 실패: {}", e.getMessage());
            return Health.down(e)
                    .withDetail("status", "disconnected")
                    .build();
        }
    }
}
