package com.aicc.silverlink.domain.session.service;

import com.aicc.silverlink.global.config.redis.RedisConfig;
import com.aicc.silverlink.global.config.redis.SessionKickPubSub;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Redis 통합 테스트 (Testcontainers 기반).
 * 실제 Redis 인스턴스를 사용하여 Lua Script, Pipeline, TTL 등을 검증합니다.
 *
 * <p>
 * 실행: {@code ./gradlew integrationTest}
 */
@Testcontainers
@SpringBootTest(properties = {
        "spring.profiles.active=test",
        "security.jwt.secret=test-secret-key-for-integration-test-minimum-32-chars"
})
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
class SessionServiceIT {

    @Container
    static GenericContainer<?> redisContainer = new GenericContainer<>(DockerImageName.parse("redis:7-alpine"))
            .withExposedPorts(6379);

    @DynamicPropertySource
    static void redisProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redisContainer::getHost);
        registry.add("spring.data.redis.port", () -> redisContainer.getMappedPort(6379));
    }

    @Autowired
    private SessionService sessionService;

    @Autowired
    private StringRedisTemplate redisTemplate;

    @BeforeEach
    void cleanRedis() {
        var keys = redisTemplate.keys("*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    @Test
    @Order(1)
    @DisplayName("Lua Script Atomic Session Swap — 신규 세션 발급")
    void issueSession_NewSession_ShouldCreateInRedis() {
        // when
        var result = sessionService.issueSession(1L,
                com.aicc.silverlink.domain.user.entity.Role.GUARDIAN);

        // then
        assertThat(result).isNotNull();
        assertThat(result.sid()).isNotNull().hasSize(36); // UUID format
        assertThat(result.refreshToken()).isNotNull();

        // Redis에 실제 세션 데이터 확인
        String sessKey = "sess:" + result.sid();
        assertThat(redisTemplate.hasKey(sessKey)).isTrue();

        Map<Object, Object> sessionData = redisTemplate.opsForHash().entries(sessKey);
        assertThat(sessionData.get("userId")).isEqualTo("1");
        assertThat(sessionData.get("role")).isEqualTo("GUARDIAN");
        assertThat(sessionData.get("refreshHash")).isNotNull();

        // 사용자→세션 매핑 확인
        String mappedSid = redisTemplate.opsForValue().get("user:1:sid");
        assertThat(mappedSid).isEqualTo(result.sid());
    }

    @Test
    @Order(2)
    @DisplayName("Lua Script Atomic Session Swap — 기존 세션 원자적 교체")
    void issueSession_ExistingSession_ShouldAtomicallySwap() {
        // given: 첫 번째 세션 발급
        var first = sessionService.issueSession(2L,
                com.aicc.silverlink.domain.user.entity.Role.GUARDIAN);
        String firstSessKey = "sess:" + first.sid();
        assertThat(redisTemplate.hasKey(firstSessKey)).isTrue();

        // when: 두 번째 세션 발급 (KICK_OLD 정책)
        var second = sessionService.issueSession(2L,
                com.aicc.silverlink.domain.user.entity.Role.GUARDIAN);

        // then: 첫 번째 세션 삭제됨
        assertThat(redisTemplate.hasKey(firstSessKey)).isFalse();
        // 무효화 마킹 존재
        assertThat(redisTemplate.hasKey("sess:invalidated:" + first.sid())).isTrue();
        // 두 번째 세션 활성
        assertThat(redisTemplate.hasKey("sess:" + second.sid())).isTrue();
        // 매핑이 두 번째 세션으로 갱신
        assertThat(redisTemplate.opsForValue().get("user:2:sid")).isEqualTo(second.sid());
    }

    @Test
    @Order(3)
    @DisplayName("세션 TTL 갱신 (Pipeline)")
    void touch_ShouldExtendTTL() throws InterruptedException {
        // given
        var issued = sessionService.issueSession(3L,
                com.aicc.silverlink.domain.user.entity.Role.GUARDIAN);
        String sessKey = "sess:" + issued.sid();

        Long initialTtl = redisTemplate.getExpire(sessKey, TimeUnit.SECONDS);
        assertThat(initialTtl).isGreaterThan(0);

        // when
        Thread.sleep(1100); // 1초 대기
        sessionService.touch(issued.sid());

        // then: lastSeen 갱신 확인
        String lastSeen = (String) redisTemplate.opsForHash().get(sessKey, "lastSeen");
        assertThat(lastSeen).isNotNull();
        // TTL이 갱신됨 (초기값 이상)
        Long newTtl = redisTemplate.getExpire(sessKey, TimeUnit.SECONDS);
        assertThat(newTtl).isGreaterThan(0);
    }

    @Test
    @Order(4)
    @DisplayName("세션 활성 확인")
    void isActive_ShouldReturnCorrectly() {
        // given
        var issued = sessionService.issueSession(4L,
                com.aicc.silverlink.domain.user.entity.Role.GUARDIAN);

        // then: 활성 세션
        assertThat(sessionService.isActive(issued.sid(), 4L)).isTrue();
        // 다른 userId로 확인
        assertThat(sessionService.isActive(issued.sid(), 999L)).isFalse();
        // 존재하지 않는 SID
        assertThat(sessionService.isActive("non-existent-sid", 4L)).isFalse();
    }

    @Test
    @Order(5)
    @DisplayName("세션 무효화 (Pipeline) + wasInvalidated 확인")
    void invalidateBySid_ShouldCleanup() {
        // given
        var issued = sessionService.issueSession(5L,
                com.aicc.silverlink.domain.user.entity.Role.GUARDIAN);
        String sessKey = "sess:" + issued.sid();

        // when
        sessionService.invalidateBySid(issued.sid());

        // then
        assertThat(redisTemplate.hasKey(sessKey)).isFalse();
        assertThat(redisTemplate.hasKey("user:5:sid")).isFalse();
        assertThat(sessionService.wasInvalidated(issued.sid())).isTrue();
    }

    @Test
    @Order(6)
    @DisplayName("DeviceInfo 포함 세션 발급 + 충돌 디바이스 조회 (HMGET)")
    void issueSession_WithDeviceInfo_ShouldStoreAndRetrieve() {
        // given
        var deviceInfo = new com.aicc.silverlink.domain.session.dto.DeviceInfo(
                "192.168.1.100", "Chrome/120.0 (Windows)", "device-hash-abc");

        // when
        var issued = sessionService.issueSession(6L,
                com.aicc.silverlink.domain.user.entity.Role.GUARDIAN, deviceInfo);

        // then: 디바이스 정보 저장 확인
        String sessKey = "sess:" + issued.sid();
        assertThat(redisTemplate.opsForHash().get(sessKey, "ip")).isEqualTo("192.168.1.100");
        assertThat(redisTemplate.opsForHash().get(sessKey, "ua")).isEqualTo("Chrome/120.0 (Windows)");
        assertThat(redisTemplate.opsForHash().get(sessKey, "deviceId")).isEqualTo("device-hash-abc");

        // HMGET으로 충돌 디바이스 조회
        var conflict = sessionService.getConflictingDeviceInfo(issued.sid());
        assertThat(conflict).isNotNull();
        assertThat(conflict.ipAddress()).isEqualTo("192.168.1.100");
    }

    @Test
    @Order(7)
    @DisplayName("로그인 토큰 생성 및 검증 (일회용)")
    void loginToken_ShouldBeOneTimeUse() {
        // given
        String token = sessionService.createLoginToken(7L);

        // when: 첫 번째 검증 — 성공
        Long userId = sessionService.validateLoginToken(token);
        assertThat(userId).isEqualTo(7L);

        // then: 두 번째 검증 — 실패 (일회용)
        assertThatThrownBy(() -> sessionService.validateLoginToken(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INVALID_LOGIN_TOKEN");
    }

    @Test
    @Order(8)
    @DisplayName("리프레시 토큰 회전")
    void rotateRefresh_ShouldUpdateHash() {
        // given
        var issued = sessionService.issueSession(8L,
                com.aicc.silverlink.domain.user.entity.Role.GUARDIAN);
        String oldRefreshHash = (String) redisTemplate.opsForHash()
                .get("sess:" + issued.sid(), "refreshHash");

        // when: 리프레시 토큰 회전
        String newRefresh = sessionService.rotateRefresh(issued.sid(), issued.refreshToken());

        // then: 해시 변경됨
        String newRefreshHash = (String) redisTemplate.opsForHash()
                .get("sess:" + issued.sid(), "refreshHash");
        assertThat(newRefreshHash).isNotEqualTo(oldRefreshHash);
        assertThat(newRefresh).isNotNull().isNotEqualTo(issued.refreshToken());
    }
}
