package com.aicc.silverlink.domain.auth.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "webauthn_credentials")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class WebAuthnCredential {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "webauthn_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "credential_id", nullable = false, unique = true, length = 255)
    private String credentialId;

    @Column(name = "rp_id", nullable = false, length = 255)
    private String rpId;

    @Lob
    @Column(name="public_key", columnDefinition="BLOB", nullable=false)
    private byte[] publicKey;

    @Column(name = "sign_count", nullable = false)
    private long signCount;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "last_used_at")
    private LocalDateTime lastUsedAt;

    @Column(name = "revoked_at")
    private LocalDateTime revokedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "registered_by")
    private User registeredBy;


    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.lastUsedAt == null) {
            this.lastUsedAt = LocalDateTime.now(); // 생성 시점도 사용으로 간주
        }
    }

    public static WebAuthnCredential register(User user, String credentialId, String rpId, byte[] publicKey, long signCount, User registeredBy) {
        return WebAuthnCredential.builder()
                .user(user)
                .credentialId(credentialId)
                .rpId(rpId)
                .publicKey(publicKey)
                .signCount(signCount)
                .registeredBy(registeredBy)
                .build();
    }

    public void updateUsage(long newSignCount) {
        this.signCount = newSignCount;
        this.lastUsedAt = LocalDateTime.now();
    }
}
