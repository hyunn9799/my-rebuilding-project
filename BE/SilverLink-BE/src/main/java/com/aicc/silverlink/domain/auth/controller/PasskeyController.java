package com.aicc.silverlink.domain.auth.controller;

import com.aicc.silverlink.domain.auth.dto.AuthDtos;
import com.aicc.silverlink.domain.auth.service.AuthService;
import com.aicc.silverlink.domain.auth.service.WebAuthnService;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.config.auth.AuthPolicyProperties;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseCookie;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;

@Tag(name = "Passkey 인증", description = "WebAuthn 기반 Passkey 등록/로그인 API")
@RestController
@RequestMapping("/api/auth/passkey")
@RequiredArgsConstructor
public class PasskeyController {

    private final WebAuthnService webAuthnService;
    private final AuthService authService;
    private final AuthPolicyProperties props;
    private final UserRepository userRepository;

    // ✅ 보안 강화: userId를 요청에서 받지 않고 인증 정보에서 추출
    public record StartRegReq() {
    }

    public record FinishRegReq(@NotBlank String requestId, @NotBlank String credentialJson) {
    }

    @PostMapping("/register/options")
    @PreAuthorize("isAuthenticated()")  // ✅ 인증 필수
    @Operation(
        summary = "Passkey 등록 시작 (인증 필요)",
        description = "로그인한 사용자만 자신의 계정에 Passkey를 등록할 수 있습니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public WebAuthnService.StartRegResponse startReg(
            @AuthenticationPrincipal Long userId  // ✅ 인증된 사용자 ID 자동 추출
    ) throws JsonProcessingException {
        return webAuthnService.startRegistration(userId);
    }

    @PostMapping("/register/verify")
    @PreAuthorize("isAuthenticated()")  // ✅ 인증 필수
    @Operation(
        summary = "Passkey 등록 완료 (인증 필요)",
        description = "Passkey 등록을 완료합니다. 로그인한 사용자만 자신의 계정에 등록할 수 있습니다.",
        security = @SecurityRequirement(name = "bearerAuth")
    )
    public void finishReg(
            @AuthenticationPrincipal Long userId,  // ✅ 인증된 사용자 ID 자동 추출
            @RequestBody FinishRegReq req
    ) {
        // ✅ 인증된 사용자 ID로만 등록 가능 (타인 계정 등록 불가)
        webAuthnService.finishRegistration(userId, req.requestId(), req.credentialJson(), userId);
    }

    public record StartLoginReq(String loginId) {
    }

    public record FinishLoginReq(@NotBlank String requestId, @NotBlank String credentialJson) {
    }

    @PostMapping("/login/options")
    public WebAuthnService.StartAuthResponse startLogin(@RequestBody StartLoginReq req) throws JsonProcessingException {
        return webAuthnService.startAssertion(req.loginId());
    }

    /**
     * Passkey 로그인 완료 - 토큰 + 사용자 프로필 반환 (추가 API 호출 불필요)
     */
    @PostMapping("/login/verify")
    public AuthDtos.PasskeyLoginResponse finishLogin(
            @Valid @RequestBody FinishLoginReq req,
            HttpServletRequest http,
            HttpServletResponse res) {
        Long userId = webAuthnService.finishAssertion(req.requestId(), req.credentialJson());
        AuthService.AuthResult result = authService.issueForUser(userId, http);

        // 사용자 정보 조회
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        setRefreshCookie(res, result.sid() + "." + result.refreshToken());

        // 토큰 + 사용자 프로필 함께 반환
        AuthDtos.UserProfile userProfile = new AuthDtos.UserProfile(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getRole().name());
        return new AuthDtos.PasskeyLoginResponse(result.accessToken(), result.ttl(), userProfile);
    }

    private void setRefreshCookie(HttpServletResponse res, String value) {
        ResponseCookie cookie = ResponseCookie.from(props.getRefreshCookieName(), value)
                .httpOnly(true)
                .secure(true)
                .path(props.getRefreshCookiePath())
                .maxAge(props.getRefreshTtlSeconds())
                .sameSite(props.getRefreshCookieSameSite())
                .build();
        res.addHeader("Set-Cookie", cookie.toString());
    }

}
