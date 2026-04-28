package com.aicc.silverlink.domain.ocr.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConfirmMedicationRequest {

    @NotBlank
    @JsonProperty("request_id")
    @JsonAlias("requestId")
    private String requestId;

    @NotBlank
    @JsonProperty("selected_item_seq")
    @JsonAlias("selectedItemSeq")
    private String selectedItemSeq;

    @NotNull
    private Boolean confirmed;
}
