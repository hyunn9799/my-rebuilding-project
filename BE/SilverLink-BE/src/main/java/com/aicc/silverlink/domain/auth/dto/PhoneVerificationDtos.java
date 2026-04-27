package com.aicc.silverlink.domain.auth.dto;

import com.aicc.silverlink.domain.auth.entity.PhoneVerification;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDateTime;

public class PhoneVerificationDtos {

        public record RequestCodeRequest(
                        @NotBlank String phone,
                        @NotNull PhoneVerification.Purpose purpose,
                        Long userId) {
        }

        public record RequestCodeResponse(
                        Long verificationId,
                        LocalDateTime expireAt,
                        Long expiresInSeconds,
                        String debugCode // 개발 테스트용
        ) {
        }

        public record VerifyCodeRequest(
                        @NotNull Long verificationId,
                        @NotBlank String code

        ) {
        }

        public record VerifyCodeResponse(
                        boolean verified,
                        String proofToken) {
        }
}
