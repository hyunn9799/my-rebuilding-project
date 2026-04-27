package com.aicc.silverlink.domain.elderly.dto.request;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public record ElderlyCreateRequest(
                @NotNull Long userId,
                @NotNull Long admCode,
                @NotNull LocalDate birthDate,
                @NotNull Elderly.Gender gender,
                @Size(max = 200) String addressLine1,
                @Size(max = 200) String addressLine2,
                @Size(max = 10) String zipcode,

                // 통화 스케줄 설정
                @Pattern(regexp = "^([01]\\d|2[0-3]):[0-5]\\d$", message = "시간 형식은 HH:mm이어야 합니다") String preferredCallTime, // 선호 통화 시간(HH:mm 형식, 예:"09:00")
                List<String> preferredCallDays, // 선호 통화 요일 (예: ["MON", "WED", "FRI"])

                Boolean callScheduleEnabled // 통화 스케줄 활성화 여부
) {
        /**
         * 선호 통화 요일을 콤마 구분 문자열로 변환
         * 예: ["MON", "WED", "FRI"] → "MON,WED,FRI"
         */
        public String getPreferredCallDaysAsString() {
                if (preferredCallDays == null || preferredCallDays.isEmpty()) {
                        return null;
                }
                return String.join(",", preferredCallDays);
        }
}