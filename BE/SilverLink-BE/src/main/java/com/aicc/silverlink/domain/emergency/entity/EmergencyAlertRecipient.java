package com.aicc.silverlink.domain.emergency.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 긴급 알림 수신자 엔티티
 * 긴급 알림을 받는 사용자 정보 (관리자, 상담사, 보호자)
 */
@Entity
@Table(name = "emergency_alert_recipients",
        indexes = {
                @Index(name = "idx_ear_receiver_read", columnList = "receiver_user_id, is_read, created_at DESC"),
                @Index(name = "idx_ear_alert", columnList = "alert_id")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uq_ear_alert_receiver", columnNames = {"alert_id", "receiver_user_id"})
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmergencyAlertRecipient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "recipient_id")
    private Long id;

    /**
     * 긴급 알림
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "alert_id", nullable = false)
    @Setter
    private EmergencyAlert emergencyAlert;

    /**
     * 수신자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    /**
     * 수신자 역할 (ADMIN, COUNSELOR, GUARDIAN)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "receiver_role", nullable = false, length = 20)
    private ReceiverRole receiverRole;

    /**
     * 웹 알림 읽음 여부
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private boolean isRead = false;

    /**
     * 읽은 시간
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * SMS 발송 대상 여부
     */
    @Column(name = "sms_required", nullable = false)
    @Builder.Default
    private boolean smsRequired = false;

    /**
     * SMS 발송 여부
     */
    @Column(name = "sms_sent", nullable = false)
    @Builder.Default
    private boolean smsSent = false;

    /**
     * SMS 발송 시간
     */
    @Column(name = "sms_sent_at")
    private LocalDateTime smsSentAt;

    /**
     * SMS 발송 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sms_delivery_status", length = 20)
    private SmsDeliveryStatus smsDeliveryStatus;

    /**
     * 생성 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    // ========== Enum 정의 ==========

    /**
     * 수신자 역할
     */
    public enum ReceiverRole {
        ADMIN("관리자"),
        COUNSELOR("상담사"),
        GUARDIAN("보호자");

        private final String description;

        ReceiverRole(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * SMS 발송 상태
     */
    public enum SmsDeliveryStatus {
        PENDING("대기"),
        SENT("발송"),
        DELIVERED("전달완료"),
        FAILED("실패");

        private final String description;

        SmsDeliveryStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * 읽음 처리
     */
    public void markAsRead() {
        if (!this.isRead) {
            this.isRead = true;
            this.readAt = LocalDateTime.now();
        }
    }

    /**
     * SMS 발송 처리
     */
    public void markSmsSent() {
        this.smsSent = true;
        this.smsSentAt = LocalDateTime.now();
        this.smsDeliveryStatus = SmsDeliveryStatus.SENT;
    }

    /**
     * SMS 발송 실패 처리
     */
    public void markSmsFailed() {
        this.smsSent = false;
        this.smsDeliveryStatus = SmsDeliveryStatus.FAILED;
    }

    /**
     * SMS 전달 완료 처리 (Webhook 콜백 시)
     */
    public void markSmsDelivered() {
        this.smsDeliveryStatus = SmsDeliveryStatus.DELIVERED;
    }

    /**
     * 팩토리 메서드: 수신자 생성
     */
    public static EmergencyAlertRecipient create(
            EmergencyAlert alert,
            User receiver,
            ReceiverRole role,
            boolean smsRequired) {

        EmergencyAlertRecipient recipient = EmergencyAlertRecipient.builder()
                .emergencyAlert(alert)
                .receiver(receiver)
                .receiverRole(role)
                .smsRequired(smsRequired)
                .smsDeliveryStatus(smsRequired ? SmsDeliveryStatus.PENDING : null)
                .build();

        return recipient;
    }
}
