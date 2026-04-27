package com.aicc.silverlink.domain.consent.dto;

import com.aicc.silverlink.domain.consent.entity.AccessRequest;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessRequestStatus;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

public class AccessRequestDto {

    // ========== Request DTOs ==========

    /**
     * 접근 권한 요청 생성 DTO
     * 보호자가 어르신의 민감정보 열람을 요청할 때 사용
     */
    public record CreateRequest(
            @NotNull(message = "어르신 ID는 필수입니다.") Long elderlyUserId,

            @NotNull(message = "접근 범위는 필수입니다.") AccessScope scope) {
    }

    /**
     * 서류 확인 완료 처리 DTO
     * 관리자가 동의서와 가족관계증명서를 확인했음을 표시
     */
    public record VerifyDocumentsRequest(
            @NotNull(message = "요청 ID는 필수입니다.") Long accessRequestId) {
    }

    /**
     * 접근 권한 승인 DTO
     */
    public record ApproveRequest(
            @NotNull(message = "요청 ID는 필수입니다.") Long accessRequestId,

            /**
             * 권한 만료일 (null이면 무기한)
             * 권장: 1년 후 만료
             */
            LocalDateTime expiresAt,

            @Size(max = 500, message = "승인 메모는 500자 이내로 입력해주세요.") String note) {
    }

    /**
     * 접근 권한 거절 DTO
     */
    public record RejectRequest(
            @NotNull(message = "요청 ID는 필수입니다.") Long accessRequestId,

            @NotNull(message = "거절 사유는 필수입니다.") @Size(min = 10, max = 500, message = "거절 사유는 10자 이상 500자 이내로 입력해주세요.") String reason) {
    }

    /**
     * 접근 권한 철회 DTO
     * 어르신 또는 관리자가 승인된 권한을 철회할 때 사용
     */
    public record RevokeRequest(
            @NotNull(message = "요청 ID는 필수입니다.") Long accessRequestId,

            @Size(max = 500, message = "철회 사유는 500자 이내로 입력해주세요.") String reason) {
    }

    // ========== Response DTOs ==========

    /**
     * 접근 권한 요청 응답 DTO
     */
    public record AccessRequestResponse(
            Long id,
            RequesterInfo requester,
            ElderlyInfo elderly,
            String scope,
            String scopeDescription,
            String status,
            String statusDescription,
            boolean documentVerified,
            ReviewerInfo reviewer,
            LocalDateTime requestedAt,
            LocalDateTime decidedAt,
            LocalDateTime expiresAt,
            LocalDateTime revokedAt,
            String decisionNote,
            boolean accessGranted) {
        public static AccessRequestResponse from(AccessRequest ar) {
            return new AccessRequestResponse(
                    ar.getId(),
                    RequesterInfo.from(ar.getRequester()),
                    ElderlyInfo.from(ar.getElderly()),
                    ar.getScope().name(),
                    ar.getScope().getDescription(),
                    ar.getStatus().name(),
                    ar.getStatus().getDescription(),
                    ar.isDocumentVerified(),
                    ar.getReviewedByAdmin() != null ? ReviewerInfo.from(ar.getReviewedByAdmin())
                            : null,
                    ar.getRequestedAt(),
                    ar.getDecidedAt(),
                    ar.getExpiresAt(),
                    ar.getRevokedAt(),
                    ar.getDecisionNote(),
                    ar.isAccessGranted());
        }
    }

    /**
     * 요청자(보호자) 정보
     */
    public record RequesterInfo(
            Long userId,
            String name,
            String phone,
            String email) {
        public static RequesterInfo from(com.aicc.silverlink.domain.user.entity.User user) {
            return new RequesterInfo(
                    user.getId(),
                    user.getName(),
                    user.getPhone(),
                    user.getEmail());
        }
    }

    /**
     * 어르신 정보
     */
    public record ElderlyInfo(
            Long userId,
            String name,
            String phone,
            Long admCode,
            String sidoName,
            String sigunguName,
            String dongName,
            String fullAddress) {
        public static ElderlyInfo from(com.aicc.silverlink.domain.elderly.entity.Elderly elderly) {
            AdministrativeDivision division = elderly.getAdministrativeDivision();
            return new ElderlyInfo(
                    elderly.getId(),
                    elderly.getUser().getName(),
                    elderly.getUser().getPhone(),
                    elderly.getAdmCode(),
                    division != null ? division.getSidoName() : null,
                    division != null ? division.getSigunguName() : null,
                    division != null ? division.getDongName() : null,
                    division != null ? division.getFullAddress() : null);
        }
    }

    /**
     * 검토자(관리자) 정보
     */
    public record ReviewerInfo(
            Long userId,
            String name) {
        public static ReviewerInfo from(com.aicc.silverlink.domain.admin.entity.Admin admin) {
            return new ReviewerInfo(
                    admin.getUserId(),
                    admin.getUser().getName());
        }
    }

    /**
     * 접근 권한 요청 목록 응답 DTO (간략 정보)
     */
    public record AccessRequestSummary(
            Long id,
            String requesterName,
            String elderlyName,
            String scope,
            String scopeDescription,
            String status,
            String statusDescription,
            boolean documentVerified,
            LocalDateTime requestedAt,
            LocalDateTime decidedAt,
            String decisionNote,
            String reviewedBy,
            boolean accessGranted) {
        public static AccessRequestSummary from(AccessRequest ar) {
            return new AccessRequestSummary(
                    ar.getId(),
                    ar.getRequester().getName(),
                    ar.getElderly().getUser().getName(),
                    ar.getScope().name(),
                    ar.getScope().getDescription(),
                    ar.getStatus().name(),
                    ar.getStatus().getDescription(),
                    ar.isDocumentVerified(),
                    ar.getRequestedAt(),
                    ar.getDecidedAt(),
                    ar.getDecisionNote(),
                    ar.getReviewedByAdmin() != null ? ar.getReviewedByAdmin().getUser().getName()
                            : null,
                    ar.isAccessGranted());
        }
    }

    /**
     * 접근 권한 확인 결과 DTO
     */
    public record AccessCheckResult(
            boolean hasAccess,
            String scope,
            LocalDateTime grantedAt,
            LocalDateTime expiresAt,
            String message) {
        public static AccessCheckResult granted(AccessRequest ar) {
            return new AccessCheckResult(
                    true,
                    ar.getScope().name(),
                    ar.getDecidedAt(),
                    ar.getExpiresAt(),
                    "접근 권한이 있습니다.");
        }

        public static AccessCheckResult denied(String message) {
            return new AccessCheckResult(
                    false,
                    null,
                    null,
                    null,
                    message);
        }
    }

    /**
     * 대기 중인 요청 통계 (관리자 대시보드용)
     */
    public record PendingRequestStats(
            long totalPending,
            long documentVerifiedPending,
            long documentNotVerifiedPending) {
    }
}