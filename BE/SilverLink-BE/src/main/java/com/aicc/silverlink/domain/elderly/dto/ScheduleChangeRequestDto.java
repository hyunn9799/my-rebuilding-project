package com.aicc.silverlink.domain.elderly.dto;

import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest;
import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest.RequestStatus;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.*;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

/**
 * 스케줄 변경 요청 관련 DTO
 */
public class ScheduleChangeRequestDto {

    // ===== 요청 DTO =====

    /**
     * 변경 요청 생성
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CreateRequest {
        @NotBlank(message = "선호 통화 시간은 필수입니다")
        private String preferredCallTime; // "09:00" 형식

        @NotEmpty(message = "선호 통화 요일은 필수입니다")
        private List<String> preferredCallDays; // ["MON", "WED", "FRI"]

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

    /**
     * 거절 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RejectRequest {
        private String reason;
    }

    // ===== 응답 DTO =====

    /**
     * 변경 요청 응답
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class Response {
        private Long id;
        private Long elderlyId;
        private String elderlyName;
        private String requestedCallTime;
        private List<String> requestedCallDays;
        private RequestStatus status;
        private LocalDateTime createdAt;
        private LocalDateTime processedAt;
        private String processedByName;
        private String rejectReason;

        public static Response from(ScheduleChangeRequest request) {
            return Response.builder()
                    .id(request.getId())
                    .elderlyId(request.getElderly().getId())
                    .elderlyName(request.getElderly().getUser().getName())
                    .requestedCallTime(request.getRequestedCallTime())
                    .requestedCallDays(parseDays(request.getRequestedCallDays()))
                    .status(request.getStatus())
                    .createdAt(request.getCreatedAt())
                    .processedAt(request.getProcessedAt())
                    .processedByName(request.getProcessedBy() != null ? request.getProcessedBy().getName() : null)
                    .rejectReason(request.getRejectReason())
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
