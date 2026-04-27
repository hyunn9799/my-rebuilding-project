package com.aicc.silverlink.domain.notification.dto;

import com.aicc.silverlink.domain.notification.entity.Notification;
import com.aicc.silverlink.domain.notification.entity.Notification.NotificationType;
import lombok.*;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * 일반 알림 DTO
 */
public class NotificationDto {

    // ========== 응답 DTO ==========

    /**
     * 알림 요약 응답 (목록용)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryResponse {
        private Long notificationId;
        private NotificationType notificationType;
        private String notificationTypeText;
        private String title;
        private String content;
        private String referenceType;
        private Long referenceId;
        private String linkUrl;
        private Boolean isRead;
        private LocalDateTime createdAt;
        private String timeAgo;

        public static SummaryResponse from(Notification notification) {
            return SummaryResponse.builder()
                    .notificationId(notification.getId())
                    .notificationType(notification.getNotificationType())
                    .notificationTypeText(notification.getNotificationType().getDescription())
                    .title(notification.getTitle())
                    .content(notification.getContent())
                    .referenceType(notification.getReferenceType())
                    .referenceId(notification.getReferenceId())
                    .linkUrl(notification.getLinkUrl())
                    .isRead(notification.getIsRead())
                    .createdAt(notification.getCreatedAt())
                    .timeAgo(calculateTimeAgo(notification.getCreatedAt()))
                    .build();
        }

        private static String calculateTimeAgo(LocalDateTime dateTime) {
            if (dateTime == null)
                return "";

            LocalDateTime now = LocalDateTime.now();
            long minutes = ChronoUnit.MINUTES.between(dateTime, now);

            if (minutes < 1)
                return "방금 전";
            if (minutes < 60)
                return minutes + "분 전";

            long hours = ChronoUnit.HOURS.between(dateTime, now);
            if (hours < 24)
                return hours + "시간 전";

            long days = ChronoUnit.DAYS.between(dateTime, now);
            if (days < 7)
                return days + "일 전";

            if (days < 30)
                return (days / 7) + "주 전";

            return dateTime.toLocalDate().toString();
        }
    }

    /**
     * 알림 상세 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DetailResponse {
        private Long notificationId;
        private NotificationType notificationType;
        private String notificationTypeText;
        private String title;
        private String content;
        private String referenceType;
        private Long referenceId;
        private String linkUrl;
        private Boolean isRead;
        private LocalDateTime readAt;
        private Boolean smsSent;
        private LocalDateTime smsSentAt;
        private LocalDateTime createdAt;
        private String timeAgo;

        public static DetailResponse from(Notification notification) {
            return DetailResponse.builder()
                    .notificationId(notification.getId())
                    .notificationType(notification.getNotificationType())
                    .notificationTypeText(notification.getNotificationType().getDescription())
                    .title(notification.getTitle())
                    .content(notification.getContent())
                    .referenceType(notification.getReferenceType())
                    .referenceId(notification.getReferenceId())
                    .linkUrl(notification.getLinkUrl())
                    .isRead(notification.getIsRead())
                    .readAt(notification.getReadAt())
                    .smsSent(notification.getSmsSent())
                    .smsSentAt(notification.getSmsSentAt())
                    .createdAt(notification.getCreatedAt())
                    .timeAgo(SummaryResponse.calculateTimeAgo(notification.getCreatedAt()))
                    .build();
        }
    }

    /**
     * 실시간 알림 응답 (SSE용)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RealtimeResponse {
        private Long notificationId;
        private String type; // "notification" (긴급 알림과 구분)
        private NotificationType notificationType;
        private String notificationTypeText;
        private String title;
        private String content;
        private String linkUrl;
        private LocalDateTime createdAt;
        private String timeAgo;

        public static RealtimeResponse from(Notification notification) {
            return RealtimeResponse.builder()
                    .notificationId(notification.getId())
                    .type("notification")
                    .notificationType(notification.getNotificationType())
                    .notificationTypeText(notification.getNotificationType().getDescription())
                    .title(notification.getTitle())
                    .content(notification.getContent())
                    .linkUrl(notification.getLinkUrl())
                    .createdAt(notification.getCreatedAt())
                    .timeAgo("방금 전")
                    .build();
        }
    }

    /**
     * 미확인 알림 수 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnreadCountResponse {
        private long totalUnread; // 전체 미확인 수
        private long emergencyUnread; // 긴급 알림 미확인 수
        private long notificationUnread; // 일반 알림 미확인 수

        public static UnreadCountResponse of(long emergencyUnread, long notificationUnread) {
            return UnreadCountResponse.builder()
                    .totalUnread(emergencyUnread + notificationUnread)
                    .emergencyUnread(emergencyUnread)
                    .notificationUnread(notificationUnread)
                    .build();
        }
    }

    /**
     * 알림 통계 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StatsResponse {
        private long totalCount;
        private long unreadCount;
        private long todayCount;
        private List<TypeCount> countByType;

        @Getter
        @Builder
        @NoArgsConstructor
        @AllArgsConstructor
        public static class TypeCount {
            private NotificationType type;
            private String typeText;
            private long count;
        }
    }

    // ========== 요청 DTO ==========

    /**
     * 시스템 알림 생성 요청 (관리자용)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SystemNotificationRequest {
        private List<Long> receiverUserIds; // 수신자 ID 목록 (null이면 전체)
        private String title;
        private String content;
        private String linkUrl;
    }

    /**
     * 알림 설정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SettingsRequest {
        private Boolean enableInquiryNotification;
        private Boolean enableComplaintNotification;
        private Boolean enableAccessNotification;
        private Boolean enableAssignmentNotification;
        private Boolean enableNoticeNotification;
        private Boolean enableSms;
    }
}
