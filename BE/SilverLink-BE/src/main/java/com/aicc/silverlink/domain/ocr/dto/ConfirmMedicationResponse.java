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
public class ConfirmMedicationResponse {

    private Boolean success;
    private String message;

    @JsonProperty("alias_suggestion_created")
    private Boolean aliasSuggestionCreated;
}
