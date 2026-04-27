package com.aicc.silverlink.domain.auth.controller;

import com.aicc.silverlink.domain.auth.dto.AuthDtos;
import com.aicc.silverlink.domain.auth.service.AuthService;
import com.aicc.silverlink.domain.session.dto.DeviceInfo;
import com.aicc.silverlink.domain.session.service.SessionService;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import com.aicc.silverlink.global.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

import java.time.Instant;
import java.util.Map;

@Tag(name = "인증", description = "로그인/로그아웃/토큰 갱신 API")
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final SessionService sessionService;
    private final JwtTokenProvider jwtTokenProvider;
    private final AuthPolicyProperties props;

    @PostMapping("/login")
    public AuthDtos.TokenResponse login(@RequestBody AuthDtos.LoginRequest req, HttpServletResponse res,
            HttpServletRequest request) {

        DeviceInfo deviceInfo = DeviceInfo.from(request);
        AuthService.AuthResult result = authService.login(req, deviceInfo);

        String cookieValue = result.sid() + "." + result.refreshToken();
        setRefreshCookie(res, cookieValue);

        return new AuthDtos.TokenResponse(result.accessToken(), result.ttl(), result.role().name());
    }

    @PostMapping("/login/phone")
    public AuthDtos.TokenResponse loginWithPhone(@RequestBody AuthDtos.PhoneLoginRequest req, HttpServletResponse res,
            HttpServletRequest request) {
        AuthService.AuthResult result = authService.loginWithPhone(req.phone(), req.proofToken());

        String cookieValue = result.sid() + "." + result.refreshToken();
        setRefreshCookie(res, cookieValue);

        return new AuthDtos.TokenResponse(result.accessToken(), result.ttl(), result.role().name());
    }

    @PostMapping("/refresh")
    public AuthDtos.RefreshResponse refresh(HttpServletRequest req, HttpServletResponse res) {
        String cookieValue = readCookie(req, props.getRefreshCookieName());

        if (cookieValue == null || !cookieValue.contains(".")) {
            throw new IllegalArgumentException("NO_REFRESH_TOKEN");
        }

        String sid = cookieValue.substring(0, cookieValue.indexOf('.'));
        String refreshToken = cookieValue.substring(cookieValue.indexOf('.') + 1);

        AuthService.AuthResult result = authService.refresh(sid, refreshToken);

        String newCookieValue = result.sid() + "." + result.refreshToken();
        setRefreshCookie(res, newCookieValue);

        return new AuthDtos.RefreshResponse(result.accessToken(), result.ttl());

    }

    @PostMapping("/logout")
    public void logout(HttpServletRequest req, HttpServletResponse res) {
        String cookieVal = readCookie(req, props.getRefreshCookieName());
        if (cookieVal != null && cookieVal.contains(".")) {
            String sid = cookieVal.substring(0, cookieVal.indexOf('.'));
            authService.logout(sid);

            ResponseCookie clear = ResponseCookie.from(props.getRefreshCookieName(), "")
                    .httpOnly(true)
                    .secure(Boolean.TRUE.equals(props.getRefreshCookieSecure()))
                    .path(props.getRefreshCookiePath())
                    .maxAge(0)
                    .sameSite(props.getRefreshCookieSameSite())
                    .build();

            res.addHeader("Set-Cookie", clear.toString());
        }
    }

    private void setRefreshCookie(HttpServletResponse res, String value) {
        ResponseCookie cookie = ResponseCookie.from(props.getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(Boolean.TRUE.equals(props.getRefreshCookieSecure()))
                .path(props.getRefreshCookiePath())
                .maxAge(props.getRefreshTtlSeconds())
                .sameSite(props.getRefreshCookieSameSite())
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

    private String readCookie(HttpServletRequest req, String name) {
        Cookie[] cookies = req.getCookies();
        if (cookies == null)
            return null;
        for (Cookie c : cookies) {
            if (name.equals(c.getName()))
                return c.getValue();
        }

        return null;
    }

    /**
     * 비밀번호 재설정
     * POST /api/auth/reset-password
     * 휴대폰 인증(proofToken) 후 새 비밀번호 설정
     */
    @PostMapping("/reset-password")
    public org.springframework.http.ResponseEntity<Void> resetPassword(
            @jakarta.validation.Valid @RequestBody AuthDtos.PasswordResetRequest req) {
        authService.resetPassword(req.loginId(), req.proofToken(), req.newPassword());
        return org.springframework.http.ResponseEntity.ok().build();
    }

    /**
     * 아이디 찾기
     * POST /api/auth/find-id
     * 이름 + 휴대폰 인증(proofToken)으로 마스킹된 아이디 반환
     */
    @PostMapping("/find-id")
    public org.springframework.http.ResponseEntity<AuthDtos.FindIdResponse> findId(
            @jakarta.validation.Valid @RequestBody AuthDtos.FindIdRequest req) {
        String maskedId = authService.findMaskedLoginId(req.name(), req.proofToken());
        return org.springframework.http.ResponseEntity.ok(new AuthDtos.FindIdResponse(maskedId));
    }

    /**
     * 로그인 확인 (기존 세션 체크)
     * POST /api/auth/login/check
     * 기존 세션이 있으면 확인 필요, 없으면 바로 로그인
     */
    @PostMapping("/login/check")
    public AuthDtos.LoginCheckResponse checkLogin(
            @RequestBody AuthDtos.LoginRequest req,
            HttpServletResponse res,
            HttpServletRequest request) {

        DeviceInfo deviceInfo = DeviceInfo.from(request);
        AuthService.LoginCheckResult result = authService.checkLogin(req, deviceInfo);

        if (result.needsConfirmation()) {
            // 기존 세션 있음 - 확인 필요 (충돌 디바이스 정보 포함)
            return new AuthDtos.LoginCheckResponse(true, result.loginToken(), null, result.conflictDeviceInfo());
        } else {
            // 기존 세션 없음 - 바로 로그인
            AuthService.AuthResult authResult = result.authResult();
            String cookieValue = authResult.sid() + "." + authResult.refreshToken();
            setRefreshCookie(res, cookieValue);

            AuthDtos.TokenResponse tokenResponse = new AuthDtos.TokenResponse(
                    authResult.accessToken(),
                    authResult.ttl(),
                    authResult.role().name());
            return new AuthDtos.LoginCheckResponse(false, null, tokenResponse, null);
        }
    }

    /**
     * 강제 로그인 (기존 세션 종료 후 로그인)
     * POST /api/auth/login/force
     * 사용자 확인 후 기존 세션을 종료하고 새 세션 생성
     */
    @PostMapping("/login/force")
    public AuthDtos.TokenResponse forceLogin(
            @jakarta.validation.Valid @RequestBody AuthDtos.ForceLoginRequest req,
            HttpServletResponse res,
            HttpServletRequest request) {

        DeviceInfo deviceInfo = DeviceInfo.from(request);
        AuthService.AuthResult result = authService.forceLogin(req.loginToken(), deviceInfo);

        String cookieValue = result.sid() + "." + result.refreshToken();
        setRefreshCookie(res, cookieValue);

        return new AuthDtos.TokenResponse(result.accessToken(), result.ttl(), result.role().name());
    }

    /**
     * 세션 정보 조회
     * GET /api/auth/session/info
     * 현재 세션의 남은 시간 등 정보 반환
     */
    @GetMapping("/session/info")
    public AuthDtos.SessionInfoResponse getSessionInfo(HttpServletRequest req) {
        // JWT 토큰에서 sid 추출
        String token = resolveBearer(req);
        if (token == null) {
            throw new IllegalArgumentException("NO_TOKEN");
        }

        Claims claims = jwtTokenProvider.parseAndValidate(token).getPayload();
        String sid = jwtTokenProvider.getSid(claims);
        Long userId = jwtTokenProvider.getUserId(claims);

        // 세션 메타데이터 조회
        Map<String, String> sessionMeta = sessionService.getSessionMeta(sid);
        String lastSeenStr = sessionMeta.get("lastSeen");
        long lastSeen = Long.parseLong(lastSeenStr);

        // 만료 시간 계산
        long idleTtl = props.getIdleTtlSeconds();
        long expiresAt = lastSeen + idleTtl;
        long now = Instant.now().getEpochSecond();
        long remainingSeconds = Math.max(0, expiresAt - now);

        return new AuthDtos.SessionInfoResponse(
                sid,
                lastSeen,
                expiresAt,
                remainingSeconds,
                idleTtl);
    }

    private String resolveBearer(HttpServletRequest req) {
        String h = req.getHeader("Authorization");
        if (h == null || h.isBlank())
            return null;
        if (!h.startsWith("Bearer "))
            return null;
        String token = h.substring(7).trim();
        return token.isEmpty() ? null : token;
    }
}
