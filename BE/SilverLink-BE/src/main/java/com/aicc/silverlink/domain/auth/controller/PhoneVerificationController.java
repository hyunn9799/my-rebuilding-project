package com.aicc.silverlink.domain.auth.controller;

import com.aicc.silverlink.domain.auth.dto.PhoneVerificationDtos;
import com.aicc.silverlink.domain.auth.service.PhoneVerificationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "휴대폰 인증", description = "SMS 인증 요청/확인 API")
@RestController
@RequestMapping("/api/auth/phone")
@RequiredArgsConstructor
public class PhoneVerificationController {

    private final PhoneVerificationService service;

    @PostMapping("/request")
    public PhoneVerificationDtos.RequestCodeResponse request(
            @Valid @RequestBody PhoneVerificationDtos.RequestCodeRequest req,
            HttpServletRequest http) {
        return service.requestCode(req, ip(http));
    }

    @PostMapping("/verify")
    public PhoneVerificationDtos.VerifyCodeResponse verify(
            @Valid @RequestBody PhoneVerificationDtos.VerifyCodeRequest req,
            HttpServletRequest http) {
        return service.verifyCode(req, ip(http));
    }

    private String ip(HttpServletRequest r) {
        String xff = r.getHeader("X-Forwarded-For");
        return (xff != null && !xff.isBlank() ? xff.split(",")[0].trim() : r.getRemoteAddr());
    }
}
