package com.aicc.silverlink.domain.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class QualityReportRunHistoryItem {

    private Long id;

    @JsonProperty("action_type")
    private String actionType;

    @JsonProperty("actor_user_id")
    private Long actorUserId;

    private Integer limit;

    private Boolean success;

    @JsonProperty("matched_count")
    private Integer matchedCount;

    @JsonProperty("pending_review_count")
    private Integer pendingReviewCount;

    @JsonProperty("alias_candidate_count")
    private Integer aliasCandidateCount;

    @JsonProperty("manual_review_count")
    private Integer manualReviewCount;

    @JsonProperty("normalization_candidate_count")
    private Integer normalizationCandidateCount;

    @JsonProperty("candidate_count")
    private Integer candidateCount;

    @JsonProperty("upserted_count")
    private Integer upsertedCount;

    @JsonProperty("skipped_count")
    private Integer skippedCount;

    private String message;

    @JsonProperty("created_at")
    private String createdAt;
}
