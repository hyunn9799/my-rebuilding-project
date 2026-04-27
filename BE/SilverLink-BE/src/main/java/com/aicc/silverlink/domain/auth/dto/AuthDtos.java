package com.aicc.silverlink.domain.auth.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;

public class AuthDtos {

        public record LoginRequest(
                        @Schema(description = "사용자 로그인 ID", example = "silverlink111") @NotBlank(message = "로그인 ID는 필수입니다.") String loginId,

                        @Schema(description = "사용자 비밀번호", example = "pass1234!") @NotBlank(message = "비밀번호는 필수입니다.") String password) {
        }

        public record TokenResponse(
                        @Schema(description = "Access Token 값") String accessToken,

                        @Schema(description = "토큰 만료 시간(초)") long expiresInSeconds,

                        @Schema(description = "사용자 권한 (ADMIN, GUARDIAN 등)") String role) {
        }

        public record RefreshResponse(
                        @Schema(description = "새로 발급된 Access Token") String accessToken,

                        @Schema(description = "토큰 만료 시간(초)") long expiresInSeconds) {
        }

        public record PhoneLoginRequest(
                        @Schema(description = "휴대폰 번호 (하이픈 없이)", example = "01012345678") @NotBlank(message = "휴대폰 번호는 필수입니다.") String phone,

                        @Schema(description = "휴대폰 인증 성공 후 발급된 proofToken") @NotBlank(message = "인증 토큰은 필수입니다.") String proofToken) {
        }

        // 비밀번호 재설정 요청
        public record PasswordResetRequest(
                        @Schema(description = "로그인 ID", example = "silverlink111") @NotBlank String loginId,
                        @Schema(description = "휴대폰 인증 proofToken") @NotBlank String proofToken,
                        @Schema(description = "새 비밀번호", example = "newPass1234!") @NotBlank String newPassword) {
        }

        // 아이디 찾기 요청
        public record FindIdRequest(
                        @Schema(description = "이름") @NotBlank String name,
                        @Schema(description = "휴대폰 인증 proofToken") @NotBlank String proofToken) {
        }

        // 아이디 찾기 응답
        public record FindIdResponse(
                        @Schema(description = "마스킹된 로그인 ID", example = "user***") String maskedLoginId) {
        }

        // 충돌하는 기존 세션의 디바이스 정보 (마스킹된 IP, 기기 요약, 로그인 시간)
        public record ConflictDeviceInfo(
                        @Schema(description = "마스킹된 IP 주소", example = "192.168.1.***") String maskedIp,
                        @Schema(description = "기기 요약 (브라우저 + OS)", example = "Chrome/120.0 (Windows)") String deviceSummary,
                        @Schema(description = "로그인 시간 (Unix timestamp, 초)") long loginAt) {
        }

        // 로그인 확인 응답 (기존 세션 체크)
        public record LoginCheckResponse(
                        @Schema(description = "기존 세션 존재 여부 (true면 확인 필요)") boolean needsConfirmation,
                        @Schema(description = "임시 로그인 토큰 (needsConfirmation=true일 때만)") String loginToken,
                        @Schema(description = "토큰 정보 (needsConfirmation=false일 때만)") TokenResponse tokenResponse,
                        @Schema(description = "충돌하는 기존 세션의 디바이스 정보 (needsConfirmation=true일 때만)") ConflictDeviceInfo conflictDevice) {
        }

        // 강제 로그인 요청
        public record ForceLoginRequest(
                        @Schema(description = "로그인 확인 시 받은 임시 토큰") @NotBlank String loginToken) {
        }

        // 세션 정보 응답
        public record SessionInfoResponse(
                        @Schema(description = "세션 ID") String sid,
                        @Schema(description = "마지막 활동 시간 (Unix timestamp, 초)") long lastSeen,
                        @Schema(description = "만료 시간 (Unix timestamp, 초)") long expiresAt,
                        @Schema(description = "남은 시간 (초)") long remainingSeconds,
                        @Schema(description = "Idle TTL (초)") long idleTtl) {
        }

        // Passkey(지문) 로그인 응답 - 토큰 + 사용자 프로필 포함
        public record PasskeyLoginResponse(
                        @Schema(description = "Access Token 값") String accessToken,
                        @Schema(description = "토큰 만료 시간(초)") long expiresInSeconds,
                        @Schema(description = "사용자 정보") UserProfile user) {
        }

        // Passkey 로그인용 사용자 프로필
        public record UserProfile(
                        @Schema(description = "사용자 ID") Long id,
                        @Schema(description = "사용자 이름") String name,
                        @Schema(description = "휴대폰 번호") String phone,
                        @Schema(description = "사용자 권한") String role) {
        }
}
