package com.aicc.silverlink.domain.consent.entity;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 민감정보 접근 권한 요청 엔티티
 *
 * 보호자가 어르신의 민감정보(건강정보, 복약정보, 통화기록)를
 * 열람하기 위해 관리자에게 승인을 요청하는 프로세스를 관리합니다.
 *
 * 필요 서류:
 * 1. 어르신의 '민감정보 제3자 제공 동의서'
 * 2. 가족관계증명서
 */
@Entity
@Table(name = "access_requests")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AccessRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "access_request_id")
    private Long id;

    /**
     * 요청자 (보호자)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "requester_user_id", nullable = false)
    private User requester;

    /**
     * 대상 어르신
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private Elderly elderly;

    /**
     * 접근 범위 (복약정보, 건강정보, 통화기록)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "scope", nullable = false)
    private AccessScope scope;

    /**
     * 요청 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private AccessRequestStatus status;

    /**
     * 서류 확인 여부 (동의서 + 가족관계증명서)
     */
    @Column(name = "is_document_verified", nullable = false)
    private boolean documentVerified;

    /**
     * 검토한 관리자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "reviewed_by_admin_user_id")
    private Admin reviewedByAdmin;

    /**
     * 요청 일시
     */
    @Column(name = "requested_at", nullable = false, updatable = false)
    private LocalDateTime requestedAt;

    /**
     * 결정 일시 (승인/거절)
     */
    @Column(name = "decided_at")
    private LocalDateTime decidedAt;

    /**
     * 권한 만료 일시
     */
    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    /**
     * 철회 일시
     */
    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    /**
     * 결정 사유 (거절 시 거절 사유 등)
     */
    @Column(name = "decision_note", length = 500)
    private String decisionNote;

    /**
     * 접근 범위 Enum
     */
    public enum AccessScope {
        MEDICATION("복약정보"),
        HEALTH_INFO("건강정보"),
        CALL_RECORDS("통화기록");

        private final String description;

        AccessScope(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 요청 상태 Enum
     */
    public enum AccessRequestStatus {
        PENDING("대기중"),
        APPROVED("승인됨"),
        REJECTED("거절됨"),
        REVOKED("철회됨"),
        EXPIRED("만료됨"),
        CANCELLED("취소됨");

        private final String description;

        AccessRequestStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.requestedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AccessRequestStatus.PENDING;
        }
        if (!this.documentVerified) {
            this.documentVerified = false;
        }
    }

    // ========== 팩토리 메서드 ==========

    /**
     * 접근 권한 요청 생성
     */
    public static AccessRequest create(User requester, Elderly elderly, AccessScope scope) {
        if (requester == null) throw new IllegalArgumentException("REQUESTER_REQUIRED");
        if (elderly == null) throw new IllegalArgumentException("ELDERLY_REQUIRED");
        if (scope == null) throw new IllegalArgumentException("SCOPE_REQUIRED");

        return AccessRequest.builder()
                .requester(requester)
                .elderly(elderly)
                .scope(scope)
                .status(AccessRequestStatus.PENDING)
                .documentVerified(false)
                .build();
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * 서류 확인 완료 처리
     */
    public void verifyDocuments() {
        this.documentVerified = true;
    }

    /**
     * 관리자가 요청 승인
     * @param admin 승인하는 관리자
     * @param expiresAt 권한 만료일 (null이면 무기한)
     * @param note 승인 메모
     */
    public void approve(Admin admin, LocalDateTime expiresAt, String note) {
        validatePendingStatus();
        validateDocumentVerified();

        this.status = AccessRequestStatus.APPROVED;
        this.reviewedByAdmin = admin;
        this.decidedAt = LocalDateTime.now();
        this.expiresAt = expiresAt;
        this.decisionNote = note;
    }

    /**
     * 관리자가 요청 거절
     * @param admin 거절하는 관리자
     * @param reason 거절 사유
     */
    public void reject(Admin admin, String reason) {
        validatePendingStatus();

        this.status = AccessRequestStatus.REJECTED;
        this.reviewedByAdmin = admin;
        this.decidedAt = LocalDateTime.now();
        this.decisionNote = reason;
    }

    /**
     * 권한 철회 (어르신 또는 관리자가 수행)
     */
    public void revoke(String reason) {
        if (this.status != AccessRequestStatus.APPROVED) {
            throw new IllegalStateException("ONLY_APPROVED_CAN_BE_REVOKED");
        }

        this.status = AccessRequestStatus.REVOKED;
        this.revokedAt = LocalDateTime.now();
        this.decisionNote = reason;
    }

    /**
     * 요청자가 요청 취소
     */
    public void cancel() {
        validatePendingStatus();
        this.status = AccessRequestStatus.CANCELLED;
    }

    /**
     * 권한 만료 처리
     */
    public void expire() {
        if (this.status != AccessRequestStatus.APPROVED) {
            throw new IllegalStateException("ONLY_APPROVED_CAN_EXPIRE");
        }
        this.status = AccessRequestStatus.EXPIRED;
    }

    /**
     * 현재 접근 권한이 유효한지 확인
     */
    public boolean isAccessGranted() {
        if (this.status != AccessRequestStatus.APPROVED) {
            return false;
        }
        if (this.expiresAt != null && LocalDateTime.now().isAfter(this.expiresAt)) {
            return false;
        }
        return true;
    }

    // ========== 검증 메서드 ==========

    private void validatePendingStatus() {
        if (this.status != AccessRequestStatus.PENDING) {
            throw new IllegalStateException("REQUEST_NOT_PENDING");
        }
    }

    private void validateDocumentVerified() {
        if (!this.documentVerified) {
            throw new IllegalStateException("DOCUMENTS_NOT_VERIFIED");
        }
    }
}