package com.aicc.silverlink.domain.policy.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "policies", indexes = {
        @Index(name = "idx_policy_key", columnList = "policy_key")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
@Builder
@AllArgsConstructor
public class Policy {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "policy_id")
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(name = "policy_key", nullable = false, length = 50)
    private PolicyType policyType;

    @Column(name = "version", nullable = false, length = 20)
    private String version;

    @Lob
    @Column(name = "content", nullable = false, columnDefinition = "LONGTEXT")
    private String content;

    @Column(name = "is_mandatory", nullable = false)
    private boolean isMandatory;

    // 💡 추가: DDL에 있던 설명 필드 (nullable)
    @Column(name = "description")
    private String description;

    @CreatedDate
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    // 💡 [핵심 변경사항] updatedAt 필드 추가
    @LastModifiedDate
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    // 💡 [핵심 변경사항] create 메서드에서 두 날짜를 모두 초기화
    public static Policy create(PolicyType policyType, String version, String content,
                                boolean isMandatory, String description, User createdBy) {
        LocalDateTime now = LocalDateTime.now();
        return Policy.builder()
                .policyType(policyType)
                .version(version)
                .content(content)
                .isMandatory(isMandatory)
                .description(description) // 💡 추가
                .createdBy(createdBy)
                .createdAt(now)
                .updatedAt(now)
                .build();
    }
}