package com.aicc.silverlink.domain.session.service;

import com.aicc.silverlink.domain.session.dto.DeviceInfo;
import com.aicc.silverlink.domain.session.event.SessionEventPublisher;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import com.aicc.silverlink.global.config.redis.SessionKickPubSub;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.redis.core.HashOperations;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.data.redis.core.script.RedisScript;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SessionServiceTest {

    @Mock
    private StringRedisTemplate redis;

    @Mock
    private AuthPolicyProperties props;

    @Mock
    private SessionEventPublisher eventPublisher;

    @Mock
    private RedisScript<String> atomicSessionSwapScript;

    @Mock
    private ValueOperations<String, String> valueOps;

    @Mock
    private HashOperations<String, Object, Object> hashOps;

    @Mock
    private SessionKickPubSub sessionKickPubSub;

    @InjectMocks
    private SessionService sessionService;

    @BeforeEach
    void setUp() {
        given(redis.opsForValue()).willReturn(valueOps);
        given(redis.opsForHash()).willReturn(hashOps);
        given(props.getIdleTtlSeconds()).willReturn(3600L);
        given(props.getConcurrentPolicy()).willReturn("KICK_OLD");
    }

    // ========== 기존 세션 확인 테스트 ==========

    @Test
    @DisplayName("기존 세션 확인 - 세션 있음")
    void hasExistingSession_SessionExists() {
        // given
        Long userId = 1L;
        String existingSid = "existing-sid-123";
        String userKey = "user:1:sid";
        String sessKey = "sess:existing-sid-123";

        given(valueOps.get(userKey)).willReturn(existingSid);
        given(redis.hasKey(sessKey)).willReturn(true);

        // when
        String result = sessionService.hasExistingSession(userId);

        // then
        assertThat(result).isEqualTo(existingSid);
        verify(valueOps).get(userKey);
        verify(redis).hasKey(sessKey);
    }

    @Test
    @DisplayName("기존 세션 확인 - 세션 없음")
    void hasExistingSession_NoSession() {
        // given
        Long userId = 1L;
        String userKey = "user:1:sid";

        given(valueOps.get(userKey)).willReturn(null);

        // when
        String result = sessionService.hasExistingSession(userId);

        // then
        assertThat(result).isNull();
        verify(valueOps).get(userKey);
        verify(redis, never()).hasKey(anyString());
    }

    @Test
    @DisplayName("기존 세션 확인 - SID는 있지만 세션 데이터 없음")
    void hasExistingSession_SidExistsButSessionExpired() {
        // given
        Long userId = 1L;
        String existingSid = "expired-sid-456";
        String userKey = "user:1:sid";
        String sessKey = "sess:expired-sid-456";

        given(valueOps.get(userKey)).willReturn(existingSid);
        given(redis.hasKey(sessKey)).willReturn(false); // 세션 만료됨

        // when
        String result = sessionService.hasExistingSession(userId);

        // then
        assertThat(result).isNull();
        verify(valueOps).get(userKey);
        verify(redis).hasKey(sessKey);
    }

    // ========== 기존 세션 강제 종료 테스트 ==========

    @Test
    @DisplayName("기존 세션 강제 종료 - 세션 있음")
    void forceKickExistingSession_SessionExists() {
        // given
        Long userId = 1L;
        String existingSid = "existing-sid-123";
        String userKey = "user:1:sid";
        String sessKey = "sess:existing-sid-123";

        given(valueOps.get(userKey)).willReturn(existingSid);
        given(redis.hasKey(sessKey)).willReturn(true);
        given(hashOps.get(sessKey, "userId")).willReturn("1");

        // when
        sessionService.forceKickExistingSession(userId);

        // then
        verify(redis).executePipelined(any(RedisCallback.class));
        verify(sessionKickPubSub).publishSessionKicked(userId, existingSid);
    }

    @Test
    @DisplayName("기존 세션 강제 종료 - 세션 없음 (아무 작업 안 함)")
    void forceKickExistingSession_NoSession() {
        // given
        Long userId = 1L;
        String userKey = "user:1:sid";

        given(valueOps.get(userKey)).willReturn(null);

        // when
        sessionService.forceKickExistingSession(userId);

        // then
        verify(redis, never()).delete(startsWith("sess:"));
    }

    // ========== 로그인 토큰 생성 테스트 ==========

    @Test
    @DisplayName("로그인 토큰 생성 성공")
    void createLoginToken_Success() {
        // given
        Long userId = 1L;

        // when
        String token = sessionService.createLoginToken(userId);

        // then
        assertThat(token).isNotNull();
        assertThat(token).hasSize(36); // UUID 형식
        verify(valueOps).set(eq("login:pending:" + token), eq("1"), eq(5L), eq(TimeUnit.MINUTES));
    }

    @Test
    @DisplayName("로그인 토큰 생성 - 여러 번 호출 시 다른 토큰 생성")
    void createLoginToken_DifferentTokensEachTime() {
        // given
        Long userId = 1L;

        // when
        String token1 = sessionService.createLoginToken(userId);
        String token2 = sessionService.createLoginToken(userId);

        // then
        assertThat(token1).isNotEqualTo(token2);
    }

    // ========== 로그인 토큰 검증 테스트 ==========

    @Test
    @DisplayName("로그인 토큰 검증 성공 - 유효한 토큰")
    void validateLoginToken_Success() {
        // given
        String token = "valid-token-123";
        String key = "login:pending:valid-token-123";

        given(valueOps.get(key)).willReturn("1");

        // when
        Long userId = sessionService.validateLoginToken(token);

        // then
        assertThat(userId).isEqualTo(1L);
        verify(valueOps).get(key);
        verify(redis).delete(key); // 일회용이므로 삭제
    }

    @Test
    @DisplayName("로그인 토큰 검증 실패 - 만료된 토큰")
    void validateLoginToken_Fail_ExpiredToken() {
        // given
        String token = "expired-token-456";
        String key = "login:pending:expired-token-456";

        given(valueOps.get(key)).willReturn(null); // 만료됨

        // when & then
        assertThatThrownBy(() -> sessionService.validateLoginToken(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INVALID_LOGIN_TOKEN");

        verify(redis, never()).delete(anyString());
    }

    @Test
    @DisplayName("로그인 토큰 검증 실패 - 유효하지 않은 토큰")
    void validateLoginToken_Fail_InvalidToken() {
        // given
        String token = "invalid-token-789";
        String key = "login:pending:invalid-token-789";

        given(valueOps.get(key)).willReturn(null);

        // when & then
        assertThatThrownBy(() -> sessionService.validateLoginToken(token))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("INVALID_LOGIN_TOKEN");
    }

    // ========== 세션 발급 테스트 (Lua Script Atomic Swap) ==========

    @Test
    @DisplayName("세션 발급 - 기존 세션 없음 (Lua Script로 신규 발급)")
    void issueSession_NoExistingSession() {
        // given
        Long userId = 1L;
        Role role = Role.GUARDIAN;
        String userKey = "user:1:sid";

        given(valueOps.get(userKey)).willReturn(null); // 기존 세션 없음
        given(redis.execute(
                any(RedisScript.class), anyList(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn("");

        // when
        SessionService.SessionIssue result = sessionService.issueSession(userId, role);

        // then
        assertThat(result).isNotNull();
        assertThat(result.sid()).isNotNull();
        assertThat(result.refreshToken()).isNotNull();
        verify(redis).execute(
                any(RedisScript.class), anyList(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any());
        verify(eventPublisher).publishSessionCreated(eq(userId), anyString(), isNull());
    }

    @Test
    @DisplayName("세션 발급 - DeviceInfo 포함 발급")
    void issueSession_WithDeviceInfo() {
        // given
        Long userId = 1L;
        Role role = Role.GUARDIAN;
        String userKey = "user:1:sid";
        DeviceInfo deviceInfo = new DeviceInfo("192.168.1.100", "Chrome/120.0", "abc123");

        given(valueOps.get(userKey)).willReturn(null);
        given(redis.execute(any(RedisScript.class), anyList(), any())).willReturn("");

        // when
        SessionService.SessionIssue result = sessionService.issueSession(userId, role, deviceInfo);

        // then
        assertThat(result).isNotNull();
        assertThat(result.sid()).isNotNull();
        verify(eventPublisher).publishSessionCreated(eq(userId), anyString(), eq(deviceInfo));
    }

    @Test
    @DisplayName("세션 발급 - KICK_OLD 정책 (Lua Script가 기존 세션 원자적 교체)")
    void issueSession_KickOld_ExistingSession() {
        // given
        Long userId = 1L;
        Role role = Role.GUARDIAN;
        String userKey = "user:1:sid";
        String existingSid = "old-sid-123";
        String sessKey = "sess:old-sid-123";

        given(props.getConcurrentPolicy()).willReturn("KICK_OLD");
        given(valueOps.get(userKey)).willReturn(existingSid);
        given(redis.hasKey(sessKey)).willReturn(true);
        // Lua Script가 기존 SID 반환
        given(redis.execute(
                any(RedisScript.class), anyList(),
                any(), any(), any(), any(), any(), any(), any(), any(), any(), any()))
                .willReturn(existingSid);

        // when
        SessionService.SessionIssue result = sessionService.issueSession(userId, role);

        // then
        assertThat(result).isNotNull();
        verify(eventPublisher).publishSessionInvalidated(eq(userId), eq(existingSid), isNull());
        verify(eventPublisher).publishSessionCreated(eq(userId), anyString(), isNull());
    }

    @Test
    @DisplayName("세션 발급 - BLOCK_NEW 정책 (기존 세션 있으면 차단)")
    void issueSession_BlockNew_ExistingSession() {
        // given
        Long userId = 1L;
        Role role = Role.GUARDIAN;
        String userKey = "user:1:sid";
        String existingSid = "existing-sid-456";
        String sessKey = "sess:existing-sid-456";

        given(props.getConcurrentPolicy()).willReturn("BLOCK_NEW");
        given(valueOps.get(userKey)).willReturn(existingSid);
        given(redis.hasKey(sessKey)).willReturn(true);

        // when & then
        assertThatThrownBy(() -> sessionService.issueSession(userId, role))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("ALREADY_LOGGED_IN");

        verify(redis, never()).execute(any(RedisScript.class), anyList(), any());
    }

    // ========== 세션 활성 확인 테스트 ==========

    @Test
    @DisplayName("세션 활성 확인 - 활성 세션")
    void isActive_ActiveSession() {
        // given
        String sid = "active-sid-123";
        Long userId = 1L;
        String sessKey = "sess:active-sid-123";
        String userKey = "user:1:sid";

        given(redis.hasKey(sessKey)).willReturn(true);
        given(valueOps.get(userKey)).willReturn(sid);

        // when
        boolean result = sessionService.isActive(sid, userId);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("세션 활성 확인 - 세션 만료")
    void isActive_ExpiredSession() {
        // given
        String sid = "expired-sid-456";
        Long userId = 1L;
        String sessKey = "sess:expired-sid-456";

        given(redis.hasKey(sessKey)).willReturn(false);

        // when
        boolean result = sessionService.isActive(sid, userId);

        // then
        assertThat(result).isFalse();
    }

    @Test
    @DisplayName("세션 활성 확인 - 다른 사용자의 세션")
    void isActive_DifferentUserSession() {
        // given
        String sid = "other-sid-789";
        Long userId = 1L;
        String sessKey = "sess:other-sid-789";
        String userKey = "user:1:sid";

        given(redis.hasKey(sessKey)).willReturn(true);
        given(valueOps.get(userKey)).willReturn("different-sid-999"); // 다른 SID

        // when
        boolean result = sessionService.isActive(sid, userId);

        // then
        assertThat(result).isFalse();
    }

    // ========== 충돌 디바이스 정보 테스트 ==========

    @Test
    @DisplayName("충돌 디바이스 정보 조회 - 디바이스 정보 있음")
    void getConflictingDeviceInfo_WithDeviceInfo() {
        // given
        String existingSid = "existing-sid-123";
        String sessKey = "sess:existing-sid-123";

        given(redis.hasKey(sessKey)).willReturn(true);
        given(hashOps.multiGet(sessKey, List.of("ip", "ua", "deviceId")))
                .willReturn(List.of("192.168.1.100", "Chrome/120.0 Windows", "device-hash-abc"));

        // when
        DeviceInfo result = sessionService.getConflictingDeviceInfo(existingSid);

        // then
        assertThat(result).isNotNull();
        assertThat(result.ipAddress()).isEqualTo("192.168.1.100");
        assertThat(result.userAgent()).isEqualTo("Chrome/120.0 Windows");
        assertThat(result.deviceId()).isEqualTo("device-hash-abc");
    }

    @Test
    @DisplayName("충돌 디바이스 정보 조회 - 디바이스 정보 없음")
    void getConflictingDeviceInfo_NoDeviceInfo() {
        // given
        String existingSid = "existing-sid-456";
        String sessKey = "sess:existing-sid-456";

        given(redis.hasKey(sessKey)).willReturn(true);
        given(hashOps.multiGet(sessKey, List.of("ip", "ua", "deviceId")))
                .willReturn(Arrays.asList(null, null, null));

        // when
        DeviceInfo result = sessionService.getConflictingDeviceInfo(existingSid);

        // then
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("충돌 디바이스 정보 조회 - 세션 만료됨")
    void getConflictingDeviceInfo_SessionExpired() {
        // given
        String existingSid = "expired-sid-789";
        String sessKey = "sess:expired-sid-789";

        given(redis.hasKey(sessKey)).willReturn(false);

        // when
        DeviceInfo result = sessionService.getConflictingDeviceInfo(existingSid);

        // then
        assertThat(result).isNull();
    }

    // ========== 세션 무효화 확인 테스트 ==========

    @Test
    @DisplayName("세션 강제 종료 여부 확인 - 강제 종료됨")
    void wasInvalidated_True() {
        // given
        String sid = "invalidated-sid-123";
        given(redis.hasKey("sess:invalidated:invalidated-sid-123")).willReturn(true);

        // when
        boolean result = sessionService.wasInvalidated(sid);

        // then
        assertThat(result).isTrue();
    }

    @Test
    @DisplayName("세션 강제 종료 여부 확인 - 강제 종료 안 됨")
    void wasInvalidated_False() {
        // given
        String sid = "normal-sid-456";
        given(redis.hasKey("sess:invalidated:normal-sid-456")).willReturn(false);

        // when
        boolean result = sessionService.wasInvalidated(sid);

        // then
        assertThat(result).isFalse();
    }
}
