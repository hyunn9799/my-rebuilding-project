package com.aicc.silverlink.domain.auth.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Entity
@Table(name = "phone_verifications")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PhoneVerification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "verification_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "phone_e164", nullable = false, length = 20)
    private String phoneE164;

    @Enumerated(EnumType.STRING)
    @Column(name = "purpose", nullable = false)
    private Purpose purpose;

    @Column(name = "code_hash", length = 255)
    private String codeHash;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private Status status;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Column(name = "verified_at")
    private LocalDateTime verifiedAt;

    @Column(name = "fail_count", nullable = false)
    private int failCount;

    @Column(name = "request_ip", length = 45)
    private String requestIp;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    private PhoneVerification(
            User user,
            String phoneE164,
            Purpose purpose,
            String codeHash,
            LocalDateTime expiresAt,
            String requestIp,
            Status status,
            int failCount) {
        this.user = user;
        this.phoneE164 = phoneE164;
        this.purpose = purpose;
        this.codeHash = codeHash;
        this.expiresAt = expiresAt;
        this.requestIp = requestIp;
        this.failCount = failCount;
        this.status = status;
        this.failCount = 0;
    }

    public static PhoneVerification create(User user, String phoneE164, Purpose purpose, String codeHash,
            String requestIp, long ttlSeconds) {
        return PhoneVerification.builder()
                .user(user)
                .phoneE164(phoneE164)
                .purpose(purpose)
                // Twilio Verify API 사용 시 codeHash는 null이 올 수 있음 → 플레이스홀더 저장
                .codeHash(codeHash != null ? codeHash : "TWILIO_MANAGED")
                .requestIp(requestIp)
                .expiresAt(LocalDateTime.now().plusSeconds(ttlSeconds))
                .build();
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = Status.REQUESTED;
        }
    }

    public enum Purpose {
        SIGNUP, DEVICE_REGISTRATION, PASSWORD_RESET
    }

    public enum Status {
        REQUESTED, VERIFIED, EXPIRED, FAILED
    }

    public void increaseFailCount() {
        this.failCount++;
    }

    public void verify() {
        this.status = Status.VERIFIED;
        this.verifiedAt = LocalDateTime.now();
    }

    public void expire() {
        this.status = Status.EXPIRED;
    }

    public void fail() {
        this.status = Status.FAILED;
    }

}
