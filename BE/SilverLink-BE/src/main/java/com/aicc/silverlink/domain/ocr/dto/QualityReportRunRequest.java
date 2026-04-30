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
public class QualityReportRunRequest {

    @Builder.Default
    private Integer limit = 20;

    @JsonProperty("include_candidates")
    @Builder.Default
    private Boolean includeCandidates = true;

    @JsonProperty("persist_files")
    @Builder.Default
    private Boolean persistFiles = false;
}
