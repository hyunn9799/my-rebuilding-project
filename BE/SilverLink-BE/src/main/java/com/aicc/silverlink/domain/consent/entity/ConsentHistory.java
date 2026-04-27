package com.aicc.silverlink.domain.consent.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "consent_histories")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class ConsentHistory {

    @Id
    @Column(name="consent_history_id")
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "consent_type", nullable = false)
    private String consentType; // PRIVACY, SENSITIVE_INFO, MEDICATION, THIRD_PARTY

    @Column(name = "action_type", nullable = false)
    private String actionType; // AGREE, DISAGREE, WITHDRAW

    @Column(name = "ip_address")
    private String ipAddress;

    @Column(name = "user_agent")
    private String userAgent;

    @Column(name = "consent_date", nullable = false)
    private LocalDateTime consentDate;

    @PrePersist
    protected void onCreate() {
        if (this.consentDate == null) {
            this.consentDate = LocalDateTime.now();
        }
    }
}
