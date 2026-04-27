package com.aicc.silverlink.domain.notification.entity;

import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 일반 알림 엔티티
 *
 * 문의 답변, 민원 답변, 접근권한, 담당 배정, 공지사항 등의 알림
 */
@Entity
@Table(name = "notifications", indexes = {
        @Index(name = "idx_notif_receiver_read", columnList = "receiver_user_id, is_read, created_at DESC"),
        @Index(name = "idx_notif_type", columnList = "notification_type"),
        @Index(name = "idx_notif_reference", columnList = "reference_type, reference_id")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Notification extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "notification_id")
    private Long id;

    /**
     * 수신자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "receiver_user_id", nullable = false)
    private User receiver;

    /**
     * 알림 유형
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "notification_type", nullable = false, length = 30)
    private NotificationType notificationType;

    /**
     * 알림 제목
     */
    @Column(nullable = false, length = 200)
    private String title;

    /**
     * 알림 내용
     */
    @Column(columnDefinition = "TEXT")
    private String content;

    /**
     * 참조 테이블 타입 (inquiries, complaints, access_requests 등)
     */
    @Column(name = "reference_type", length = 50)
    private String referenceType;

    /**
     * 참조 테이블의 PK
     */
    @Column(name = "reference_id")
    private Long referenceId;

    /**
     * 이동 URL (클릭 시 이동할 경로)
     */
    @Column(name = "link_url", length = 500)
    private String linkUrl;

    /**
     * 읽음 여부
     */
    @Column(name = "is_read", nullable = false)
    @Builder.Default
    private Boolean isRead = false;

    /**
     * 읽은 시간
     */
    @Column(name = "read_at")
    private LocalDateTime readAt;

    /**
     * SMS 발송 여부
     */
    @Column(name = "sms_sent", nullable = false)
    @Builder.Default
    private Boolean smsSent = false;

    /**
     * SMS 발송 시간
     */
    @Column(name = "sms_sent_at")
    private LocalDateTime smsSentAt;

    // ========== 알림 유형 Enum ==========

    @Getter
    @RequiredArgsConstructor
    public enum NotificationType {
        // 문의/민원 관련
        INQUIRY_NEW("새 문의 등록", true),
        INQUIRY_REPLY("문의 답변 등록", true),
        COMPLAINT_NEW("새 민원 등록", true),
        COMPLAINT_REPLY("민원 답변 등록", true),

        // 접근권한 관련
        ACCESS_REQUEST_NEW("접근권한 요청", false),
        ACCESS_APPROVED("접근권한 승인", true),
        ACCESS_REJECTED("접근권한 거절", true),

        // 담당 배정 관련
        ASSIGNMENT_NEW("담당 어르신 배정", false),
        ASSIGNMENT_CHANGED("담당 변경", false),

        // 공지사항
        NOTICE_NEW("새 공지사항", false),

        // 긴급 알림 연계
        EMERGENCY_NEW("긴급 알림 발생", true),
        EMERGENCY_RESOLVED("긴급 알림 처리 완료", false),

        // 상담사 코멘트
        COUNSELOR_COMMENT("상담사 코멘트", false),

        // 시스템 알림
        SYSTEM("시스템 알림", false);

        private final String description;
        private final boolean smsRequired; // 기본 SMS 발송 여부
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
     * SMS 발송 완료 처리
     */
    public void markSmsSent() {
        this.smsSent = true;
        this.smsSentAt = LocalDateTime.now();
    }

    /**
     * 읽지 않은 알림인지 확인
     */
    public boolean isUnread() {
        return !this.isRead;
    }

    // ========== 팩토리 메서드 ==========

    /**
     * 문의 답변 알림 생성
     */
    public static Notification createInquiryReplyNotification(User receiver, Long inquiryId, String inquiryTitle) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.INQUIRY_REPLY)
                .title("문의에 답변이 등록되었습니다")
                .content("'" + truncate(inquiryTitle, 30) + "' 문의에 답변이 등록되었습니다.")
                .referenceType("inquiries")
                .referenceId(inquiryId)
                .linkUrl("/inquiries/" + inquiryId)
                .build();
    }

    /**
     * 민원 답변 알림 생성
     */
    public static Notification createComplaintReplyNotification(User receiver, Long complaintId,
            String complaintTitle) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.COMPLAINT_REPLY)
                .title("민원에 답변이 등록되었습니다")
                .content("'" + truncate(complaintTitle, 30) + "' 민원에 답변이 등록되었습니다.")
                .referenceType("complaints")
                .referenceId(complaintId)
                .linkUrl("/complaints/" + complaintId)
                .build();
    }

    /**
     * 접근권한 승인 알림 생성
     */
    public static Notification createAccessApprovedNotification(User receiver, Long requestId, String elderlyName) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.ACCESS_APPROVED)
                .title("접근권한이 승인되었습니다")
                .content(elderlyName + " 어르신의 민감정보 열람 권한이 승인되었습니다.")
                .referenceType("access_requests")
                .referenceId(requestId)
                .linkUrl("/access-requests/" + requestId)
                .build();
    }

    /**
     * 접근권한 거절 알림 생성
     */
    public static Notification createAccessRejectedNotification(User receiver, Long requestId, String elderlyName,
            String reason) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.ACCESS_REJECTED)
                .title("접근권한이 거절되었습니다")
                .content(elderlyName + " 어르신의 민감정보 열람 권한 요청이 거절되었습니다. 사유: " + truncate(reason, 50))
                .referenceType("access_requests")
                .referenceId(requestId)
                .linkUrl("/access-requests/" + requestId)
                .build();
    }

    /**
     * 새 접근권한 요청 알림 (관리자에게)
     */
    public static Notification createAccessRequestNotification(User receiver, Long requestId, String requesterName,
            String elderlyName) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.ACCESS_REQUEST_NEW)
                .title("새 접근권한 요청")
                .content(requesterName + "님이 " + elderlyName + " 어르신의 민감정보 열람을 요청했습니다.")
                .referenceType("access_requests")
                .referenceId(requestId)
                .linkUrl("/admin/access-requests/" + requestId)
                .build();
    }

    /**
     * 담당 배정 알림 (상담사에게)
     */
    public static Notification createAssignmentNotification(User receiver, Long assignmentId, String elderlyName) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.ASSIGNMENT_NEW)
                .title("새 담당 어르신이 배정되었습니다")
                .content(elderlyName + " 어르신이 담당 어르신으로 배정되었습니다.")
                .referenceType("assignments")
                .referenceId(assignmentId)
                .linkUrl("/counselor/elderly/" + assignmentId)
                .build();
    }

    /**
     * 공지사항 알림
     */
    public static Notification createNoticeNotification(User receiver, Long noticeId, String noticeTitle) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.NOTICE_NEW)
                .title("새 공지사항")
                .content("'" + truncate(noticeTitle, 50) + "' 공지사항이 등록되었습니다.")
                .referenceType("notices")
                .referenceId(noticeId)
                .linkUrl("/notices/" + noticeId)
                .build();
    }

    /**
     * 새 문의 알림 (관리자에게)
     */
    public static Notification createInquiryNewNotification(User receiver, Long inquiryId, String writerName,
            String inquiryTitle) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.INQUIRY_NEW)
                .title("새 문의가 등록되었습니다")
                .content(writerName + "님이 '" + truncate(inquiryTitle, 30) + "' 문의를 등록했습니다.")
                .referenceType("inquiries")
                .referenceId(inquiryId)
                .linkUrl("/admin/inquiries/" + inquiryId)
                .build();
    }

    /**
     * 새 민원 알림 (관리자에게)
     */
    public static Notification createComplaintNewNotification(User receiver, Long complaintId, String writerName,
            String complaintTitle) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.COMPLAINT_NEW)
                .title("새 민원이 등록되었습니다")
                .content(writerName + "님이 '" + truncate(complaintTitle, 30) + "' 민원을 등록했습니다.")
                .referenceType("complaints")
                .referenceId(complaintId)
                .linkUrl("/admin/complaints/" + complaintId)
                .build();
    }

    /**
     * 긴급 알림 생성 (수신자용)
     */
    public static Notification createEmergencyNewNotification(User receiver, Long alertId, String elderlyName,
            String severityDescription, String linkUrl) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.EMERGENCY_NEW)
                .title("긴급 알림: " + elderlyName)
                .content(elderlyName + " 어르신에게 " + severityDescription + " 상황이 감지되었습니다.")
                .referenceType("emergency_alerts")
                .referenceId(alertId)
                .linkUrl(linkUrl)
                .build();
    }

    /**
     * 시스템 알림
     */
    public static Notification createSystemNotification(User receiver, String title, String content) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.SYSTEM)
                .title(title)
                .content(content)
                .build();
    }

    /**
     * 상담사 코멘트 알림 생성
     */
    public static Notification createCounselorCommentNotification(User receiver, Long callId, String elderlyName) {
        return Notification.builder()
                .receiver(receiver)
                .notificationType(NotificationType.COUNSELOR_COMMENT)
                .title("새로운 상담사 코멘트")
                .content(elderlyName + " 어르신의 통화 기록에 새로운 코멘트가 등록되었습니다.")
                .referenceType("call_records")
                .referenceId(callId)
                .linkUrl("/guardian/calls/" + callId)
                .build();
    }

    // ========== 유틸 메서드 ==========

    private static String truncate(String text, int maxLength) {
        if (text == null)
            return "";
        if (text.length() <= maxLength)
            return text;
        return text.substring(0, maxLength) + "...";
    }
}