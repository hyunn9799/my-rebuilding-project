package com.aicc.silverlink.domain.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QualityReportTrendResponse {

    @JsonProperty("matched_delta")
    private Integer matchedDelta;

    @JsonProperty("pending_review_delta")
    private Integer pendingReviewDelta;

    @JsonProperty("alias_candidate_delta")
    private Integer aliasCandidateDelta;

    @JsonProperty("manual_review_delta")
    private Integer manualReviewDelta;

    @JsonProperty("normalization_candidate_delta")
    private Integer normalizationCandidateDelta;

    @JsonProperty("current_run_id")
    private Long currentRunId;

    @JsonProperty("previous_run_id")
    private Long previousRunId;
}
