package com.aicc.silverlink.domain.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PendingConfirmationItem {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("raw_ocr_text")
    private String rawOcrText;

    @JsonProperty("decision_status")
    private String decisionStatus;

    @JsonProperty("match_confidence")
    private Double matchConfidence;

    @JsonProperty("best_drug_name")
    private String bestDrugName;

    @JsonProperty("best_drug_item_seq")
    private String bestDrugItemSeq;

    private List<Map<String, Object>> candidates;

    @JsonProperty("created_at")
    private String createdAt;
}
