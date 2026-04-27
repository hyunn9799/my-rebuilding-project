package com.aicc.silverlink.domain.ocr.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * Python AI 서버로 보내는 OCR 검증 요청 DTO
 * Python가 snake_case를 사용하므로 JsonProperty 사용
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PythonOcrRequest {

    @JsonProperty("ocr_text")
    private String ocrText;

    @JsonProperty("elderly_user_id")
    private Long elderlyUserId;
}
