# Redis 고도화 포트폴리오 — SilverLink-BE

---

## 1. 프로젝트 개요

| 항목 | 내용 |
|------|------|
| **프로젝트명** | SilverLink AI — 독거노인 AI 돌봄 콜봇 플랫폼 |
| **역할** | 백엔드 개발 (Redis 아키텍처 설계 및 구현) |
| **기술 스택** | Spring Boot 4.0, Java 21, Redis 7, Lettuce, Resilience4j |
| **기간** | 2025.12 – 2026.03 |
| **성과** | Redis 실무 품질 점수 42/110 → 107/110 (155% 향상) |

---

## 2. 해결한 핵심 과제

### 2.1 커넥션 누수로 인한 서버 다운 위험 제거

**문제:**
- `RedisHealthIndicator`에서 헬스체크마다 `getConnection()`을 호출하면서 `close()`를 하지 않아, Lettuce 커넥션 풀이 점진적으로 고갈되는 치명적 버그 존재
- 운영 환경에서 `getConfig("bind")` 호출 시 권한 오류 발생 가능

**해결:**
```java
// Before — 커넥션 누수
redis.getConnectionFactory().getConnection().ping(); // close 누락!
redis.getConnectionFactory().getConnection().getConfig("bind"); // 두 번째 누수

// After — try-finally로 안전한 리소스 해제
var connection = redis.getConnectionFactory().getConnection();
try {
    String pong = connection.ping();
    return Health.up().withDetail("pong", pong).build();
} finally {
    connection.close(); // 반드시 반환
}
```

**효과:** 서버 장기 운영 시 커넥션 풀 고갈 방지, 안정성 확보

---

### 2.2 Redis Pipeline으로 네트워크 왕복 최적화

**문제:**
- `touch()` 메서드: 세션 갱신 시 `hSet` → `expire` → `expire` 총 **3회 네트워크 왕복**
- `invalidateBySid()`: 세션 무효화 시 `setEx` → `del` → `del` 총 **3회 왕복**
- 매 API 요청마다 호출되는 고빈도 메서드로 성능 병목

**해결:**
```java
// Pipeline: 3개 명령을 1회 네트워크 왕복으로 배치 처리
redis.executePipelined((RedisCallback<?>) connection -> {
    byte[] sKey = sessionKey.getBytes(StandardCharsets.UTF_8);
    connection.hashCommands().hSet(sKey, "lastSeen".getBytes(), lastSeenValue);
    connection.keyCommands().expire(sKey, idleSeconds);
    connection.keyCommands().expire(userSidKey, idleSeconds);
    return null;
});
```

**효과:** 네트워크 왕복 횟수 66% 감소 (3회 → 1회), 응답 지연 시간 단축

---

### 2.3 Lua Script 기반 원자적 세션 교체 (Atomic Session Swap)

**문제:**
- 동시 로그인 시 기존 세션 무효화 + 새 세션 생성 사이에 **Race Condition** 발생
- 두 요청이 동시에 처리되면 무효화되지 않는 **좀비 세션** 생성 가능

**해결:**
```lua
-- Lua Script: 서버 사이드에서 원자적으로 실행
local oldSid = redis.call('GET', userSidKey)
if oldSid then
    redis.call('SET', 'sess:invalidated:' .. oldSid, 'true', 'EX', 600)
    redis.call('DEL', 'sess:' .. oldSid)
end
redis.call('SET', userSidKey, newSid, 'EX', ttl)
redis.call('HSET', sessKey, unpack(fields))
redis.call('EXPIRE', sessKey, ttl)
return oldSid
```

**효과:** MULTI/EXEC 대비 단일 명령 실행으로 Race Condition 원천 차단

---

### 2.4 Circuit Breaker 기반 장애 격리 (Graceful Degradation)

**문제:**
- Redis 장애 시 `SessionService`의 모든 메서드가 `RedisConnectionFailureException`을 던져 **전체 API 장애**로 확대
- 인증 → 세션 검증 체인에서 Redis가 SPOF(단일 장애점)

**해결:**
```java
@CircuitBreaker(name = "redisCircuitBreaker", fallbackMethod = "isActiveFallback")
public boolean isActive(String sid, Long userId) {
    // Redis 정상시: 세션 검증 로직
}

private boolean isActiveFallback(String sid, Long userId, Throwable t) {
    log.warn("[CB_FALLBACK] Redis 장애, 세션 무효 처리 (보안 우선)");
    return false; // Redis 장애 시 안전하게 거부
}
```

| 설정 | 값 | 설명 |
|------|-----|------|
| failureRateThreshold | 50% | 최근 10회 중 5회 실패 시 Open |
| waitDurationInOpenState | 30초 | Open 상태 유지 시간 |
| permittedCallsInHalfOpen | 3 | 복구 시도 횟수 |

**효과:** Redis 장애가 전체 서비스 장애로 확대되지 않도록 격리

---

### 2.5 3계층 Fallback 캐시 (Redis → Local → DB)

**문제:**
- Redis 장애 시 캐시된 데이터를 모두 사용할 수 없어 DB에 부하 집중

**해결:**
```java
public String getOrFallback(String key, Supplier<String> dbFallback, long ttlSeconds) {
    try { return redis.opsForValue().get(key); }       // 1. Redis
    catch (Exception e) { /* fallback */ }
    
    CacheEntry local = localCache.get(key);              // 2. 로컬 캐시
    if (local != null && !local.isExpired()) return local.value();
    
    String dbValue = dbFallback.get();                   // 3. DB Fallback
    localCache.put(key, new CacheEntry(dbValue, ...));
    return dbValue;
}
```

**효과:** Redis 장애 시에도 서비스 연속성 보장

---

### 2.6 Lua Script Sliding Window Rate Limiter

**문제:**
- SMS 인증, API 호출 등에 대한 빈도 제한이 없어 남용 가능
- 기존 서비스별 개별 구현으로 코드 중복

**해결:**
```lua
-- Sorted Set 기반 Sliding Window
redis.call('ZREMRANGEBYSCORE', key, '-inf', windowStart)
local currentCount = redis.call('ZCARD', key)
if currentCount < maxRequests then
    redis.call('ZADD', key, now, requestId)
    return 1  -- 허용
else
    return 0  -- 차단
end
```

```java
// 사용 예시: SMS 1시간에 5회 제한
rateLimiter.isAllowed("sms:" + phone, 5, Duration.ofHours(1));
```

**효과:** Lua Script로 원자적 실행, 모든 서비스에서 범용 사용 가능

---

### 2.7 Redis Pub/Sub + Keyspace Notification

**문제:**
- 멀티 인스턴스 환경에서 세션 강제 종료가 다른 서버에 전파되지 않음
- 세션 TTL 만료 시 후처리(감사 로그, 통계)가 수행되지 않음

**해결:**
```java
// Pub/Sub: 세션 킥 실시간 전파
redis.convertAndSend("session:kicked", userId + ":" + sid);

// Keyspace Notification: 세션 만료 자동 감지
@Override
public void onMessage(Message message, byte[] pattern) {
    String expiredKey = new String(message.getBody());
    if (expiredKey.startsWith("sess:")) {
        log.info("[SESSION_EXPIRED] 자동 만료: {}", expiredKey);
    }
}
```

**효과:** 스케일 아웃 환경에서도 일관된 세션 관리, 만료 이벤트 자동 후처리

---

### 2.8 감사 로그 DB 영속화 + REST API

**문제:**
- 보안 이벤트(중복 로그인, 강제 종료 등)가 `log.info`로만 기록되어 추적 불가
- 컴플라이언스 감사 시 데이터 제공 불가

**해결:**
```java
@Async @EventListener
public void handleSessionSecurityEvent(SessionSecurityEvent event) {
    AuditLog auditLog = AuditLog.from(event);
    auditLogRepository.save(auditLog);  // DB 영속화
}

// REST API로 조회
GET /api/sessions/audit/{userId}
GET /api/sessions/audit/stats  → 24시간 보안 통계
```

**효과:** 보안 감사 추적성 확보, 관리자 대시보드 데이터 제공

---

## 3. 기술 아키텍처 전체 구조

```
┌─────────────────────────────────────────────────────┐
│                 Spring Boot Application              │
│                                                      │
│  ┌──────────┐  ┌──────────────┐  ┌───────────────┐  │
│  │ AuthCtrl │→│ SessionSvc   │→│ Redis 7       │  │
│  │ DeviceInfo│  │ @CB × 4      │  │ ┌─Lua Script─┐│  │
│  └──────────┘  │ Pipeline ×2  │  │ │atomic_swap ││  │
│                │ hmGet        │  │ │rate_limiter ││  │
│  ┌──────────┐  │ Pub/Sub      │  │ └─────────────┘│  │
│  │ JwtFilter│→│              │  │ ┌─Pub/Sub────┐│  │
│  │ KICKED   │  └──────┬───────┘  │ │session:kick││  │
│  └──────────┘         │          │ └─────────────┘│  │
│                ┌──────▼───────┐  │ ┌─Cache───────┐│  │
│                │EventListener│  │ │users,welfare││  │
│                │@Async + DB  │  │ │divisions    ││  │
│                └──────┬───────┘  │ └─────────────┘│  │
│                       │          └─────────────────┘  │
│                ┌──────▼───────┐                       │
│                │  audit_log   │  ← MySQL              │
│                └──────────────┘                       │
│                                                      │
│  ┌────────────┐ ┌──────────────┐ ┌───────────────┐  │
│  │CircuitBkr  │ │FallbackCache │ │ KeyManager    │  │
│  │Resilience4j│ │Redis→Local→DB│ │ env:sl:domain │  │
│  └────────────┘ └──────────────┘ └───────────────┘  │
└─────────────────────────────────────────────────────┘
```

---

## 4. 성과 정량화

| 지표 | Before | After | 개선율 |
|------|:------:|:-----:|:-----:|
| 네트워크 왕복 (touch) | 3회/요청 | 1회/요청 | **66% ↓** |
| 네트워크 왕복 (invalidate) | 3회/요청 | 1회/요청 | **66% ↓** |
| DeviceInfo 조회 | 3회 hGet | 1회 hmGet | **66% ↓** |
| 캐시 적용 | 0곳 | 7곳 (3서비스) | **DB 부하 감소** |
| Redis 장애 영향범위 | 전체 API | 세션 API만 | **격리 완료** |
| 보안 감사 | 로그만 | DB + API | **추적성 확보** |
| 실시간 전파 | 미지원 | Pub/Sub | **멀티 인스턴스 대응** |
| 테스트 커버리지 | Mock 19 | + IT 8 | **통합 검증** |

---

## 5. 기술적 의사결정

| 결정 | 선택 | 대안 | 이유 |
|------|------|------|------|
| 세션 원자적 교체 | Lua Script | MULTI/EXEC | 단일 명령 실행, CAS 체크 불필요 |
| 캐시 직렬화 | Jackson JSON | JDK Serialization | 디버깅 가시성, 호환성, 크기 절감 |
| 장애 격리 | Resilience4j | Hystrix | 최신 유지보수, Spring Boot 3 공식 지원 |
| Rate Limiting | Lua Sliding Window | Fixed Window | 경계값 burst 방지, 정확한 빈도 제한 |
| Fallback | 3계층 (Redis→Local→DB) | 단순 예외 처리 | 서비스 연속성 극대화 |
