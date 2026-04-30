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
public class QualityReportUpsertRequest {

    @Builder.Default
    private Integer limit = 20;

    @JsonProperty("confirm_write")
    @Builder.Default
    private Boolean confirmWrite = false;
}
