# Redis 기반 중복 로그인 감지 및 1계정 1기기 정책 고도화

기존 `checkLogin → forceLogin` 2-Step 플로우는 동작하지만, 포트폴리오 수준에서 **전문성**을 보여주기에 부족합니다. 아래 변경을 통해 **보안 아키텍처**, **동시성 안전**, **감사 추적(Audit Trail)** 영역의 전문성을 보강합니다.

---

## 1. Device Fingerprint 기반 세션 메타데이터 확장

Redis Hash `sess:{sid}`에 디바이스 정보를 추가해 "어떤 기기에서 접속 중인지" 메타데이터를 기록합니다. 단순 세션 존재 여부 체크가 아닌, **기기 식별 기반 세션 관리**로 전환합니다.

### [NEW] DeviceInfo.java
- 경로: `domain/session/dto/DeviceInfo.java`

```java
public record DeviceInfo(String ipAddress, String userAgent, String deviceId) {}
```
- `HttpServletRequest`에서 IP, User-Agent를 추출하고 해싱하여 `deviceId` (SHA-256 fingerprint) 생성
- Redis `sess:{sid}` Hash에 `ip`, `ua`, `deviceId` 필드 추가

### [MODIFY] SessionService.java
- `issueSession(userId, role, deviceInfo)` — 디바이스 정보 포함 세션 발급
- `getConflictingDeviceInfo(existingSid)` — 기존 세션의 디바이스 정보를 반환 (팝업 안내용)
- `hasExistingSession` → 기존 세션이 있으면 해당 세션의 **IP/UA** 정보도 함께 반환

---

## 2. Redis Lua Script 기반 Atomic Session Swap

기존 코드는 `invalidateBySid() → issueSession()`이 2개의 독립적 Redis 호출로 분리되어 있어, 경합 상태(race condition)에서 세션 중복이 발생할 수 있습니다.

### [MODIFY] SessionService.java
- `atomicSessionSwap(userId, role, newSid, refreshHash, deviceInfo)` — Lua Script를 통해 기존 세션 무효화 + 새 세션 생성을 **단일 원자적 연산**으로 실행
- `forceKickExistingSession` + `issueSession` 을 하나의 Lua 트랜잭션으로 통합

```lua
-- KEYS[1] = user:{userId}:sid
-- KEYS[2] = sess:{oldSid}
-- KEYS[3] = sess:{newSid}
-- KEYS[4] = sess:invalidated:{oldSid}
-- ARGV[1..n] = 새 세션 데이터
local oldSid = redis.call('GET', KEYS[1])
if oldSid then
    redis.call('SET', KEYS[4], 'true', 'EX', 600)  -- invalidated 마킹
    redis.call('DEL', KEYS[2])                        -- 기존 세션 삭제
end
redis.call('HSET', KEYS[3], unpack(ARGV, 1, #ARGV - 1))
redis.call('EXPIRE', KEYS[3], ARGV[#ARGV])
redis.call('SET', KEYS[1], KEYS[3]:sub(6), 'EX', ARGV[#ARGV])
return oldSid or ''
```

> **IMPORTANT**: Lua Script는 Redis 서버에서 원자적으로 실행되며 직렬화(serialized)가 보장됩니다. 이를 통해 두 기기에서 동시에 로그인 시도해도 **정확히 한 세션만 활성** 상태가 됩니다.

---

## 3. Security Event Audit 로깅 체계

보안 관련 이벤트를 구조화된 형태로 기록합니다. Spring의 `ApplicationEventPublisher`를 활용합니다.

### [NEW] SessionSecurityEvent.java
- 경로: `domain/session/event/SessionSecurityEvent.java`

```java
public record SessionSecurityEvent(
    EventType type,
    Long userId,
    String sessionId,
    DeviceInfo deviceInfo,
    Instant timestamp
) {
    public enum EventType {
        SESSION_CREATED,
        SESSION_INVALIDATED,
        DUPLICATE_LOGIN_DETECTED,
        FORCE_LOGIN_EXECUTED,
        SESSION_EXPIRED
    }
}
```

### [NEW] SessionEventPublisher.java
- 경로: `domain/session/event/SessionEventPublisher.java`
- `ApplicationEventPublisher`를 래핑하여 각 이벤트 타입별 publish 메서드 제공

### [NEW] SessionEventListener.java
- 경로: `domain/session/event/SessionEventListener.java`
- `@EventListener`로 수신하여 **구조화된 보안 감사 로그** 출력
- 포맷: `[SECURITY_AUDIT] type=DUPLICATE_LOGIN_DETECTED userId=42 ip=1.2.3.4 ua=...'`

---

## 4. JwtAuthenticationFilter 세션 무효화 실시간 감지 개선

### [MODIFY] JwtAuthenticationFilter.java
- `sessionService.isActive()` 실패 시, `wasInvalidated(sid)`를 호출하여 **강제 종료된 것인지 vs 만료된 것인지** 구분
- `SESSION_INVALIDATED`인 경우 Response에 `X-Session-Status: KICKED` 커스텀 헤더를 추가하여 프론트엔드에서 "다른 기기에서 로그인" 안내 가능

---

## 5. AuthService 디바이스 정보 전달

### [MODIFY] AuthService.java
- `checkLogin(req, deviceInfo)` — 로그인 체크 시 디바이스 정보 전달
- `forceLogin(loginToken, deviceInfo)` — 강제 로그인 시 디바이스 정보 전달
- `LoginCheckResult`에 기존 세션의 디바이스 정보(`conflictDeviceInfo`) 포함
- 이벤트 퍼블리셔 연동으로 `DUPLICATE_LOGIN_DETECTED`, `FORCE_LOGIN_EXECUTED` 이벤트 발행

---

## 6. AuthController 및 DTO 응답 보강

### [MODIFY] AuthController.java
- `checkLogin`, `forceLogin` 엔드포인트에서 `HttpServletRequest`로부터 `DeviceInfo` 추출하여 서비스에 전달

### [MODIFY] AuthDtos.java
- `LoginCheckResponse`에 `ConflictDeviceInfo` 필드 추가 (기존 세션의 IP 마스킹 + User-Agent 정보)
- `ConflictDeviceInfo` record 추가: `maskedIp`, `deviceSummary`, `lastSeenAt`

---

## 7. 단위 테스트

### [MODIFY] SessionServiceTest.java
추가 테스트 케이스:
- `atomicSessionSwap` — 기존 세션 교체 시 invalidation 마킹 + 새 세션 생성 확인
- `getConflictingDeviceInfo` — 기존 세션의 디바이스 정보 반환 확인
- `issueSession_withDeviceInfo` — 디바이스 정보 포함 세션 발급 확인

### [NEW] AuthServiceTest.java
- `checkLogin_ExistingSession_ReturnsConflictDeviceInfo` — 중복 로그인 시 기존 디바이스 정보 반환
- `forceLogin_InvalidatesOldSession_CreatesNew` — 강제 로그인 시 세션 교체 + 이벤트 발행
- `checkLogin_NoExistingSession_DirectLogin` — 기존 세션 없을 때 직접 로그인

---

## 검증 계획

### 자동 테스트
```bash
cd c:\Users\user\sesac25\SilverLInk-aicc\SilverLink-BE
.\gradlew test --tests "*SessionServiceTest" --tests "*AuthServiceTest" -i
```

### 수동 검증
1. **빌드 성공 확인**: `.\gradlew build -x test`
2. **전체 단위 테스트 통과**: `.\gradlew test -i`
