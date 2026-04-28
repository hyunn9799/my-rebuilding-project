package com.aicc.silverlink.domain.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * alias 제안 승인/거부 응답 (Python AI 서버 → BE).
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AliasSuggestionActionResponse {

    private boolean success;
    private String message;

    @JsonProperty("target_table")
    private String targetTable;

    @JsonProperty("reload_success")
    private Boolean reloadSuccess;

    @JsonProperty("reload_warning")
    private String reloadWarning;
}
