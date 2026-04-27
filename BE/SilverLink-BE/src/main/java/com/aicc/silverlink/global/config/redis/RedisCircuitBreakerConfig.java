package com.aicc.silverlink.global.config.redis;

import io.github.resilience4j.circuitbreaker.CircuitBreaker;
import io.github.resilience4j.circuitbreaker.CircuitBreakerConfig;
import io.github.resilience4j.circuitbreaker.CircuitBreakerRegistry;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.RedisConnectionFailureException;

import java.time.Duration;

/**
 * Redis Circuit Breaker 설정.
 * Redis 장애 시 애플리케이션 전체 다운을 방지하는 장애 격리 패턴.
 *
 * <ul>
 * <li>실패율 50% 초과 시 Circuit Open (최근 10회 호출 기준)</li>
 * <li>Open 상태 30초 유지 후 Half-Open 전환</li>
 * <li>Half-Open에서 3회 시도 성공 시 Closed 복귀</li>
 * </ul>
 */
@Configuration
public class RedisCircuitBreakerConfig {

    public static final String REDIS_CB = "redisCircuitBreaker";

    @Bean
    public CircuitBreakerConfig customRedisCircuitBreakerConfig() {
        return CircuitBreakerConfig.custom()
                .failureRateThreshold(50) // 실패율 50% 초과 시 Open
                .waitDurationInOpenState(Duration.ofSeconds(30)) // Open 30초 유지
                .slidingWindowType(CircuitBreakerConfig.SlidingWindowType.COUNT_BASED)
                .slidingWindowSize(10) // 최근 10회 호출 기준
                .permittedNumberOfCallsInHalfOpenState(3) // Half-Open 시 3회 시도
                .recordExceptions(
                        RedisConnectionFailureException.class,
                        io.lettuce.core.RedisException.class,
                        io.lettuce.core.RedisConnectionException.class)
                .build();
    }

    @Bean
    public CircuitBreakerRegistry circuitBreakerRegistry(CircuitBreakerConfig config) {
        return CircuitBreakerRegistry.of(config);
    }

    @Bean
    public CircuitBreaker redisCircuitBreaker(CircuitBreakerRegistry registry) {
        return registry.circuitBreaker(REDIS_CB);
    }
}
