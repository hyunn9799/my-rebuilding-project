package com.aicc.silverlink.domain.medication.dto;

import com.aicc.silverlink.domain.medication.entity.MedicationSchedule;
import com.aicc.silverlink.domain.medication.entity.MedicationScheduleTime;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

@Getter
@Builder
public class MedicationResponse {
    private Long id;
    private String name;
    private String dosage;
    private List<String> times;
    private boolean reminder;
    private String startDate;
    private String endDate;
    private String instructions;

    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    public static MedicationResponse from(MedicationSchedule schedule, List<MedicationScheduleTime> scheduleTimes) {
        return MedicationResponse.builder()
                .id(schedule.getId())
                .name(schedule.getMedicationName())
                .dosage(schedule.getDosageText())
                .times(mapTimesToStrings(scheduleTimes))
                .reminder(schedule.isActive())
                .startDate(schedule.getStartDate() != null ? schedule.getStartDate().format(DATE_FORMATTER) : null)
                .endDate(schedule.getEndDate() != null ? schedule.getEndDate().format(DATE_FORMATTER) : null)
                .instructions(mapIntakeTimingToString(schedule.getIntakeTiming()))
                .build();
    }

    private static List<String> mapTimesToStrings(List<MedicationScheduleTime> scheduleTimes) {
        if (scheduleTimes == null || scheduleTimes.isEmpty()) {
            return List.of();
        }

        return scheduleTimes.stream()
                .map(st -> mapTimeToLabel(st.getIntakeTime()))
                .collect(Collectors.toList());
    }

    private static String mapTimeToLabel(LocalTime time) {
        if (time == null)
            return "morning";
        int hour = time.getHour();
        if (hour >= 6 && hour < 11)
            return "morning";
        if (hour >= 11 && hour < 14)
            return "noon";
        if (hour >= 14 && hour < 20)
            return "evening";
        return "night";
    }

    private static String mapIntakeTimingToString(MedicationSchedule.IntakeTiming timing) {
        if (timing == null)
            return null;
        return switch (timing) {
            case BEFORE_MEAL -> "식전 복용";
            case AFTER_MEAL -> "식후 복용";
            case ANYTIME -> null;
        };
    }
}
