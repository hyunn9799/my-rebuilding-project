package com.aicc.silverlink.domain.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class QualityReportUpsertResponse {

    private Boolean success;

    @JsonProperty("upserted_count")
    private Integer upsertedCount;

    @JsonProperty("candidate_count")
    private Integer candidateCount;

    @JsonProperty("skipped_count")
    private Integer skippedCount;

    private String message;

    @JsonProperty("generated_at")
    private String generatedAt;
}
