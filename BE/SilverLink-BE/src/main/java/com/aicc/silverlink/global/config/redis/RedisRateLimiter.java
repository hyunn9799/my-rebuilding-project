package com.aicc.silverlink.global.config.redis;

import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

/**
 * Redis Lua Script 기반 Sliding Window Rate Limiter.
 * Sorted Set을 활용하여 지정된 시간 창 내 요청 횟수를 원자적으로 제한합니다.
 *
 * <p>
 * 사용 예시:
 * 
 * <pre>
 * // API 엔드포인트: 1분에 10회 제한
 * if (!rateLimiter.isAllowed("api:login:" + userId, 10, Duration.ofMinutes(1))) {
 *     throw new TooManyRequestsException("잠시 후 다시 시도해 주세요.");
 * }
 *
 * // SMS 인증: 1시간에 5회 제한
 * if (!rateLimiter.isAllowed("sms:" + phone, 5, Duration.ofHours(1))) {
 *     throw new TooManyRequestsException("인증 요청 횟수를 초과했습니다.");
 * }
 * </pre>
 */
@Slf4j
@Component
public class RedisRateLimiter {

    private final StringRedisTemplate redis;
    private final RedisScript<Long> rateLimiterScript;

    public RedisRateLimiter(StringRedisTemplate redis) {
        this.redis = redis;

        DefaultRedisScript<Long> script = new DefaultRedisScript<>();
        script.setLocation(new ClassPathResource("redis/sliding_window_rate_limiter.lua"));
        script.setResultType(Long.class);
        this.rateLimiterScript = script;
    }

    /**
     * 요청이 허용되는지 확인합니다 (Sliding Window).
     *
     * @param key         Rate limit 키 (예: "api:login:userId:42")
     * @param maxRequests 윈도우 내 최대 허용 요청 수
     * @param window      시간 윈도우 크기
     * @return true = 허용, false = 초과 (차단)
     */
    public boolean isAllowed(String key, int maxRequests, Duration window) {
        String redisKey = "rl:" + key;
        long now = System.currentTimeMillis();
        String requestId = now + ":" + UUID.randomUUID().toString().substring(0, 8);

        try {
            Long result = redis.execute(
                    rateLimiterScript,
                    List.of(redisKey),
                    String.valueOf(window.getSeconds()),
                    String.valueOf(maxRequests),
                    String.valueOf(now),
                    requestId);

            boolean allowed = result != null && result == 1L;

            if (!allowed) {
                log.warn("[RATE_LIMIT] 요청 초과: key={}, maxRequests={}/{}", key, maxRequests, window);
            }

            return allowed;
        } catch (Exception e) {
            log.error("[RATE_LIMIT] Redis 오류, 기본 허용: key={}, error={}", key, e.getMessage());
            return true; // Redis 장애 시 허용 (Graceful Degradation)
        }
    }

    /**
     * 현재 윈도우 내 남은 요청 수를 반환합니다.
     */
    public long getRemainingRequests(String key, int maxRequests, Duration window) {
        String redisKey = "rl:" + key;
        long now = System.currentTimeMillis();
        long windowStartMs = now - (window.toMillis());

        try {
            // 만료된 항목 정리 후 카운트
            redis.opsForZSet().removeRangeByScore(redisKey, Double.NEGATIVE_INFINITY, windowStartMs);
            Long current = redis.opsForZSet().zCard(redisKey);
            return Math.max(0, maxRequests - (current != null ? current : 0));
        } catch (Exception e) {
            return maxRequests; // Redis 장애 시 전체 허용
        }
    }
}
