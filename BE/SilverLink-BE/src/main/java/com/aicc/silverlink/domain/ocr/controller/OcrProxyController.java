package com.aicc.silverlink.domain.ocr.controller;

import com.aicc.silverlink.domain.ocr.dto.OcrValidationRequest;
import com.aicc.silverlink.domain.ocr.dto.OcrValidationResponse;
import com.aicc.silverlink.domain.ocr.dto.PythonOcrRequest;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import jakarta.validation.Valid;

/**
 * Python OCR 서비스와 프론트엔드 사이의 프록시 컨트롤러
 * Spring Boot가 Python AI 서버로 요청을 전달
 */
@Slf4j
@RestController
@RequestMapping("/api/ocr")
@RequiredArgsConstructor
@Tag(name = "OCR", description = "OCR 약 정보 검증 API")
public class OcrProxyController {

    private final RestTemplate restTemplate;

    @Value("${chatbot.python.url:http://localhost:5000}")
    private String pythonAiUrl;

    @Value("${chatbot.secret.header:X-SilverLink-Secret}")
    private String secretHeader;

    @Value("${chatbot.secret.key:X-SilverLink-Key!}")
    private String secretKey;

    @PostMapping("/validate-medication")
    @Operation(summary = "약 정보 OCR 검증", description = "Luxia OCR 결과를 Python AI 서버의 LLM으로 검증하고 약 정보를 추출합니다.")
    public ResponseEntity<OcrValidationResponse> validateMedication(
            @Valid @RequestBody OcrValidationRequest request) {

        log.info("OCR validation request received: elderlyUserId={}, textLength={}",
                request.getElderlyUserId(),
                request.getOcrText() != null ? request.getOcrText().length() : 0);

        try {
            // Python AI 서버로 보낼 요청 생성 (snake_case 변환)
            PythonOcrRequest pythonRequest = PythonOcrRequest.builder()
                    .ocrText(request.getOcrText())
                    .elderlyUserId(request.getElderlyUserId())
                    .build();

            // HTTP 헤더 설정 (secret 헤더 포함)
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.set(secretHeader, secretKey);

            HttpEntity<PythonOcrRequest> entity = new HttpEntity<>(pythonRequest, headers);

            // Python AI 서버 호출
            String pythonUrl = pythonAiUrl + "/api/ocr/validate-medication";
            log.info("Calling Python OCR service: url={}", pythonUrl);

            ResponseEntity<OcrValidationResponse> responseEntity = restTemplate.exchange(
                    pythonUrl,
                    HttpMethod.POST,
                    entity,
                    OcrValidationResponse.class);

            OcrValidationResponse response = responseEntity.getBody();

            log.info("OCR validation response received: success={}, medicationsCount={}",
                    response != null ? response.getSuccess() : null,
                    response != null && response.getMedications() != null ? response.getMedications().size() : 0);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            log.error("Error calling Python OCR service", e);

            // 에러 응답 생성
            OcrValidationResponse errorResponse = OcrValidationResponse.builder()
                    .success(false)
                    .errorMessage("OCR 검증 서비스 호출 중 오류가 발생했습니다: " + e.getMessage())
                    .build();

            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(errorResponse);
        }
    }
}
