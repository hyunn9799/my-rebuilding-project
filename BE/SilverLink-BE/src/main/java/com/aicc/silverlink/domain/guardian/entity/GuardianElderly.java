package com.aicc.silverlink.domain.guardian.entity;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "guardian_elderly")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class GuardianElderly {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "guardian_elderly_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "guardian_user_id", nullable = false, unique = true)
    private Guardian guardian;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false, unique = true)
    private Elderly elderly;

    @Enumerated(EnumType.STRING)
    @Column(name = "relation_type", nullable = false)
    private RelationType relationType;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public static GuardianElderly create(Guardian guardian, Elderly elderly, RelationType relationType,
            LocalDateTime createdAt) {

        return GuardianElderly.builder()
                .guardian(guardian)
                .elderly(elderly)
                .relationType(relationType)
                .createdAt(LocalDateTime.now())
                .build();
    }
}
