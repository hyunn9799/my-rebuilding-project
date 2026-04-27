package com.aicc.silverlink.global.config.redis;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

/**
 * Redis 장애 시 로컬 캐시로 자동 전환하는 Fallback 캐시.
 * Circuit Breaker와 함께 사용하여 완전한 Graceful Degradation을 구현합니다.
 *
 * <p>
 * 동작 방식:
 * <ol>
 * <li>Redis에서 값 조회 시도</li>
 * <li>Redis 장애 시 로컬 ConcurrentHashMap에서 캐시 조회</li>
 * <li>로컬 캐시도 없으면 DB Fallback 실행</li>
 * </ol>
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class RedisFallbackCache {

    private final StringRedisTemplate redis;

    // 로컬 캐시 (Redis 장애 시 임시 사용)
    private final ConcurrentHashMap<String, CacheEntry> localCache = new ConcurrentHashMap<>();

    private record CacheEntry(String value, long expiresAt) {
        boolean isExpired() {
            return System.currentTimeMillis() > expiresAt;
        }
    }

    /**
     * Redis에서 값을 조회하되, 실패 시 로컬 캐시 → DB Fallback 순서로 처리합니다.
     *
     * @param key        Redis 키
     * @param dbFallback DB에서 값을 가져오는 Supplier (최후의 수단)
     * @param ttlSeconds 캐시 TTL (초)
     * @return 캐시된 값 또는 DB에서 조회한 값
     */
    public String getOrFallback(String key, Supplier<String> dbFallback, long ttlSeconds) {
        // 1차: Redis 시도
        try {
            String value = redis.opsForValue().get(key);
            if (value != null) {
                // 로컬 캐시에도 저장 (Redis 장애 대비)
                localCache.put(key, new CacheEntry(value, System.currentTimeMillis() + ttlSeconds * 1000));
                return value;
            }
        } catch (Exception e) {
            log.warn("[REDIS_FALLBACK] Redis 조회 실패, 로컬 캐시 확인: key={}", key);
        }

        // 2차: 로컬 캐시
        CacheEntry local = localCache.get(key);
        if (local != null && !local.isExpired()) {
            log.info("[REDIS_FALLBACK] 로컬 캐시 히트: key={}", key);
            return local.value();
        }

        // 3차: DB Fallback
        log.info("[REDIS_FALLBACK] DB Fallback 실행: key={}", key);
        String dbValue = dbFallback.get();

        if (dbValue != null) {
            // 로컬 캐시에 저장
            localCache.put(key, new CacheEntry(dbValue, System.currentTimeMillis() + ttlSeconds * 1000));

            // Redis 복구 시 저장 시도
            try {
                redis.opsForValue().set(key, dbValue, ttlSeconds, TimeUnit.SECONDS);
            } catch (Exception e) {
                log.debug("[REDIS_FALLBACK] Redis 저장 실패 (무시): key={}", key);
            }
        }

        return dbValue;
    }

    /**
     * 만료된 로컬 캐시 항목을 정리합니다.
     * Scheduled 태스크에서 주기적으로 호출하세요.
     */
    public void evictExpired() {
        localCache.entrySet().removeIf(entry -> entry.getValue().isExpired());
    }

    /**
     * 로컬 캐시 크기를 반환합니다 (모니터링용).
     */
    public int localCacheSize() {
        return localCache.size();
    }
}
