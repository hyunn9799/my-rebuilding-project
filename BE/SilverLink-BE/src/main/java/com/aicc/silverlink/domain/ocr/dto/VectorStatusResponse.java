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
public class VectorStatusResponse {

    @JsonProperty("collection_name")
    private String collectionName;

    @JsonProperty("persist_directory")
    private String persistDirectory;

    private Integer count;

    @JsonProperty("expected_count")
    private Integer expectedCount;

    @JsonProperty("embedding_model")
    private String embeddingModel;

    private String status;
    private String message;

    @JsonProperty("is_degraded")
    private Boolean isDegraded;

    @JsonProperty("checked_at")
    private String checkedAt;
}
