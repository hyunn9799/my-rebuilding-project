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
public class OcrResultOwnerResponse {

    @JsonProperty("request_id")
    private String requestId;

    @JsonProperty("elderly_user_id")
    private Long elderlyUserId;

    @JsonProperty("decision_status")
    private String decisionStatus;

    @JsonProperty("user_confirmed")
    private Boolean userConfirmed;
}
