package com.aicc.silverlink.domain.session.service;

import com.aicc.silverlink.domain.session.dto.DeviceInfo;
import com.aicc.silverlink.domain.session.event.SessionEventPublisher;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import com.aicc.silverlink.global.config.redis.SessionKickPubSub;
import io.github.resilience4j.circuitbreaker.annotation.CircuitBreaker;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.RedisConnectionFailureException;
import org.springframework.data.redis.core.RedisCallback;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Redis 기반 세션 관리 서비스.
 *
 * <h3>고도화 적용 사항:</h3>
 * <ul>
 * <li>Lua Script Atomic Session Swap — 동시성 안전</li>
 * <li>Redis Pipeline — touch(), invalidateBySid() 네트워크 최적화</li>
 * <li>HMGET — 다중 필드 1회 왕복 조회</li>
 * <li>Circuit Breaker — Redis 장애 시 Graceful Degradation</li>
 * <li>Pub/Sub — 세션 킥 실시간 알림 (멀티 인스턴스)</li>
 * <li>Security Event — 감사 로그 발행</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SessionService {

    private final StringRedisTemplate redis;
    private final AuthPolicyProperties props;
    private final SessionEventPublisher eventPublisher;
    private final RedisScript<String> atomicSessionSwapScript;
    private final SessionKickPubSub sessionKickPubSub;

    // ==================== Key 생성 ====================

    private static String userSidKey(Long userId) {
        return "user:" + userId + ":sid";
    }

    private static String sessKey(String sid) {
        return "sess:" + sid;
    }

    private static String invalidatedKey(String sid) {
        return "sess:invalidated:" + sid;
    }

    // ==================== DTO ====================

    public record SessionIssue(String sid, String refreshToken) {
    }

    // ==================== 세션 발급 ====================

    /**
     * 세션 발급 (기존 호환 — DeviceInfo 없이 호출 가능)
     */
    public SessionIssue issueSession(Long userId, Role role) {
        return issueSession(userId, role, null);
    }

    /**
     * 세션 발급 (DeviceInfo 포함, Atomic Session Swap 적용).
     * Lua Script를 통해 기존 세션 무효화 + 새 세션 생성을 원자적으로 수행합니다.
     * Circuit Breaker가 적용되어 Redis 장애 시 안전하게 실패합니다.
     */
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "issueSessionFallback")
    public SessionIssue issueSession(Long userId, Role role, DeviceInfo deviceInfo) {
        long idleSeconds = props.getIdleTtlSeconds();
        String policy = props.getConcurrentPolicy();

        String userKey = userSidKey(userId);
        String existingSid = redis.opsForValue().get(userKey);

        // BLOCK_NEW 정책: 기존 세션 있으면 차단
        if (existingSid != null && Boolean.TRUE.equals(redis.hasKey(sessKey(existingSid)))) {
            if ("BLOCK_NEW".equalsIgnoreCase(policy)) {
                throw new IllegalStateException("ALREADY_LOGGED_IN");
            }
        }

        String sid = UUID.randomUUID().toString();
        String refresh = UUID.randomUUID().toString().replace("-", "") + UUID.randomUUID().toString().replace("-", "");
        long now = Instant.now().getEpochSecond();

        // 세션 데이터 구성 (DeviceInfo 포함)
        List<String> sessionFields = new ArrayList<>();
        sessionFields.addAll(List.of(
                "userId", String.valueOf(userId),
                "role", role.name(),
                "refreshHash", sha256(refresh),
                "lastSeen", String.valueOf(now)));

        if (deviceInfo != null) {
            sessionFields.addAll(List.of(
                    "ip", deviceInfo.ipAddress(),
                    "ua", deviceInfo.userAgent(),
                    "deviceId", deviceInfo.deviceId(),
                    "loginAt", String.valueOf(now)));
        }

        // Lua Script로 원자적 세션 교체
        List<String> keys = List.of(userKey, sessKey(sid));
        List<String> args = new ArrayList<>();
        args.add(sid);
        args.add(String.valueOf(idleSeconds));
        args.addAll(sessionFields);

        String oldSid = redis.execute(atomicSessionSwapScript, keys, (Object[]) args.toArray(new String[0]));

        // 이벤트 발행 + Pub/Sub 알림
        if (oldSid != null && !oldSid.isEmpty()) {
            eventPublisher.publishSessionInvalidated(userId, oldSid, deviceInfo);
            sessionKickPubSub.publishSessionKicked(userId, oldSid);
            log.info("[SESSION] 기존 세션 교체: userId={}, oldSid={}, newSid={}", userId, oldSid, sid);
        }
        eventPublisher.publishSessionCreated(userId, sid, deviceInfo);

        return new SessionIssue(sid, refresh);
    }

    /**
     * issueSession Circuit Breaker Fallback.
     * Redis 장애 시 로그인 불가 안내.
     */
    @SuppressWarnings("unused")
    private SessionIssue issueSessionFallback(Long userId, Role role, DeviceInfo deviceInfo, Throwable t) {
        log.error("[CB_FALLBACK] issueSession 실패 — Redis 장애: userId={}, error={}", userId, t.getMessage());
        throw new IllegalStateException("SERVICE_TEMPORARILY_UNAVAILABLE");
    }

    // ==================== 세션 조회 ====================

    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "getSessionMetaFallback")
    public Map<String, String> getSessionMeta(String sid) {
        if (Boolean.FALSE.equals(redis.hasKey(sessKey(sid)))) {
            if (wasInvalidated(sid)) {
                throw new IllegalStateException("SESSION_INVALIDATED");
            }
            throw new IllegalStateException("SESSION_EXPIRED");
        }
        return redis.<String, String>opsForHash().entries(sessKey(sid));
    }

    @SuppressWarnings("unused")
    private Map<String, String> getSessionMetaFallback(String sid, Throwable t) {
        log.error("[CB_FALLBACK] getSessionMeta 실패: sid={}, error={}", sid, t.getMessage());
        throw new IllegalStateException("SESSION_SERVICE_UNAVAILABLE");
    }

    /**
     * 기존 세션의 디바이스 정보를 반환합니다 (충돌 안내용).
     * 단일 HMGET으로 3개 필드를 1회 왕복으로 조회합니다.
     */
    public DeviceInfo getConflictingDeviceInfo(String existingSid) {
        if (existingSid == null)
            return null;
        String sessKey = sessKey(existingSid);

        if (Boolean.FALSE.equals(redis.hasKey(sessKey)))
            return null;

        // 3개 개별 hGet → 1회 hmGet으로 최적화
        List<Object> values = redis.opsForHash().multiGet(sessKey, List.of("ip", "ua", "deviceId"));
        String ip = (String) values.get(0);
        String ua = (String) values.get(1);
        String deviceId = (String) values.get(2);

        if (ip == null && ua == null)
            return null;

        return new DeviceInfo(
                ip != null ? ip : "unknown",
                ua != null ? ua : "unknown",
                deviceId != null ? deviceId : "unknown");
    }

    // ==================== 세션 갱신 ====================

    /**
     * 세션 TTL 갱신 (Sliding Window).
     * Redis Pipeline으로 3개 명령을 1회 네트워크 왕복으로 처리합니다.
     */
    public void touch(String sid) {
        if (sid == null)
            return;
        String sessionKey = sessKey(sid);

        if (Boolean.FALSE.equals(redis.hasKey(sessionKey)))
            return;

        long idleSeconds = props.getIdleTtlSeconds();
        byte[] lastSeenValue = String.valueOf(Instant.now().getEpochSecond()).getBytes(StandardCharsets.UTF_8);

        // userId를 먼저 가져와야 Pipeline에서 사용 가능
        Object userIdObj = redis.opsForHash().get(sessionKey, "userId");

        // Pipeline: lastSeen 갱신 + sessionKey TTL + userSidKey TTL을 1회 왕복
        redis.executePipelined((RedisCallback<?>) connection -> {
            byte[] sKey = sessionKey.getBytes(StandardCharsets.UTF_8);
            connection.hashCommands().hSet(sKey, "lastSeen".getBytes(StandardCharsets.UTF_8), lastSeenValue);
            connection.keyCommands().expire(sKey, idleSeconds);
            if (userIdObj != null) {
                byte[] uKey = userSidKey(Long.valueOf((String) userIdObj)).getBytes(StandardCharsets.UTF_8);
                connection.keyCommands().expire(uKey, idleSeconds);
            }
            return null;
        });
    }

    // ==================== 세션 검증 ====================

    /**
     * 세션 활성 여부 확인.
     * Circuit Breaker 적용 — Redis 장애 시 세션 무효로 처리 (안전 우선).
     */
    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "isActiveFallback")
    public boolean isActive(String sid, Long userId) {
        if (sid == null)
            return false;

        if (Boolean.FALSE.equals(redis.hasKey(sessKey(sid))))
            return false;

        String mappedSid = redis.opsForValue().get(userSidKey(userId));
        return sid.equals(mappedSid);
    }

    /**
     * isActive Circuit Breaker Fallback.
     * Redis 장애 시 세션 무효로 처리하여 보안 유지.
     */
    @SuppressWarnings("unused")
    private boolean isActiveFallback(String sid, Long userId, Throwable t) {
        log.warn("[CB_FALLBACK] isActive 실패 — Redis 장애, 세션 무효 처리: sid={}, error={}", sid, t.getMessage());
        return false;
    }

    // ==================== 리프레시 토큰 ====================

    @CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "rotateRefreshFallback")
    public String rotateRefresh(String sid, String presentedRefresh) {
        String sessionKey = sessKey(sid);

        if (Boolean.FALSE.equals(redis.hasKey(sessionKey))) {
            throw new IllegalStateException("SESSION_EXPIRED");
        }

        String savedHash = (String) redis.opsForHash().get(sessionKey, "refreshHash");
        if (savedHash == null || !savedHash.equals(sha256(presentedRefresh))) {
            invalidateBySid(sid);
            throw new IllegalStateException("REFRESH_REUSED");
        }

        String newRefresh = UUID.randomUUID().toString().replace("-", "")
                + UUID.randomUUID().toString().replace("-", "");
        redis.opsForHash().put(sessionKey, "refreshHash", sha256(newRefresh));

        touch(sid);

        return newRefresh;
    }

    @SuppressWarnings("unused")
    private String rotateRefreshFallback(String sid, String presentedRefresh, Throwable t) {
        log.error("[CB_FALLBACK] rotateRefresh 실패: sid={}, error={}", sid, t.getMessage());
        throw new IllegalStateException("SERVICE_TEMPORARILY_UNAVAILABLE");
    }

    // ==================== 세션 무효화 ====================

    /**
     * 세션 강제 종료.
     * Pipeline으로 마킹 + 삭제 + 매핑 삭제를 1회 왕복으로 처리합니다.
     */
    public void invalidateBySid(String sid) {
        if (sid == null)
            return;
        String sessionKey = sessKey(sid);

        // userId를 먼저 가져와야 Pipeline에서 사용 가능
        String userId = (String) redis.opsForHash().get(sessionKey, "userId");

        // Pipeline: 강제 종료 마킹 + 세션 삭제 + 사용자 매핑 삭제를 1회 왕복
        redis.executePipelined((RedisCallback<?>) connection -> {
            // 강제 종료 마킹 (10분간 유지)
            byte[] invKey = invalidatedKey(sid).getBytes(StandardCharsets.UTF_8);
            connection.stringCommands().setEx(invKey, 600, "true".getBytes(StandardCharsets.UTF_8));
            // 세션 삭제
            connection.keyCommands().del(sessionKey.getBytes(StandardCharsets.UTF_8));
            // 사용자 매핑 삭제
            if (userId != null) {
                connection.keyCommands().del(userSidKey(Long.valueOf(userId)).getBytes(StandardCharsets.UTF_8));
            }
            return null;
        });
    }

    /**
     * 세션이 강제 종료되었는지 확인
     */
    public boolean wasInvalidated(String sid) {
        return Boolean.TRUE.equals(redis.hasKey(invalidatedKey(sid)));
    }

    // ==================== 기존 세션 관리 ====================

    /**
     * 사용자의 기존 세션 존재 여부 확인
     */
    public String hasExistingSession(Long userId) {
        String userKey = userSidKey(userId);
        String existingSid = redis.opsForValue().get(userKey);

        if (existingSid != null && Boolean.TRUE.equals(redis.hasKey(sessKey(existingSid)))) {
            return existingSid;
        }
        return null;
    }

    /**
     * 기존 세션 강제 종료
     */
    public void forceKickExistingSession(Long userId) {
        String existingSid = hasExistingSession(userId);
        if (existingSid != null) {
            invalidateBySid(existingSid);
            sessionKickPubSub.publishSessionKicked(userId, existingSid);
        }
    }

    // ==================== 로그인 토큰 ====================

    /**
     * 로그인 확인용 임시 토큰 생성 (5분 유효)
     */
    public String createLoginToken(Long userId) {
        String token = UUID.randomUUID().toString();
        String key = "login:pending:" + token;
        redis.opsForValue().set(key, String.valueOf(userId), 5, TimeUnit.MINUTES);
        return token;
    }

    /**
     * 로그인 토큰 검증 및 사용자 ID 반환 (일회용)
     */
    public Long validateLoginToken(String token) {
        String key = "login:pending:" + token;
        String userId = redis.opsForValue().get(key);
        if (userId == null) {
            throw new IllegalStateException("INVALID_LOGIN_TOKEN");
        }
        redis.delete(key); // 일회용이므로 즉시 삭제
        return Long.valueOf(userId);
    }

    // ==================== 유틸리티 ====================

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}