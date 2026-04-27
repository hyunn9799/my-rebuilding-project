package com.aicc.silverlink.domain.ocr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import jakarta.validation.constraints.NotBlank;

/**
 * OCR 약 검증 요청 DTO
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OcrValidationRequest {

    @NotBlank(message = "OCR 텍스트는 필수입니다")
    private String ocrText;

    private Long elderlyUserId;
}
