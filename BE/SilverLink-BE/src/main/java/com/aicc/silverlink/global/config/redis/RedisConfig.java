package com.aicc.silverlink.global.config.redis;

import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.jsontype.BasicPolymorphicTypeValidator;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.ClassPathResource;
import org.springframework.data.redis.cache.RedisCacheConfiguration;
import org.springframework.data.redis.cache.RedisCacheManager;
import org.springframework.data.redis.connection.RedisConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.data.redis.listener.PatternTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.data.redis.serializer.GenericJackson2JsonRedisSerializer;
import org.springframework.data.redis.serializer.RedisSerializationContext.SerializationPair;
import org.springframework.data.redis.serializer.StringRedisSerializer;

import java.time.Duration;

@EnableCaching
@Configuration
public class RedisConfig {

        @Bean
        public StringRedisTemplate stringRedisTemplate(RedisConnectionFactory cf) {
                return new StringRedisTemplate(cf);
        }

        /**
         * Lua Script Bean: 세션 원자적 교체 (Atomic Session Swap)
         * 기존 세션 무효화 + 새 세션 생성을 단일 원자적 연산으로 수행
         */
        @Bean
        public RedisScript<String> atomicSessionSwapScript() {
                DefaultRedisScript<String> script = new DefaultRedisScript<>();
                script.setLocation(new ClassPathResource("redis/atomic_session_swap.lua"));
                script.setResultType(String.class);
                return script;
        }

        /**
         * Spring Cache + Redis 통합 CacheManager.
         * Jackson JSON 직렬화로 JDK 직렬화 대비 호환성/가독성 향상.
         */
        @Bean
        public RedisCacheManager cacheManager(RedisConnectionFactory cf) {
                // Jackson ObjectMapper (캐시 전용 — 타입 정보 포함)
                ObjectMapper cacheMapper = new ObjectMapper();
                cacheMapper.registerModule(new JavaTimeModule());
                cacheMapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
                cacheMapper.activateDefaultTyping(
                                BasicPolymorphicTypeValidator.builder()
                                                .allowIfBaseType(Object.class)
                                                .build(),
                                ObjectMapper.DefaultTyping.NON_FINAL,
                                JsonTypeInfo.As.PROPERTY);

                GenericJackson2JsonRedisSerializer jsonSerializer = new GenericJackson2JsonRedisSerializer(cacheMapper);

                RedisCacheConfiguration defaultConfig = RedisCacheConfiguration.defaultCacheConfig()
                                .entryTtl(Duration.ofMinutes(10))
                                .disableCachingNullValues()
                                .serializeKeysWith(SerializationPair.fromSerializer(new StringRedisSerializer()))
                                .serializeValuesWith(SerializationPair.fromSerializer(jsonSerializer));

                return RedisCacheManager.builder(cf)
                                .cacheDefaults(defaultConfig)
                                .withCacheConfiguration("users",
                                                defaultConfig.entryTtl(Duration.ofMinutes(5)))
                                .withCacheConfiguration("sessions",
                                                defaultConfig.entryTtl(Duration.ofMinutes(30)))
                                .withCacheConfiguration("divisions",
                                                defaultConfig.entryTtl(Duration.ofHours(1)))
                                .withCacheConfiguration("welfare",
                                                defaultConfig.entryTtl(Duration.ofMinutes(30)))
                                .build();
        }

        /**
         * Redis Keyspace Notification Listener Container.
         * 세션 만료 이벤트를 자동으로 수신하여 후처리합니다.
         * (Redis에서 "notify-keyspace-events Ex" 설정 필요)
         */
        @Bean
        public RedisMessageListenerContainer keyspaceNotificationContainer(RedisConnectionFactory cf) {
                RedisMessageListenerContainer container = new RedisMessageListenerContainer();
                container.setConnectionFactory(cf);
                // 리스너는 SessionExpirationListener에서 등록
                return container;
        }
}
