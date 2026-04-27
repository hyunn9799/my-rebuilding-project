package com.aicc.silverlink.domain.ocr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * OCR 약 검증 응답 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrValidationResponse {

    private Boolean success;
    private List<MedicationInfo> medications;
    private String rawOcrText;
    private String llmAnalysis;
    private List<String> warnings;
    private String errorMessage;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationInfo {
        private String medicationName;
        private String dosage;
        private List<String> times;
        private String instructions;
        private Double confidence;
        private String category;
    }
}
