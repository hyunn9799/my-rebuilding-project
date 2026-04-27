package com.aicc.silverlink.domain.emergency.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * SMS 발송 이력 엔티티
 * 모든 SMS 발송 기록을 저장
 */
@Entity
@Table(name = "sms_logs",
        indexes = {
                @Index(name = "idx_sms_receiver_time", columnList = "receiver_user_id, created_at DESC"),
                @Index(name = "idx_sms_type_ref", columnList = "message_type, reference_id"),
                @Index(name = "idx_sms_status", columnList = "status, created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class SmsLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "sms_log_id")
    private Long id;

    /**
     * 수신자 (회원인 경우)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id")
    private User receiver;

    /**
     * 수신 전화번호 (E.164 형식: +821012345678)
     */
    @Column(name = "receiver_phone", nullable = false, length = 20)
    private String receiverPhone;

    /**
     * 메시지 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "message_type", nullable = false, length = 30)
    private MessageType messageType;

    /**
     * 참조 테이블 종류 (emergency_alerts, inquiries, complaints, access_requests 등)
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * 참조 테이블의 PK
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * 발송된 메시지 내용
     */
    @Column(name = "message_content", nullable = false, columnDefinition = "TEXT")
    private String messageContent;

    /**
     * 웹 연결 단축 URL
     */
    @Column(name = "short_url", length = 255)
    private String shortUrl;

    /**
     * 발송 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SmsStatus status = SmsStatus.PENDING;

    /**
     * 외부 서비스(Twilio) 메시지 ID
     */
    @Column(name = "external_msg_id", length = 100)
    private String externalMsgId;

    /**
     * 발송 시각
     */
    @Column(name = "sent_at")
    private LocalDateTime sentAt;

    /**
     * 전달 완료 시각
     */
    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    /**
     * 오류 메시지
     */
    @Column(name = "error_message", columnDefinition = "TEXT")
    private String errorMessage;

    /**
     * 생성 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = SmsStatus.PENDING;
        }
    }

    // ========== Enum 정의 ==========

    /**
     * 메시지 유형
     */
    public enum MessageType {
        EMERGENCY_CRITICAL("긴급 알림 (CRITICAL)"),
        EMERGENCY_WARNING("주의 알림 (WARNING)"),
        INQUIRY_REPLY("문의 답변"),
        COMPLAINT_REPLY("민원 답변"),
        ACCESS_APPROVED("접근권한 승인"),
        ACCESS_REJECTED("접근권한 거절"),
        SYSTEM("시스템 알림");

        private final String description;

        MessageType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * SMS 발송 상태
     */
    public enum SmsStatus {
        PENDING("대기"),
        SENT("발송"),
        DELIVERED("전달완료"),
        FAILED("실패");

        private final String description;

        SmsStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * 발송 성공 처리
     */
    public void markSent(String externalMsgId) {
        this.status = SmsStatus.SENT;
        this.sentAt = LocalDateTime.now();
        this.externalMsgId = externalMsgId;
    }

    /**
     * 전달 완료 처리
     */
    public void markDelivered() {
        this.status = SmsStatus.DELIVERED;
        this.deliveredAt = LocalDateTime.now();
    }

    /**
     * 발송 실패 처리
     */
    public void markFailed(String errorMessage) {
        this.status = SmsStatus.FAILED;
        this.errorMessage = errorMessage;
    }

    /**
     * 팩토리 메서드: 긴급 알림 SMS 로그 생성
     */
    public static SmsLog createForEmergencyAlert(
            User receiver,
            String receiverPhone,
            EmergencyAlert.Severity severity,
            Long alertId,
            String messageContent,
            String shortUrl) {

        MessageType type = severity == EmergencyAlert.Severity.CRITICAL
                ? MessageType.EMERGENCY_CRITICAL
                : MessageType.EMERGENCY_WARNING;

        return SmsLog.builder()
                .receiver(receiver)
                .receiverPhone(receiverPhone)
                .messageType(type)
                .referenceType("emergency_alerts")
                .referenceId(alertId)
                .messageContent(messageContent)
                .shortUrl(shortUrl)
                .build();
    }

    /**
     * 팩토리 메서드: 문의 답변 SMS 로그 생성
     */
    public static SmsLog createForInquiryReply(
            User receiver,
            String receiverPhone,
            Long inquiryId,
            String messageContent,
            String shortUrl) {

        return SmsLog.builder()
                .receiver(receiver)
                .receiverPhone(receiverPhone)
                .messageType(MessageType.INQUIRY_REPLY)
                .referenceType("inquiries")
                .referenceId(inquiryId)
                .messageContent(messageContent)
                .shortUrl(shortUrl)
                .build();
    }

    /**
     * 팩토리 메서드: 민원 답변 SMS 로그 생성
     */
    public static SmsLog createForComplaintReply(
            User receiver,
            String receiverPhone,
            Long complaintId,
            String messageContent,
            String shortUrl) {

        return SmsLog.builder()
                .receiver(receiver)
                .receiverPhone(receiverPhone)
                .messageType(MessageType.COMPLAINT_REPLY)
                .referenceType("complaints")
                .referenceId(complaintId)
                .messageContent(messageContent)
                .shortUrl(shortUrl)
                .build();
    }

    /**
     * 팩토리 메서드: 접근권한 승인/거절 SMS 로그 생성
     */
    public static SmsLog createForAccessRequest(
            User receiver,
            String receiverPhone,
            boolean approved,
            Long requestId,
            String messageContent,
            String shortUrl) {

        MessageType type = approved
                ? MessageType.ACCESS_APPROVED
                : MessageType.ACCESS_REJECTED;

        return SmsLog.builder()
                .receiver(receiver)
                .receiverPhone(receiverPhone)
                .messageType(type)
                .referenceType("access_requests")
                .referenceId(requestId)
                .messageContent(messageContent)
                .shortUrl(shortUrl)
                .build();
    }
}
