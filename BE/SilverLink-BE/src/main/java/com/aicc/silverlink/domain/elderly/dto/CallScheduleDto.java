package com.aicc.silverlink.domain.elderly.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.*;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 통화 스케줄 관련 DTO
 */
public class CallScheduleDto {

    // ===== 요청 DTO =====

    /**
     * 통화 스케줄 설정/수정 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UpdateRequest {
        private String preferredCallTime; // "09:00" 형식
        private List<String> preferredCallDays; // ["MON", "WED", "FRI"]
        private Boolean callScheduleEnabled;

        /**
         * 요일 리스트를 콤마 구분 문자열로 변환
         */
        public String getDaysAsString() {
            if (preferredCallDays == null || preferredCallDays.isEmpty()) {
                return null;
            }
            return String.join(",", preferredCallDays);
        }
    }

    // ===== 응답 DTO =====

    /**
     * 스케줄 조회 응답
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long elderlyId;
        private String elderlyName;
        private String preferredCallTime;
        private List<String> preferredCallDays;
        private Boolean callScheduleEnabled;

        public static Response from(com.aicc.silverlink.domain.elderly.entity.Elderly elderly) {
            return Response.builder()
                    .elderlyId(elderly.getId())
                    .elderlyName(elderly.getUser().getName())
                    .preferredCallTime(elderly.getPreferredCallTime())
                    .preferredCallDays(parseDays(elderly.getPreferredCallDays()))
                    .callScheduleEnabled(elderly.getCallScheduleEnabled())
                    .build();
        }

        private static List<String> parseDays(String days) {
            if (days == null || days.isBlank()) {
                return List.of();
            }
            return Arrays.asList(days.split(","));
        }
    }

    /**
     * Python CallBot 호출용 요청 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class StartCallRequest {
        @JsonProperty("elderly_id")
        private Long elderlyId;

        @JsonProperty("elderly_name")
        private String elderlyName;

        @JsonProperty("phone_number")
        private String phone;
    }

    /**
     * 상담사/관리자 직접 수정 요청 (구두 요청 등)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DirectUpdateRequest {
        private String preferredCallTime; // "09:00" 형식
        private List<String> preferredCallDays; // ["MON", "WED", "FRI"]
        private Boolean callScheduleEnabled;
        private String changeReason; // 변경 사유 (필수)

        public String getDaysAsString() {
            if (preferredCallDays == null || preferredCallDays.isEmpty()) {
                return null;
            }
            return String.join(",", preferredCallDays);
        }
    }

    /**
     * 변경 이력 응답 DTO
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class HistoryResponse {
        private Long id;
        private Long elderlyId;
        private String elderlyName;
        private String changedByName;
        private String changedByRole;
        private String changeType;
        private String changeTypeDescription;
        private String previousTime;
        private List<String> previousDays;
        private Boolean previousEnabled;
        private String newTime;
        private List<String> newDays;
        private Boolean newEnabled;
        private String changeReason;
        private Long relatedRequestId;
        private java.time.LocalDateTime createdAt;

        public static HistoryResponse from(com.aicc.silverlink.domain.elderly.entity.CallScheduleHistory history) {
            return HistoryResponse.builder()
                    .id(history.getId())
                    .elderlyId(history.getElderly().getId())
                    .elderlyName(history.getElderly().getUser().getName())
                    .changedByName(history.getChangedBy().getName())
                    .changedByRole(history.getChangedBy().getRole().name())
                    .changeType(history.getChangeType().name())
                    .changeTypeDescription(history.getChangeType().getDescription())
                    .previousTime(history.getPreviousTime())
                    .previousDays(parseDays(history.getPreviousDays()))
                    .previousEnabled(history.getPreviousEnabled())
                    .newTime(history.getNewTime())
                    .newDays(parseDays(history.getNewDays()))
                    .newEnabled(history.getNewEnabled())
                    .changeReason(history.getChangeReason())
                    .relatedRequestId(history.getRelatedRequestId())
                    .createdAt(history.getCreatedAt())
                    .build();
        }

        private static List<String> parseDays(String days) {
            if (days == null || days.isBlank()) {
                return List.of();
            }
            return Arrays.asList(days.split(","));
        }
    }
}
