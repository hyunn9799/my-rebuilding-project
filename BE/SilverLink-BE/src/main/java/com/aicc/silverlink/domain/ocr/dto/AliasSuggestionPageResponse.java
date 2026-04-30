package com.aicc.silverlink.domain.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * alias 제안 페이징 조회 응답 (Python AI 서버 → BE).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliasSuggestionPageResponse {

    private List<AliasSuggestionItem> items;
    private int total;
    private int page;
    private int size;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AliasSuggestionItem {
        private Long id;

        @JsonProperty("item_seq")
        private String itemSeq;

        @JsonProperty("item_name")
        private String itemName;

        @JsonProperty("alias_name")
        private String aliasName;

        @JsonProperty("alias_normalized")
        private String aliasNormalized;

        @JsonProperty("suggestion_type")
        private String suggestionType;

        private String source;

        @JsonProperty("source_request_id")
        private String sourceRequestId;

        @JsonProperty("review_status")
        private String reviewStatus;

        private Integer frequency;

        @JsonProperty("priority_score")
        private Integer priorityScore;

        @JsonProperty("priority_reason")
        private String priorityReason;

        @JsonProperty("is_active")
        private Integer isActive;

        @JsonProperty("reviewed_by")
        private String reviewedBy;

        @JsonProperty("reviewed_at")
        private String reviewedAt;

        @JsonProperty("created_at")
        private String createdAt;

        @JsonProperty("updated_at")
        private String updatedAt;
    }
}
