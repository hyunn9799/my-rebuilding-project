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
public class QualityReportRunResponse {

    private Boolean success;

    @JsonProperty("generated_at")
    private String generatedAt;

    @JsonProperty("decision_counts")
    private List<Map<String, Object>> decisionCounts;

    @JsonProperty("suggestion_counts")
    private List<Map<String, Object>> suggestionCounts;

    @JsonProperty("match_method_counts")
    private List<Object> matchMethodCounts;

    @JsonProperty("recommended_action_counts")
    private Map<String, Integer> recommendedActionCounts;

    @JsonProperty("alias_candidate_count")
    private Integer aliasCandidateCount;

    @JsonProperty("manual_review_count")
    private Integer manualReviewCount;

    @JsonProperty("normalization_candidate_count")
    private Integer normalizationCandidateCount;

    @JsonProperty("report_markdown")
    private String reportMarkdown;

    @JsonProperty("alias_candidates")
    private List<Map<String, Object>> aliasCandidates;

    private String message;
}
