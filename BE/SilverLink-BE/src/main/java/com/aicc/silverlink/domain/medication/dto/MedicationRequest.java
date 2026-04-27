package com.aicc.silverlink.domain.medication.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class MedicationRequest {

    @NotBlank(message = "약 이름은 필수입니다")
    private String medicationName;

    private String dosageText; // 예: "1정", "5mg"

    @NotNull(message = "복용 시간은 필수입니다")
    private List<String> times; // "morning", "noon", "evening", "night"

    private String instructions; // 복용 방법 (예: "식후 30분")

    private LocalDate startDate;

    private LocalDate endDate;

    private boolean reminder = true;
}
