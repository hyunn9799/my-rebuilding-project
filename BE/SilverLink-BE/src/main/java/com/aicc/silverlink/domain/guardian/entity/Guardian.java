package com.aicc.silverlink.domain.guardian.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "guardians")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Guardian {
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "address_line1", length = 200)
    private String addressLine1;

    @Column(name = "address_line2", length = 200)
    private String addressLine2;

    @Column(name = "zipcode", length = 10)
    private String zipcode;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ✅ 주소 정보 수정을 위한 메서드 추가
    public void updateAddress(String addressLine1, String addressLine2, String zipcode) {
        this.addressLine1 = addressLine1;
        this.addressLine2 = addressLine2;
        this.zipcode = zipcode;
    }

    public static Guardian create(User user, String addressLine1, String addressLine2, String zipcode, LocalDateTime createdAt) {
        return Guardian.builder()
                .user(user)
                .addressLine1(addressLine1)
                .addressLine2(addressLine2)
                .zipcode(zipcode)
                .createdAt(createdAt)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}