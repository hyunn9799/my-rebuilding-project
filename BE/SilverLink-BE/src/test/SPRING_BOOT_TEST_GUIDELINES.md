# Spring Boot 테스트 코드 작성 가이드라인

**작성 목적:** 테스트 코드 작성 시 일관성을 유지하고 반복적인 오류를 방지하기 위한 표준 템플릿 및 Best Practice 정의

---

## 📋 목차

1. [공통 규칙](#공통-규칙)
2. [단위 테스트 템플릿 (Service Layer)](#단위-테스트-템플릿-service-layer)
3. [통합 테스트 템플릿 (Controller Layer)](#통합-테스트-템플릿-controller-layer)
4. [자주 발생하는 오류 및 해결 방법](#자주-발생하는-오류-및-해결-방법)
5. [검증 및 확인사항 체크리스트](#검증-및-확인사항-체크리스트)

---

## 공통 규칙

### 1. Import 문 작성 주의사항

> [!CAUTION]
> Jackson ObjectMapper는 올바른 패키지를 import 해야 합니다

```java
// ❌ 잘못된 import (tools.jackson.databind 등 존재하지 않는 패키지)
import tools.jackson.databind.ObjectMapper;

// ✅ 올바른 import
import com.fasterxml.jackson.databind.ObjectMapper;
```

**통합 테스트에서 자주 사용되는 올바른 imports:**

```java
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
```

### 2. Mockito Strictness 설정

> [!IMPORTANT]
> 불필요한 stubbing 경고를 방지하려면 `@MockitoSettings(strictness = Strictness.LENIENT)` 추가

테스트 메서드에서 일부 stubbing이 사용되지 않거나, `@BeforeEach`에서 공통 stubbing을 설정하는 경우 `UnnecessaryStubbingException`이 발생할 수 있습니다.

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // ✅ 필수 추가
class AuthServiceTest {
    // ...
}
```

### 3. 타입 일치 확인

> [!WARNING]
> Mockito `verify()`나 `given()`에서 타입을 정확히 맞춰야 합니다

```java
// ❌ 잘못된 타입 (int vs long 불일치)
verify(valueOps).set(eq("key"), eq("1"), eq(60), eq(TimeUnit.SECONDS));

// ✅ 올바른 타입 (메서드 시그니처에서 long을 요구하는 경우)
verify(valueOps).set(eq("key"), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
```

**타입 확인 방법:**
- 실제 메서드 시그니처 확인 (Ctrl+클릭 또는 IDE 기능 활용)
- 컴파일 에러 메시지 확인
- ArgumentCaptor 사용 시 제네릭 타입 정확히 지정

---

## 단위 테스트 템플릿 (Service Layer)

### 기본 구조

```java
package com.aicc.silverlink.domain.[도메인].service;

import com.aicc.silverlink.domain.[도메인].dto.[도메인]Dtos;
import com.aicc.silverlink.domain.[도메인].entity.[엔티티];
import com.aicc.silverlink.domain.[도메인].repository.[엔티티]Repository;
// 필요한 다른 의존성 import

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // ✅ 필수
class [서비스명]Test {

    // ============================================
    // Mock 객체 선언
    // ============================================
    @Mock
    private [Repository명] repository;
    
    @Mock
    private [다른의존성] dependency;

    @InjectMocks
    private [서비스명] service;

    // ============================================
    // 테스트용 데이터 필드
    // ============================================
    private [엔티티타입] testEntity;

    // ============================================
    // 테스트 전 초기화
    // ============================================
    @BeforeEach
    void setUp() {
        // 공통 Mock 설정
        // given([mock객체].[메서드]()).willReturn([반환값]);
        
        // 테스트 데이터 초기화
        testEntity = [엔티티].builder()
                .id(1L)
                .field1("value1")
                .field2("value2")
                .build();
    }

    // ============================================
    // 테스트 메서드
    // ============================================
    @Test
    @DisplayName("[기능명] 성공 - [성공 조건 설명]")
    void [메서드명]_Success() {
        // given - 테스트 준비 (입력 데이터, Mock 동작 정의)
        
        // when - 실제 테스트 대상 메서드 실행
        
        // then - 결과 검증 (assertThat, verify)
    }

    @Test
    @DisplayName("[기능명] 실패 - [실패 조건 설명]")
    void [메서드명]_Fail_[실패이유]() {
        // given
        
        // when & then
        assertThatThrownBy(() -> service.[메서드]([파라미터]))
                .isInstanceOf([예외타입].class)
                .hasMessage("[에러메시지]");
    }
}
```

### JPA Entity Mock 주의사항

> [!WARNING]
> JPA `@PrePersist`, `@PostLoad` 등의 lifecycle callback은 테스트에서 자동 실행되지 않습니다

**문제 상황:**

```java
// ❌ 테스트에서 @PrePersist가 호출되지 않아 필드가 null이 될 수 있음
testVerification = PhoneVerification.create(...);
// testVerification.getStatus() -> null (JPA lifecycle 미작동)
```

**해결 방법 1: Mock 객체 사용 (권장)**

```java
// ✅ Mock 객체로 필요한 필드 직접 설정
testVerification = mock(PhoneVerification.class);
given(testVerification.getStatus()).willReturn(PhoneVerification.Status.REQUESTED);
given(testVerification.getExpiresAt()).willReturn(LocalDateTime.now().plusMinutes(5));
given(testVerification.getFailCount()).willReturn(0);
given(testVerification.getCodeHash()).willReturn("$2a$10$hashedCode");
given(testVerification.getPhoneE164()).willReturn("+821012345678");
```

**해결 방법 2: 테스트용 생성자/빌더 추가**

```java
// Entity에 테스트용 public 생성자 또는 빌더 추가
public static PhoneVerification createForTest(
    String phoneE164, 
    Status status, 
    LocalDateTime expiresAt
) {
    PhoneVerification pv = new PhoneVerification();
    pv.phoneE164 = phoneE164;
    pv.status = status;
    pv.expiresAt = expiresAt;
    return pv;
}
```

---

## 통합 테스트 템플릿 (Controller Layer)

### 기본 구조

```java
package com.aicc.silverlink.domain.[도메인].controller;

import com.aicc.silverlink.domain.[도메인].dto.[DTO명];
import com.aicc.silverlink.domain.[도메인].service.[서비스명];
import com.fasterxml.jackson.databind.ObjectMapper;  // ✅ 올바른 import

import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc
class [컨트롤러명]IT {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;  // ✅ Jackson ObjectMapper

    /**
     * ✅ 통합 컨텍스트는 올리되, Service는 Mock으로 처리
     * (DB/Redis/외부 API 의존성 제거)
     */
    @MockitoBean
    private [서비스명] service;

    @MockitoBean  // 필요시 Properties도 Mock
    private [Properties명] properties;

    @BeforeEach
    void setup() {
        // Properties Mock 설정 (필요한 경우)
        given(properties.get[설정명]()).willReturn([값]);
    }

    @Test
    @DisplayName("[API명] 성공 - [성공 조건]")
    void [메서드명]_Success() throws Exception {
        // given
        [요청DTO] request = new [요청DTO]([파라미터]);
        [응답타입] response = new [응답타입]([파라미터]);
        
        given(service.[메서드](any([요청DTO].class))).willReturn(response);

        // when & then
        mockMvc.perform(post("/[경로]")
                        .with(csrf())  // ✅ Spring Security 사용 시 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.field1").value([기대값]))
                .andExpect(jsonPath("$.field2").exists());
    }

    @Test
    @DisplayName("[API명] 실패 - [실패 조건]")
    void [메서드명]_Fail_[실패이유]() throws Exception {
        // given
        [요청DTO] request = new [요청DTO]([잘못된파라미터]);
        
        given(service.[메서드](any([요청DTO].class)))
                .willThrow(new [예외타입]("[에러메시지]"));

        // when & then
        mockMvc.perform(post("/[경로]")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().[예상상태코드]());
        // 상태코드: isOk(), isBadRequest(), isUnauthorized(), isInternalServerError() 등
    }
    
    @Test
    @DisplayName("쿠키 테스트 예시 - 쿠키와 함께 요청")
    void testWithCookie() throws Exception {
        // given
        Cookie cookie = new Cookie("refresh_token", "token-value");
        
        // when & then
        mockMvc.perform(post("/[경로]")
                        .with(csrf())
                        .cookie(cookie))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(cookie().exists("refresh_token"))
                .andExpect(cookie().value("refresh_token", "new-token-value"));
    }
}
```

### MockMvc 주요 검증 메서드

**상태 코드 검증:**

```java
.andExpect(status().isOk())                    // 200
.andExpect(status().isCreated())               // 201
.andExpect(status().isBadRequest())            // 400
.andExpect(status().isUnauthorized())          // 401
.andExpect(status().isForbidden())             // 403
.andExpect(status().isNotFound())              // 404
.andExpect(status().isInternalServerError())   // 500
```

**JSON 응답 검증:**

```java
.andExpect(jsonPath("$.필드명").value("기대값"))
.andExpect(jsonPath("$.필드명").exists())
.andExpect(jsonPath("$.배열[0].필드").value("값"))
.andExpect(jsonPath("$.필드명").isArray())
```

**쿠키 검증:**

```java
.andExpect(cookie().exists("쿠키이름"))
.andExpect(cookie().value("쿠키이름", "값"))
.andExpect(cookie().httpOnly("쿠키이름", true))
.andExpect(cookie().secure("쿠키이름", true))
.andExpect(cookie().maxAge("쿠키이름", 3600))
```

---

## 자주 발생하는 오류 및 해결 방법

### 1. Mockito UnnecessaryStubbingException

**증상:**

```
org.mockito.exceptions.misusing.UnnecessaryStubbingException: 
Unnecessary stubbings detected.
```

**원인:**
- `@BeforeEach`에서 stubbing 했지만 일부 테스트에서 사용하지 않음
- `given()`으로 설정한 Mock이 실제로 호출되지 않음

**해결:**

```java
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)  // ✅ 추가
class YourTest {
    // ...
}
```

### 2. Jackson ObjectMapper Import 오류

**증상:**

```
Cannot resolve symbol 'ObjectMapper'
```

**원인:**
- 잘못된 패키지 import (예: `tools.jackson.databind.ObjectMapper`)

**해결:**

```java
// ✅ 올바른 import
import com.fasterxml.jackson.databind.ObjectMapper;
```

### 3. JPA @PrePersist 미작동

**증상:**
- 테스트에서 엔티티 생성 후 필드가 null
- `@PrePersist`로 설정되는 값이 없음

**원인:**
- 테스트는 실제 JPA 영속성 컨텍스트를 사용하지 않음

**해결:**

```java
// Mock 객체 사용
testEntity = mock(Entity.class);
given(testEntity.getField()).willReturn(expectedValue);
```

### 4. ArgumentMatcher 타입 불일치

**증상:**

```
argument type mismatch
```

**원인:**
- `eq()`, `anyLong()` 등 사용 시 메서드 파라미터 타입과 불일치

**해결:**

```java
// 메서드 시그니처가 set(String key, String value, long timeout, TimeUnit unit)인 경우
verify(valueOps).set(eq("key"), eq("1"), eq(60L), eq(TimeUnit.SECONDS));
//                                              ^ long 타입
```

### 5. Redis ValueOperations Mock 설정 누락

**증상:**

```
NullPointerException when calling redisTemplate.opsForValue()
```

**해결:**

```java
@Mock
private StringRedisTemplate redis;

@Mock
private ValueOperations<String, String> valueOps;

@BeforeEach
void setUp() {
    given(redis.opsForValue()).willReturn(valueOps);  // ✅ 필수
}
```

---

## 검증 및 확인사항 체크리스트

### 테스트 작성 전
- [ ] 테스트할 메서드의 성공/실패 시나리오 파악
- [ ] 필요한 Mock 객체 목록 작성
- [ ] Entity의 JPA Lifecycle callback 확인 (Mock 필요 여부)

### 테스트 작성 중
- [ ] `@ExtendWith(MockitoExtension.class)` 추가
- [ ] `@MockitoSettings(strictness = Strictness.LENIENT)` 추가
- [ ] Import 문 확인 (특히 ObjectMapper)
- [ ] Mock 타입과 실제 메서드 파라미터 타입 일치 확인
- [ ] `@BeforeEach`에서 공통 Mock 설정
- [ ] 테스트 메서드명은 `[메서드명]_[Success/Fail]_[조건]` 형식

### 테스트 실행 후
- [ ] 모든 테스트 통과 여부 확인
- [ ] 불필요한 stubbing 경고가 없는지 확인
- [ ] Verify 문으로 주요 동작 검증 확인
- [ ] Coverage 확인 (가능하면 80% 이상)

---

## 테스트 실행 명령어

```bash
# 전체 테스트 실행
.\gradlew test --no-daemon

# 특정 클래스만 테스트
.\gradlew test --tests "com.aicc.silverlink.domain.auth.service.AuthServiceTest" --no-daemon

# 특정 메서드만 테스트
.\gradlew test --tests "*.AuthServiceTest.login_Success" --no-daemon

# 테스트 결과 HTML 리포트 확인
# build/reports/tests/test/index.html
```

---

## 참고 자료

### AssertJ 주요 검증 메서드

```java
// 기본 검증
assertThat(actual).isEqualTo(expected);
assertThat(actual).isNotNull();
assertThat(actual).isNull();
assertThat(actual).isTrue();
assertThat(actual).isFalse();

// 컬렉션 검증
assertThat(list).hasSize(3);
assertThat(list).contains(element);
assertThat(list).containsExactly(elem1, elem2);

// 예외 검증
assertThatThrownBy(() -> service.method())
    .isInstanceOf(IllegalArgumentException.class)
    .hasMessage("ERROR_MESSAGE");
```

### Mockito 주요 메서드

```java
// Stubbing
given(mock.method()).willReturn(value);
given(mock.method()).willThrow(new Exception());

// 검증
verify(mock).method();
verify(mock, times(2)).method();
verify(mock, never()).method();

// ArgumentCaptor
ArgumentCaptor<Type> captor = ArgumentCaptor.forClass(Type.class);
verify(mock).method(captor.capture());
assertThat(captor.getValue()).isEqualTo(expected);
```

---

## 맺음말

이 가이드라인을 따라 테스트를 작성하면:

✅ 일관된 테스트 코드 구조 유지  
✅ 반복적인 오류 방지  
✅ 코드 리뷰 시간 단축  
✅ 유지보수성 향상

새로운 오류 발견 시 이 문서를 업데이트하여 팀 전체가 활용할 수 있도록 합니다.
