package com.aicc.silverlink.domain.emergency.dto;

import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.AlertStatus;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.AlertType;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.Severity;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlertRecipient;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 긴급 알림 관련 DTO 클래스들
 */
public class EmergencyAlertDto {

    // ========== Request DTOs ==========

    /**
     * 긴급 알림 생성 요청 (CallBot → Backend)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CreateRequest {

        @NotNull(message = "어르신 ID는 필수입니다.")
        private Long elderlyUserId;

        private Long callId; // 미응답의 경우 null

        @NotNull(message = "위험도는 필수입니다.")
        private Severity severity;

        @NotNull(message = "알림 유형은 필수입니다.")
        private AlertType alertType;

        @NotBlank(message = "제목은 필수입니다.")
        @Size(max = 200, message = "제목은 200자 이내로 입력해주세요.")
        private String title;

        @NotBlank(message = "설명은 필수입니다.")
        private String description;

        private List<String> dangerKeywords; // 감지된 위험 키워드

        private String relatedSttContent; // 관련 STT 원문
    }

    /**
     * 긴급 알림 처리 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ProcessRequest {

        @NotNull(message = "처리 상태는 필수입니다.")
        private AlertStatus status; // RESOLVED 또는 ESCALATED

        @Size(max = 2000, message = "처리 메모는 2000자 이내로 입력해주세요.")
        private String resolutionNote;
    }

    /**
     * 긴급 알림 목록 조회 필터
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ListFilter {
        private AlertStatus status;
        private Severity severity;
        private AlertType alertType;
        private Long elderlyId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
    }

    // ========== Response DTOs ==========

    /**
     * 긴급 알림 목록 응답 (요약)
     */
    @Getter
    @Builder
    public static class SummaryResponse {
        private Long alertId;
        private Severity severity;
        private String severityText;
        private AlertType alertType;
        private String alertTypeText;
        private String title;
        private String description;
        private AlertStatus status;
        private String statusText;

        // 어르신 정보
        private Long elderlyId;
        private String elderlyName;
        private Integer elderlyAge;

        // 보호자 정보
        private String guardianName;
        private String guardianPhone;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        private String timeAgo; // "10분 전", "1시간 전" 등

        public static SummaryResponse from(EmergencyAlert alert) {
            var elderly = alert.getElderly();
            var user = elderly.getUser();

            return SummaryResponse.builder()
                    .alertId(alert.getId())
                    .severity(alert.getSeverity())
                    .severityText(alert.getSeverity().getDescription())
                    .alertType(alert.getAlertType())
                    .alertTypeText(alert.getAlertType().getDescription())
                    .title(alert.getTitle())
                    .description(alert.getDescription())
                    .status(alert.getStatus())
                    .statusText(alert.getStatus().getDescription())
                    .elderlyId(elderly.getId())
                    .elderlyName(user.getName())
                    .elderlyAge(elderly.age())
                    .createdAt(alert.getCreatedAt())
                    .timeAgo(calculateTimeAgo(alert.getCreatedAt()))
                    .build();
        }

        public static SummaryResponse fromWithGuardian(
                EmergencyAlert alert,
                String guardianName,
                String guardianPhone) {

            SummaryResponse response = from(alert);
            return SummaryResponse.builder()
                    .alertId(response.alertId)
                    .severity(response.severity)
                    .severityText(response.severityText)
                    .alertType(response.alertType)
                    .alertTypeText(response.alertTypeText)
                    .title(response.title)
                    .description(response.description)
                    .status(response.status)
                    .statusText(response.statusText)
                    .elderlyId(response.elderlyId)
                    .elderlyName(response.elderlyName)
                    .elderlyAge(response.elderlyAge)
                    .guardianName(guardianName)
                    .guardianPhone(guardianPhone)
                    .createdAt(response.createdAt)
                    .timeAgo(response.timeAgo)
                    .build();
        }
    }

    /**
     * 긴급 알림 상세 응답
     */
    @Getter
    @Builder
    public static class DetailResponse {
        private Long alertId;
        private Severity severity;
        private String severityText;
        private AlertType alertType;
        private String alertTypeText;
        private String title;
        private String description;
        private List<String> dangerKeywords;
        private String relatedSttContent;
        private AlertStatus status;
        private String statusText;

        // 어르신 정보
        private ElderlyInfo elderly;

        // 보호자 정보
        private GuardianInfo guardian;

        // 담당 상담사 정보
        private CounselorInfo counselor;

        // 관련 통화 정보
        private CallInfo call;

        // 처리 정보
        private ProcessInfo process;

        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;

        @Getter
        @Builder
        public static class ElderlyInfo {
            private Long id;
            private String name;
            private Integer age;
            private String gender;
            private String phone;
            private String address;
        }

        @Getter
        @Builder
        public static class GuardianInfo {
            private Long id;
            private String name;
            private String phone;
            private String relation;
        }

        @Getter
        @Builder
        public static class CounselorInfo {
            private Long id;
            private String name;
            private String phone;
            private String department;
        }

        @Getter
        @Builder
        public static class CallInfo {
            private Long callId;
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime callAt;
            private String duration;
            private String state;
            private String emotionLevel;
            private String recordingUrl;
        }

        @Getter
        @Builder
        public static class ProcessInfo {
            private String processedByName;
            @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
            private LocalDateTime processedAt;
            private String resolutionNote;
        }
    }

    /**
     * 긴급 알림 통계 응답
     */
    @Getter
    @Builder
    public static class StatsResponse {
        private long totalCount;
        private long criticalCount;
        private long warningCount;
        private long pendingCount;
        private long inProgressCount;
        private long resolvedCount;
    }

    /**
     * 실시간 알림용 응답 (WebSocket/SSE)
     */
    @Getter
    @Builder
    public static class RealtimeResponse {
        private Long alertId;
        private Severity severity;
        private AlertType alertType;
        private String title;
        private String elderlyName;
        private Integer elderlyAge;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime createdAt;
        private String timeAgo;

        public static RealtimeResponse from(EmergencyAlert alert) {
            String elderlyName = "알 수 없음";
            Integer elderlyAge = 0;

            if (alert.getElderly() != null) {
                if (alert.getElderly().getUser() != null) {
                    elderlyName = alert.getElderly().getUser().getName();
                }
                try {
                    elderlyAge = alert.getElderly().age();
                } catch (Exception e) {
                    // age logic failure (e.g. birthDate null)
                }
            }

            return RealtimeResponse.builder()
                    .alertId(alert.getId())
                    .severity(alert.getSeverity())
                    .alertType(alert.getAlertType())
                    .title(alert.getTitle())
                    .elderlyName(elderlyName)
                    .elderlyAge(elderlyAge)
                    .createdAt(alert.getCreatedAt())
                    .timeAgo(calculateTimeAgo(alert.getCreatedAt()))
                    .build();
        }
    }

    /**
     * 수신자별 알림 응답
     */
    @Getter
    @Builder
    public static class RecipientAlertResponse {
        private Long alertId;
        private SummaryResponse alert;
        private boolean isRead;
        @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
        private LocalDateTime readAt;

        public static RecipientAlertResponse from(EmergencyAlertRecipient recipient) {
            return RecipientAlertResponse.builder()
                    .alertId(recipient.getEmergencyAlert().getId())
                    .alert(SummaryResponse.from(recipient.getEmergencyAlert()))
                    .isRead(recipient.isRead())
                    .readAt(recipient.getReadAt())
                    .build();
        }
    }

    // ========== 유틸 메서드 ==========

    /**
     * 시간 차이 계산 ("n분 전", "n시간 전" 등)
     */
    private static String calculateTimeAgo(LocalDateTime dateTime) {
        if (dateTime == null)
            return "";

        LocalDateTime now = LocalDateTime.now();
        long minutes = java.time.Duration.between(dateTime, now).toMinutes();

        if (minutes < 1)
            return "방금 전";
        if (minutes < 60)
            return minutes + "분 전";

        long hours = minutes / 60;
        if (hours < 24)
            return hours + "시간 전";

        long days = hours / 24;
        if (days < 7)
            return days + "일 전";

        return dateTime.toLocalDate().toString();
    }
}
