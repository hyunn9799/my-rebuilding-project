package com.aicc.silverlink.domain.elderly.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

import java.util.List;

public record ElderlyUpdateRequest(
                @NotBlank String name,
                @NotBlank String phone,
                String addressLine1,
                String addressLine2,
                String zipcode,

                Long admCode, // 행정구역 코드 (변경 시)

                // 통화 스케줄 설정
                @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식은 HH:mm이어야 합니다") String preferredCallTime, // 선호
                                                                                                                            // 통화
                                                                                                                            // 시간
                                                                                                                            // (HH:mm
                                                                                                                            // 형식)

                List<String> preferredCallDays, // 선호 통화 요일 (예: ["MON", "WED", "FRI"])

                Boolean callScheduleEnabled // 통화 스케줄 활성화 여부
) {
        /**
         * 선호 통화 요일을 콤마 구분 문자열로 변환
         */
        public String getPreferredCallDaysAsString() {
                if (preferredCallDays == null || preferredCallDays.isEmpty()) {
                        return null;
                }
                return String.join(",", preferredCallDays);
        }
}