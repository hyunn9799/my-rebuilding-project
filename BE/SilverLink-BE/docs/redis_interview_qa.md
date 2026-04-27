# Redis 면접 예상 질문 및 모범 답안 — 50선

---

## Part 1: Redis 기본 원리 (Q1 – Q10)

---

### Q1. Redis의 싱글 스레드 모델의 장단점을 설명해주세요.

**모범 답안:**

Redis는 **이벤트 루프 기반 싱글 스레드**로 명령을 처리합니다.

**장점:**
- **락 불필요:** 하나의 스레드가 순차적으로 명령을 처리하므로 동시성 제어를 위한 Lock/Mutex가 필요 없어 오버헤드가 제거됩니다.
- **원자적 실행:** 모든 명령이 원자적으로 실행되어 Race Condition이 원천 차단됩니다. 이것이 Lua Script가 원자적으로 동작하는 근거입니다.
- **낮은 지연:** Context Switching이 없어 마이크로초 단위의 낮은 응답 지연을 달성합니다.

**단점:**
- **CPU 바운드 작업에 부적합:** `KEYS *`, `SORT` 등 O(N) 명령이 다른 모든 명령을 블로킹합니다.
- **멀티코어 미활용:** 단일 코어만 사용하므로, Redis 6.0부터 I/O 멀티스레딩을 도입하여 네트워크 I/O는 병렬화했습니다.

**실무 적용:**
> 저는 SilverLink 프로젝트에서 이 특성을 활용하여 Lua Script로 세션 Atomic Swap을 구현했습니다. 기존 세션 무효화 + 새 세션 생성이 단일 원자적 명령으로 실행되어 MULTI/EXEC 없이 Race Condition을 해결했습니다.

---

### Q2. Redis의 데이터 타입을 설명하고, 프로젝트에서 어떻게 활용했는지 말씀해주세요.

**모범 답안:**

| 타입 | 설명 | 프로젝트 사용처 |
|------|------|-----------------|
| **String** | 가장 기본, key-value | 사용자→세션 매핑 (`user:42:sid` → `abc-123`) |
| **Hash** | 필드-값 쌍의 맵 | 세션 메타데이터 (`sess:abc` → userId, role, ip, ua, deviceId) |
| **Sorted Set** | 점수 기반 정렬 | Sliding Window Rate Limiter (타임스탬프 = score) |
| **Set** | 중복 없는 집합 | (사용하지 않음) |
| **List** | 삽입 순서 유지 | (사용하지 않음) |

**실무 적용:**
> Hash 타입으로 세션 데이터를 관리하면서, `HMGET`으로 3개 필드를 한 번에 조회하여 네트워크 왕복을 66% 줄였습니다. Rate Limiter에서는 Sorted Set의 `ZREMRANGEBYSCORE`로 만료된 항목을 효율적으로 제거했습니다.

---

### Q3. Redis 키 만료(Expiration)의 내부 동작을 설명해주세요.

**모범 답안:**

Redis는 **두 가지 만료 전략**을 조합합니다:

1. **Lazy Expiration (지연 삭제):** 키에 접근할 때 TTL을 확인하여 만료되었으면 삭제. 접근하지 않는 키는 메모리에 남음.
2. **Active Expiration (능동 삭제):** 초당 10회 랜덤으로 20개 키를 샘플링하여 만료된 키를 삭제. 만료율이 25% 이상이면 반복.

**실무 적용:**
> 세션 만료 시 자동 후처리가 필요했는데, Redis 자체 만료만으로는 "언제 만료되었는지" 알 수 없었습니다. **Keyspace Notification**(`__keyevent@*__:expired`)을 활용하여 만료 이벤트를 실시간으로 수신하고 감사 로그를 기록하도록 구현했습니다.

---

### Q4. KEYS 명령이 운영 환경에서 위험한 이유와 대안을 말씀해주세요.

**모범 답안:**

`KEYS *`는 **O(N) 시간 복잡도**로 전체 키를 스캔합니다. Redis 싱글 스레드 특성상 이 명령이 실행되는 동안 **모든 다른 명령이 블로킹**되어 서비스 장애를 유발합니다.

**대안:** `SCAN` 명령 사용
- 커서 기반으로 점진적으로 반복하여 블로킹 없이 키를 탐색
- `COUNT` 파라미터로 한 번에 반환할 키 수를 제어

**실무 적용:**
> `RedisKeyManager`에서 `scanPattern(domain)` 메서드를 제공하여, 운영 환경에서 안전하게 키를 탐색할 수 있도록 설계했습니다.

---

### Q5. Redis Pipeline이 뭔가요? 왜 사용하나요?

**모범 답안:**

Pipeline은 **여러 Redis 명령을 버퍼링하여 한 번의 네트워크 왕복으로 일괄 전송**하는 기법입니다.

```
일반 호출:   [요청1] →← [응답1] → [요청2] →← [응답2] → [요청3] →← [응답3]
Pipeline:    [요청1+요청2+요청3] →← [응답1+응답2+응답3]
```

**주의점:**
- 원자적 실행을 보장하지 **않습니다** (중간에 다른 클라이언트 명령이 끼어들 수 있음)
- 원자성이 필요하면 Lua Script나 MULTI/EXEC를 사용해야 합니다

**실무 적용:**
> `touch()` 메서드에서 hSet + expire × 2를 Pipeline으로 묶어 3회 → 1회 왕복으로 최적화했습니다. `invalidateBySid()`에서도 setEx + del × 2를 Pipeline으로 처리했습니다. Pipeline은 성능을, Lua Script는 원자성을 위해 사용하여 목적에 맞게 구분했습니다.

---

### Q6. Redis Transaction (MULTI/EXEC)과 Lua Script의 차이는?

**모범 답안:**

| 비교 | MULTI/EXEC | Lua Script |
|------|-----------|------------|
| 원자성 | ✅ 명령 일괄 실행 | ✅ 단일 명령으로 실행 |
| 조건부 로직 | ❌ 불가 (WATCH로 CAS만 가능) | ✅ if/else 가능 |
| 조회→판단→쓰기 | ❌ 실행 중 결과 참조 불가 | ✅ 중간 결과 참조 가능 |
| 네트워크 왕복 | 2회 (MULTI + EXEC) | 1회 (EVALSHA) |
| 디버깅 | 쉬움 | 어려움 (별도 .lua 파일 관리) |

**실무 적용:**
> 세션 Atomic Swap에서 "기존 세션 조회 → 있으면 무효화 → 새 세션 생성"이라는 **조건부 로직**이 필요했기 때문에 MULTI/EXEC가 아닌 Lua Script를 선택했습니다. Rate Limiter에서도 "COUNT → 판단 → ZADD" 패턴에 Lua Script를 사용했습니다.

---

### Q7. Redis의 영속화 방식 (RDB, AOF)을 비교해주세요.

**모범 답안:**

| 비교 | RDB (Snapshotting) | AOF (Append Only File) |
|------|-------------------|----------------------|
| 동작 | 주기적 메모리 스냅샷 | 모든 쓰기 명령 로그 |
| 데이터 유실 | 마지막 스냅샷 이후 유실 가능 | fsync 정책에 따라 1초 이내 |
| 파일 크기 | 작음 (압축) | 큼 (명령 로그) |
| 복구 속도 | 빠름 (메모리 로드) | 느림 (명령 재실행) |
| 권장 | 개발/캐시 | 운영/세션 |

**실무 적용:**
> SilverLink에서 Redis를 세션 저장소로 사용하므로, 운영 환경에서는 AOF (`everysec`)를 권장했습니다. 다만 세션 데이터의 특성상 TTL 만료가 빈번하므로 AOF rewrite 주기를 적절히 설정해야 합니다.

---

### Q8. Redis에서 메모리가 부족할 때 어떻게 되나요?

**모범 답안:**

`maxmemory-policy`에 따라 메모리 해제 전략이 결정됩니다:

| 정책 | 동작 |
|------|------|
| `noeviction` | 쓰기 명령 거부 (기본값) |
| `allkeys-lru` | LRU 알고리즘으로 가장 오래된 키 삭제 |
| `volatile-lru` | TTL이 설정된 키 중 LRU 삭제 |
| `allkeys-lfu` | LFU(사용 빈도) 기반 삭제 (Redis 4.0+) |
| `volatile-ttl` | TTL이 가장 짧은 키부터 삭제 |

**실무 적용:**
> 세션 저장소와 캐시를 동일 Redis에서 운영하므로, `volatile-lru`를 권장합니다. 세션 키는 모두 TTL이 설정되어 있으므로 우선 삭제 대상이 되고, TTL이 없는 중요 데이터는 보존됩니다.

---

### Q9. Redis Sentinel과 Cluster의 차이를 설명해주세요.

**모범 답안:**

| 비교 | Sentinel | Cluster |
|------|----------|---------|
| 목적 | **고가용성** (HA) | **수평 확장** + HA |
| 데이터 분산 | ❌ (마스터 1대가 전체 데이터) | ✅ (16384 해시 슬롯으로 분산) |
| 자동 페일오버 | ✅ Sentinel이 감시·승격 | ✅ 클러스터 내부 투표 |
| 적합한 규모 | 소~중규모 (메모리 < 32GB) | 대규모 (TB 단위) |
| 멀티키 명령 | ✅ 제한 없음 | ⚠️ 같은 슬롯 내에서만 |
| Lua Script | ✅ 제한 없음 | ⚠️ 같은 슬롯 키만 |

**실무 적용:**
> SilverLink는 세션 수백 ~ 수천 규모이므로 Sentinel이 적합하다고 판단하여 `application-prod.yml`에 Sentinel 3대 구성을 설정했습니다. Lua Script에서 멀티키 명령을 사용하기 때문에 Cluster보다 Sentinel이 운영이 단순합니다.

---

### Q10. Redis의 Pub/Sub과 Stream의 차이를 알려주세요.

**모범 답안:**

| 비교 | Pub/Sub | Stream |
|------|---------|--------|
| 메시지 저장 | ❌ (발행 즉시 소실) | ✅ (로그처럼 영속) |
| 소비 그룹 | ❌ | ✅ (Consumer Group) |
| 메시지 보장 | At-most-once | At-least-once (ACK) |
| 적합한 용도 | 실시간 알림, 이벤트 전파 | 이벤트 소싱, 작업 큐 |

**실무 적용:**
> 세션 킥 알림은 "지금 이 순간" 모든 인스턴스에 전파되면 되므로 Pub/Sub을 사용했습니다. 메시지 유실이 허용되는 이유는 다음 요청 시 JwtFilter에서 `wasInvalidated()` 확인으로 보완되기 때문입니다.

---

## Part 2: 실무 설계 심화 (Q11 – Q25)

---

### Q11. 세션 관리에 Redis를 사용한 이유는?

**모범 답안:**

1. **핫 데이터 특성:** 세션은 매 요청마다 검증되는 고빈도 데이터 → 인메모리 DB가 적합
2. **자동 만료:** TTL 기반 Sliding Window로 비활성 세션 자동 정리
3. **분산 환경 공유:** 여러 WAS 인스턴스가 동일한 세션 저장소 참조
4. **원자적 연산:** Lua Script로 세션 교체 시 동시성 안전 보장

---

### Q12. Sliding Window 세션 TTL은 어떻게 구현했나요?

**모범 답안:**

```
최초 로그인 → EXPIRE sess:abc 1800 (30분)
요청 1 (10분 후) → touch() → EXPIRE sess:abc 1800 (30분 재갱신)
요청 2 (20분 후) → touch() → EXPIRE sess:abc 1800 (30분 재갱신)
비활성 30분 경과 → 자동 만료
```

**핵심:** 매 요청마다 `touch()`를 호출하여 TTL을 갱신합니다. Pipeline으로 `hSet(lastSeen)` + `expire(sessionKey)` + `expire(userSidKey)`를 1회 왕복으로 처리합니다.

---

### Q13. Circuit Breaker 패턴을 왜 적용했고, 상태 전이를 설명해주세요.

**모범 답안:**

```
CLOSED ──(실패율 50% 초과)──→ OPEN
  ↑                              │
  │                         (30초 대기)
  │                              ↓
  └──(3회 성공)────── HALF-OPEN
```

| 상태 | 동작 |
|------|------|
| CLOSED | 정상 호출, 실패 집계 |
| OPEN | 즉시 Fallback 반환 (Redis 호출 안 함) |
| HALF-OPEN | 3회 시험 호출 → 성공 시 CLOSED 복귀 |

**핵심 설계 결정:**
> `isActive()`의 Fallback에서 `return false`를 선택한 이유: 보안 관점에서 "인증되지 않은 요청을 통과시키는 것"보다 "인증된 사용자를 재로그인시키는 것"이 더 안전합니다.

---

### Q14. @Cacheable 적용 전략을 설명해주세요. 어떤 데이터를 캐싱했나요?

**모범 답안:**

| 대상 | 캐시 이름 | TTL | 이유 |
|------|----------|-----|------|
| 유저 프로필 | `users` | 5분 | 자주 조회, 드물게 변경 |
| 복지 서비스 상세 | `welfare` | 30분 | 외부 API 연동, 변경 빈도 낮음 |
| 행정구역(시/도) | `divisions` | 1시간 | 거의 변하지 않는 마스터 데이터 |

**Cache Eviction 전략:**
- `updateMyProfile()` → `@CacheEvict(value = "users", key = "#userId")`
- **Write-through가 아닌 Invalidation 전략**을 선택한 이유: 데이터의 정합성을 보장하면서, 다음 조회 시 자동으로 최신 데이터가 캐시됨

---

### Q15. 캐시 직렬화를 JDK에서 Jackson JSON으로 변경한 이유는?

**모범 답안:**

| 비교 | JDK Serialization | Jackson JSON |
|------|-------------------|-------------|
| 가독성 | ❌ 바이너리 | ✅ 사람이 읽을 수 있음 |
| 호환성 | ❌ 클래스 변경 시 깨짐 | ✅ 필드 추가/삭제 유연 |
| 크기 | 큼 (클래스 메타데이터 포함) | 작음 |
| 디버깅 | `redis-cli`로 확인 불가 | `redis-cli`로 바로 확인 가능 |
| 보안 | ❌ 역직렬화 공격 취약 | ✅ 안전 |

**실무 적용:**
> `activateDefaultTyping`을 설정하여 다형성 캐시(Object 타입) 저장 시에도 타입 정보가 보존되도록 구성했습니다.

---

### Q16. Rate Limiter를 Lua Script로 구현한 이유는?

**모범 답안:**

**원자성 보장 필요:**
```
1) ZCARD로 현재 카운트 조회
2) if count < limit → ZADD로 추가
```
이 두 단계가 원자적으로 실행되지 않으면, 동시 요청 시 **제한을 초과하는 요청이 통과**될 수 있습니다.

Lua Script로 서버 사이드에서 단일 명령으로 실행하면 이 문제가 해결됩니다.

**Sliding Window vs Fixed Window:**
- Fixed Window: 경계값에서 burst 발생 (59초에 100회 + 61초에 100회 = 2초에 200회)
- Sliding Window: Sorted Set의 타임스탬프 기반으로 정확한 윈도우 계산

---

### Q17. Keyspace Notification의 주의점은?

**모범 답안:**

1. **기본 비활성화:** `notify-keyspace-events`를 명시적으로 설정해야 함
2. **At-most-once 보장:** 구독자가 없으면 이벤트 유실 → 보조 수단 필요
3. **성능 오버헤드:** 모든 키 변경마다 알림이 발행되므로 필요한 이벤트만 구독
4. **Cluster 제한:** 노드 로컬 이벤트만 발행 → Sentinel 환경이 유리

**실무 적용:**
> `__keyevent@*__:expired` 패턴만 구독하고, `sess:` 접두사 필터링으로 세션 만료만 처리합니다. 이벤트 유실 대비는 JwtFilter의 `wasInvalidated()` 확인으로 보완됩니다.

---

### Q18. DeviceInfo(디바이스 핑거프린팅)는 어떻게 구현했나요?

**모범 답안:**

```java
public record DeviceInfo(String ipAddress, String userAgent, String deviceId) {
    public static DeviceInfo fromRequest(HttpServletRequest request) {
        String ip = extractRealIp(request);  // X-Forwarded-For 우선
        String ua = request.getHeader("User-Agent");
        String deviceId = sha256(ip + "|" + ua);  // 고유 핑거프린트
        return new DeviceInfo(ip, ua, deviceId);
    }
}
```

**보안 고려:**
- IP는 프록시 헤더(`X-Forwarded-For`, `X-Real-IP`)에서 실제 클라이언트 IP 추출
- 로그 출력 시 IP 마스킹 (`192.168.***.***)
- Redis Hash에 DeviceInfo 저장 → 중복 로그인 시 기존 기기 정보 안내

---

### Q19. 감사 로그를 @Async로 처리한 이유와 주의점은?

**모범 답안:**

**이유:** 감사 로그 DB 저장이 API 응답 시간에 영향을 주지 않도록 비동기 처리.

**주의점:**
1. `@EnableAsync` 설정 필수
2. 스레드 풀 설정 (기본 SimpleAsyncTaskExecutor는 스레드 제한 없음 → 위험)
3. 예외 전파 불가 → try-catch로 방어적 처리
4. 트랜잭션 전파 불가 → 별도 트랜잭션 필요

**실무 적용:**
> DB 저장 실패 시에도 `log.info()` 출력은 이미 완료된 상태이므로 최소한의 추적성은 보장됩니다.

---

### Q20. RedisKeyManager로 키 네이밍을 표준화한 이유는?

**모범 답안:**

**문제:** dev/staging/prod가 같은 Redis를 공유하면 키 충돌 발생
```
sess:abc-123  ← dev에서 생성? prod에서 생성?
```

**해결:**
```java
// 형식: {env}:sl:{domain}:{key}
keyManager.key("sess", sid);  → "prod:sl:sess:abc-123"
keyManager.key("pv", phone);  → "dev:sl:pv:010-1234"
```

**효과:** 환경 격리, 키 추적성 향상, `scanPattern("sess")`로 안전한 일괄 조회

---

### Q21. Spring Cache의 `@CacheEvict`와 `@CachePut`의 차이는?

**모범 답안:**

| 어노테이션 | 동작 | 적합한 상황 |
|-----------|------|-----------|
| `@CacheEvict` | 캐시 삭제 → 다음 조회 시 재적재 | 조회 빈도 > 수정 빈도 |
| `@CachePut` | 항상 메서드 실행 + 결과를 캐시에 갱신 | 수정 후 즉시 캐시 반영 필요 |

**실무 적용:**
> `updateMyProfile()`에서 `@CacheEvict`를 사용했습니다. 프로필 수정 빈도가 낮으므로, 다음 조회 시 자연스럽게 최신 데이터가 캐시되는 Invalidation 전략이 적합합니다.

---

### Q22. Lettuce와 Jedis의 차이를 아시나요?

**모범 답안:**

| 비교 | Lettuce | Jedis |
|------|---------|-------|
| 연결 방식 | 비동기 + Netty 기반 | 동기 + BIO |
| 스레드 안전 | ✅ 단일 연결 공유 | ❌ 풀에서 연결 빌려야 함 |
| 성능 | 높음 (non-blocking) | 보통 |
| Spring Boot 기본 | ✅ (3.x 이후 기본) | ❌ |
| Reactive 지원 | ✅ | ❌ |

> Spring Boot 4.0은 기본으로 Lettuce를 사용하며, 비동기/리액티브 지원이 우수합니다.

---

### Q23. Redis 연결 풀 설정 시 고려할 사항은?

**모범 답안:**

```yaml
lettuce:
  pool:
    max-active: 16    # 최대 동시 연결 수
    max-idle: 8       # 유휴 연결 최대 수
    min-idle: 2       # 최소 유휴 연결 (콜드 스타트 방지)
    max-wait: 3000ms  # 연결 대기 타임아웃
```

**핵심 공식:** `max-active ≥ 동시 스레드 수 × Redis 호출 수/요청`

**주의점:**
- `max-active`가 너무 작으면 → 요청 대기 → 타임아웃
- `max-active`가 너무 크면 → Redis 서버 부하
- `min-idle = 0`이면 → 첫 요청 시 연결 생성 오버헤드 (콜드 스타트)

---

### Q24. Cache Stampede(캐시 쇄도) 문제를 어떻게 방지하나요?

**모범 답안:**

**문제:** 인기 키가 만료되는 순간, 수백 요청이 동시에 DB를 조회하는 현상

**해결 전략:**
1. **Distributed Lock:** 첫 번째 요청만 DB 조회, 나머지는 대기
2. **논리적 만료:** TTL보다 짧은 논리적 만료 시간을 설정하여 사전 갱신
3. **Never Expire + Background Refresh:** TTL 없이 백그라운드에서 주기적 갱신
4. **PER(Probabilistic Early Recomputation):** 만료 시간에 가까워질수록 확률적으로 사전 갱신

---

### Q25. Hot Key 문제란? 어떻게 해결하나요?

**모범 답안:**

**문제:** 특정 키에 요청이 집중되어 Redis 서버의 단일 스레드가 병목

**해결:**
1. **Local Cache 앞단 배치:** `RedisFallbackCache`처럼 ConcurrentHashMap으로 1차 캐시
2. **키 분산:** `hot_key:{random(1-10)}`으로 여러 키에 분산 → 읽기 시 랜덤 키 선택
3. **Redis Cluster의 Read Replica:** Replica에서 읽기 분산

---

## Part 3: 장애 대응 및 운영 (Q26 – Q35)

---

### Q26. Redis가 다운되면 어떻게 되나요?

**모범 답안:**

**SilverLink 아키텍처에서의 대응:**
1. **Circuit Breaker가 OPEN** → Fallback 메서드 실행
2. **isActive() Fallback** → `return false` → 사용자 재로그인 유도 (보안 우선)
3. **issueSession() Fallback** → 예외 발생 → "일시적 서비스 불가" 안내
4. **@Cacheable 데이터** → `RedisFallbackCache`가 로컬 캐시 → DB 순서로 제공
5. **30초 후 Half-Open** → Redis 복구 시 자동 정상화

---

### Q27. Redis 메모리 사용량을 모니터링하는 방법은?

**모범 답안:**

```bash
redis-cli INFO memory
# used_memory: 실제 사용 메모리
# used_memory_rss: OS가 할당한 메모리
# mem_fragmentation_ratio: RSS / used (1.5 이상이면 단편화 심각)
```

**실무 적용:**
> Spring Boot Actuator + Micrometer를 통해 `/actuator/metrics/redis.*` 엔드포인트로 메모리, 커넥션, 명령 수를 실시간 모니터링합니다.

---

### Q28. Redis에서 대량의 키를 삭제해야 할 때 어떻게 하나요?

**모범 답안:**

`DEL`로 대량 삭제하면 블로킹 발생 → **`UNLINK`** 사용 (Redis 4.0+)
- `UNLINK`는 비동기로 메모리 해제하여 블로킹을 방지
- `SCAN` + `UNLINK` 조합으로 점진적 삭제

---

### Q29. Redis Sentinel 페일오버 과정을 설명해주세요.

**모범 답안:**

```
1) Sentinel이 마스터에 PING → PONG 타임아웃
2) Subjective Down (1대 감지)
3) 과반수 Sentinel 동의 → Objective Down
4) 리더 Sentinel 선출
5) Replica 중 최적 노드를 새 Master로 승격
6) 다른 Replica에 SLAVEOF 명령
7) 클라이언트에 새 Master 정보 전파
```

---

### Q30. 운영 중 Redis 응답이 느려질 때 확인할 사항은?

**모범 답안:**

1. **`SLOWLOG GET 10`** — O(N) 명령 확인
2. **`INFO clients`** — 연결 수/블로킹 클라이언트 확인
3. **`INFO memory`** — 메모리 단편화/스왑 확인
4. **`INFO persistence`** — RDB/AOF 백그라운드 저장 중인지
5. **네트워크 레이턴시** — `redis-cli --latency`
6. **OS 수준** — CPU 사용률, Disk I/O (AOF fsync)

---

### Q31 – Q35: 추가 운영 질문

**Q31. maxmemory를 어떻게 설정하나요?**
> 물리 메모리의 60~80%, 나머지는 OS + RDB/AOF fork용

**Q32. Redis SSL/TLS는 왜 필요한가요?**
> 네트워크 스니핑 방지, AWS ElastiCache 필수 요구사항

**Q33. Redis ACL(Access Control)이란?**
> Redis 6.0에서 도입, 사용자별 명령/키 접근 권한 제어

**Q34. Redis Cluster에서 resharding은 어떻게 하나요?**
> `redis-cli --cluster reshard`로 해시 슬롯 이동, 온라인 수행 가능

**Q35. Pub/Sub 메시지 유실을 어떻게 보완하나요?**
> 보조 확인 로직 (wasInvalidated), Redis Stream 전환, 또는 외부 MQ 사용

---

## Part 4: 프로젝트별 심층 질문 (Q36 – Q50)

---

### Q36. Atomic Session Swap Lua Script의 동작을 라인별로 설명해주세요.

**모범 답안:**
```lua
-- 1. 현재 매핑된 세션 ID 조회
local oldSid = redis.call('GET', userSidKey)

-- 2. 기존 세션이 있으면 무효화
if oldSid then
    redis.call('SET', 'sess:invalidated:' .. oldSid, 'true', 'EX', 600)
    redis.call('DEL', 'sess:' .. oldSid)
end

-- 3. 새 세션 매핑 + 데이터 저장
redis.call('SET', userSidKey, newSid, 'EX', ttl)
redis.call('HSET', sessKey, unpack(fields))
redis.call('EXPIRE', sessKey, ttl)

-- 4. 기존 세션 ID 반환 (이벤트 발행용)
return oldSid
```

**핵심:** 전체 과정이 **단일 원자적 명령**으로 실행되어 중간에 다른 클라이언트 명령이 끼어들 수 없습니다.

---

### Q37. Circuit Breaker Fallback에서 isActive()가 false를 반환하는 이유는?

**모범 답안:**

보안과 가용성의 **트레이드오프**에서 보안을 우선했습니다.

- `return true` → Redis 장애 시 모든 요청이 인증된 것으로 처리 → **보안 위험**
- `return false` → Redis 장애 시 사용자가 재로그인 → **불편하지만 안전**

SilverLink는 독거노인 돌봄 플랫폼으로 **개인 건강 정보를 다루기 때문에 보안을 우선**합니다.

---

### Q38. Pipeline과 Lua Script를 어떻게 구분해서 사용했나요?

**모범 답안:**

| 기준 | Pipeline | Lua Script |
|------|----------|------------|
| 원자성 필요 | ❌ | ✅ |
| 중간 결과 참조 | ❌ | ✅ |
| 조건부 로직 | ❌ | ✅ |
| 단순 배치 | ✅ | ❌ (과잉) |

- `touch()` → Pipeline (hSet + expire × 2, 순서만 보장하면 됨)
- `invalidateBySid()` → Pipeline (setEx + del × 2, 독립적 명령)
- 세션 Atomic Swap → Lua Script (조회 → 판단 → 쓰기 패턴)
- Rate Limiter → Lua Script (COUNT → 판단 → ZADD 패턴)

---

### Q39. RedisFallbackCache의 3계층이 동작하는 시나리오를 구체적으로 설명해주세요.

**모범 답안:**

```
정상 시:
  Client → Redis(✅) → 응답 + 로컬 캐시에 복사

Redis 장애:
  Client → Redis(❌) → 로컬 ConcurrentHashMap(✅) → 응답

Redis 장애 + 로컬 캐시 만료:
  Client → Redis(❌) → 로컬(❌ 만료) → DB(✅) → 응답 + 로컬에 저장

Redis 복구:
  Client → Redis(✅) → 정상 복귀 + 로컬 캐시 갱신
```

---

### Q40. Testcontainers로 통합 테스트를 작성한 이유는?

**모범 답안:**

**Mock의 한계:**
- Lua Script가 서버에서 원자적으로 실행되는 동작을 Mock으로 검증 불가
- Pipeline의 순서 보장, TTL 만료 동작은 실제 Redis에서만 검증 가능
- `HMGET` 반환값의 null 처리 등 구현 세부사항은 Mock과 실제가 다를 수 있음

**Testcontainers의 장점:**
- Docker 기반으로 테스트마다 깨끗한 Redis 인스턴스 생성
- CI/CD 파이프라인에서도 동일하게 실행 가능
- `@DynamicPropertySource`로 포트 충돌 없이 자동 설정

---

### Q41 – Q45: 성능 관련 심층 질문

**Q41. HMGET과 HGETALL의 선택 기준은?**
> 필요한 필드가 일부면 HMGET, 전체면 HGETALL. SilverLink에서 DeviceInfo 조회 시 3개 필드만 필요하므로 HMGET.

**Q42. executePipelined에서 바이트 변환하는 이유는?**
> Lettuce의 low-level connection은 byte[] 기반이므로 StringRedisTemplate의 추상화를 우회.

**Q43. Jackson 직렬화에서 activateDefaultTyping을 사용한 이유는?**
> Object 타입으로 캐시 저장 시 역직렬화할 클래스 정보가 필요. `@class` 프로퍼티로 타입 보존.

**Q44. @Async와 @EventListener 조합의 스레드 풀 고려사항은?**
> 기본 SimpleAsyncTaskExecutor는 스레드 제한 없음 → ThreadPoolTaskExecutor로 교체 권장.

**Q45. Sorted Set 기반 Rate Limiter의 메모리 사용량은?**
> 각 요청이 하나의 멤버로 저장되므로, 윈도우 × 최대 요청 수 만큼의 멤버가 존재. PEXPIRE로 자동 정리.

---

### Q46 – Q50: 아키텍처 설계 질문

**Q46. 세션 저장소를 Redis에서 JWT 기반으로 전환한다면?**
> Stateless는 서버 부하 감소 장점이 있지만, 강제 로그아웃 불가 + 토큰 탈취 시 대응 어려움. SilverLink에서는 "1계정 1기기" 정책을 위해 서버 사이드 세션이 필수.

**Q47. Redis를 메시지 큐로 사용하는 것은 적절한가요?**
> 단순 작업 큐는 가능하나, 메시지 보장(at-least-once)이 중요하면 Kafka/RabbitMQ가 적합. Redis Stream은 중간 수준.

**Q48. 멀티 테넌트 환경에서 Redis 격리 전략은?**
> Database 번호 분리 (SELECT 0~15), 키 접두사 분리 (RedisKeyManager), 또는 인스턴스 분리.

**Q49. 캐시 데이터와 세션 데이터를 같은 Redis에 저장해도 되나요?**
> 가능하지만 maxmemory-policy를 `volatile-lru`로 설정하여 TTL 있는 캐시 키가 먼저 제거되도록. 세션 데이터는 TTL이 있으므로 함께 관리 가능.

**Q50. 이 Redis 고도화를 한 문장으로 요약한다면?**
> "RedIS를 단순 캐시가 아닌, Circuit Breaker로 격리되고, Lua Script로 동시성을 보장하며, Pub/Sub으로 실시간 전파되는 **분산 세션 보안 인프라**로 설계·구현했습니다."
