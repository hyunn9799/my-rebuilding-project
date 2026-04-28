package com.aicc.silverlink.domain.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * OCR validation response returned by the Python AI server.
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrValidationResponse {

    private Boolean success;
    private List<MedicationInfo> medications;

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("raw_ocr_text")
    private String rawOcrText;

    @JsonProperty("llm_analysis")
    private String llmAnalysis;

    private List<String> warnings;

    @JsonProperty("error_message")
    private String errorMessage;

    @JsonProperty("pipeline_stages")
    private List<PipelineStageInfo> pipelineStages;

    @JsonProperty("total_duration_ms")
    private Double totalDurationMs;

    @JsonProperty("decision_status")
    private String decisionStatus;

    @JsonProperty("match_confidence")
    private Double matchConfidence;

    @JsonProperty("requires_user_confirmation")
    private Boolean requiresUserConfirmation;

    @JsonProperty("decision_reasons")
    private List<String> decisionReasons;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MedicationInfo {
        @JsonProperty("medication_name")
        private String medicationName;

        private String dosage;
        private List<String> times;
        private String instructions;
        private Double confidence;
        private String category;

        @JsonProperty("item_seq")
        private String itemSeq;

        @JsonProperty("entp_name")
        private String entpName;

        @JsonProperty("match_score")
        private Double matchScore;

        @JsonProperty("match_method")
        private String matchMethod;

        private String purpose;
        private String caution;

        @JsonProperty("simple_name")
        private String simpleName;

        private Map<String, Object> evidence;

        @JsonProperty("validation_messages")
        private List<String> validationMessages;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PipelineStageInfo {
        private String stage;

        @JsonProperty("duration_ms")
        private Double durationMs;

        @JsonProperty("result_summary")
        private String resultSummary;
    }
}
