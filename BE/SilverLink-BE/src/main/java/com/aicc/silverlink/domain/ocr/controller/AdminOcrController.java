package com.aicc.silverlink.domain.ocr.controller;

import com.aicc.silverlink.domain.audit.service.AuditLogService;
import com.aicc.silverlink.domain.ocr.dto.VectorStatusResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunHistoryResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunRequest;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportUpsertRequest;
import com.aicc.silverlink.domain.ocr.dto.QualityReportUpsertResponse;
import com.aicc.silverlink.domain.ocr.entity.OcrQualityReportRun;
import com.aicc.silverlink.domain.ocr.service.OcrQualityReportRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.client.RestTemplate;

import java.util.LinkedHashMap;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/admin/ocr")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
@Tag(name = "Admin - OCR", description = "관리자 OCR 운영 API")
public class AdminOcrController {

    private final RestTemplate restTemplate;
    private final OcrQualityReportRunService qualityReportRunService;
    private final AuditLogService auditLogService;
    private final ObjectMapper objectMapper;

    @Value("${chatbot.python.url:http://localhost:8000}")
    private String pythonAiUrl;

    @Value("${chatbot.secret.header:X-SilverLink-Secret}")
    private String secretHeader;

    @Value("${chatbot.secret.key:X-SilverLink-Key!}")
    private String secretKey;

    @GetMapping("/vector-status")
    @Operation(
            summary = "OCR vector DB 상태 조회",
            description = "Python AI 내부 vector status endpoint를 관리자 전용 BE endpoint로 프록시합니다.")
    public ResponseEntity<VectorStatusResponse> getVectorStatus() {
        String pythonUrl = pythonAiUrl + "/api/ocr/internal/vector/status";
        HttpEntity<Void> entity = new HttpEntity<>(createJsonHeaders());

        try {
            ResponseEntity<VectorStatusResponse> response = restTemplate.exchange(
                    pythonUrl,
                    HttpMethod.GET,
                    entity,
                    VectorStatusResponse.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (RestClientResponseException e) {
            log.error("Python OCR vector status service returned error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            return ResponseEntity.status(e.getStatusCode()).body(errorBody(e));
        } catch (Exception e) {
            log.error("Error calling Python OCR vector status service", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(VectorStatusResponse.builder()
                            .status("ERROR")
                            .message("OCR vector status 조회 중 오류가 발생했습니다: " + e.getMessage())
                            .isDegraded(true)
                            .build());
        }
    }

    @PostMapping("/quality-report/run")
    @Operation(
            summary = "OCR 품질 리포트 실행",
            description = "Python AI OCR 품질 리포트 endpoint를 관리자 전용 BE endpoint로 프록시합니다.")
    public ResponseEntity<QualityReportRunResponse> runQualityReport(
            @RequestBody(required = false) QualityReportRunRequest request,
            @AuthenticationPrincipal Long adminId) {
        String pythonUrl = pythonAiUrl + "/api/ocr/admin/quality-report/run";
        QualityReportRunRequest body = request != null ? request : QualityReportRunRequest.builder().build();
        HttpEntity<QualityReportRunRequest> entity = new HttpEntity<>(body, createJsonHeaders());

        try {
            ResponseEntity<QualityReportRunResponse> response = restTemplate.exchange(
                    pythonUrl,
                    HttpMethod.POST,
                    entity,
                    QualityReportRunResponse.class);

            QualityReportRunResponse responseBody = response.getBody();
            recordReportRun(adminId, body, responseBody, true, responseBody != null ? responseBody.getMessage() : null);
            return ResponseEntity.status(response.getStatusCode()).body(responseBody);
        } catch (RestClientResponseException e) {
            log.error("Python OCR quality report service returned error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            QualityReportRunResponse errorBody = qualityReportErrorBody(e);
            recordReportRun(adminId, body, errorBody, false, errorBody.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(errorBody);
        } catch (Exception e) {
            log.error("Error calling Python OCR quality report service", e);
            QualityReportRunResponse errorBody = QualityReportRunResponse.builder()
                    .success(false)
                    .message("OCR 품질 리포트 실행 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
            recordReportRun(adminId, body, errorBody, false, errorBody.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody);
        }
    }

    @PostMapping("/quality-report/upsert-alias-candidates")
    @Operation(
            summary = "OCR 품질 리포트 alias 후보 등록",
            description = "Python AI OCR 품질 리포트의 alias 후보 upsert endpoint를 관리자 전용 BE endpoint로 프록시합니다.")
    public ResponseEntity<QualityReportUpsertResponse> upsertQualityReportAliasCandidates(
            @RequestBody(required = false) QualityReportUpsertRequest request,
            @AuthenticationPrincipal Long adminId) {
        String pythonUrl = pythonAiUrl + "/api/ocr/admin/quality-report/upsert-alias-candidates";
        QualityReportUpsertRequest body = request != null ? request : QualityReportUpsertRequest.builder().build();
        HttpEntity<QualityReportUpsertRequest> entity = new HttpEntity<>(body, createJsonHeaders());

        try {
            ResponseEntity<QualityReportUpsertResponse> response = restTemplate.exchange(
                    pythonUrl,
                    HttpMethod.POST,
                    entity,
                    QualityReportUpsertResponse.class);

            QualityReportUpsertResponse responseBody = response.getBody();
            recordUpsertRun(adminId, body, responseBody, true, responseBody != null ? responseBody.getMessage() : null);
            return ResponseEntity.status(response.getStatusCode()).body(responseBody);
        } catch (RestClientResponseException e) {
            log.error("Python OCR quality report upsert service returned error: status={}, body={}",
                    e.getStatusCode(), e.getResponseBodyAsString(), e);
            QualityReportUpsertResponse errorBody = qualityReportUpsertErrorBody(e);
            recordUpsertRun(adminId, body, errorBody, false, errorBody.getMessage());
            return ResponseEntity.status(e.getStatusCode()).body(errorBody);
        } catch (Exception e) {
            log.error("Error calling Python OCR quality report upsert service", e);
            QualityReportUpsertResponse errorBody = QualityReportUpsertResponse.builder()
                    .success(false)
                    .message("OCR 품질 리포트 alias 후보 등록 중 오류가 발생했습니다: " + e.getMessage())
                    .build();
            recordUpsertRun(adminId, body, errorBody, false, errorBody.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(errorBody);
        }
    }

    @GetMapping("/quality-report/runs")
    @Operation(
            summary = "OCR 품질 리포트 실행 이력 조회",
            description = "최근 OCR 품질 리포트 실행/후보 등록 이력과 최근 리포트 간 추세를 조회합니다.")
    public ResponseEntity<QualityReportRunHistoryResponse> getQualityReportRuns(
            @RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(qualityReportRunService.getRecentRuns(limit));
    }

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(secretHeader, secretKey);
        return headers;
    }

    private VectorStatusResponse errorBody(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        return VectorStatusResponse.builder()
                .status("ERROR")
                .message(body != null && !body.isBlank() ? body : e.getStatusText())
                .isDegraded(true)
                .build();
    }

    private QualityReportRunResponse qualityReportErrorBody(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        return QualityReportRunResponse.builder()
                .success(false)
                .message(body != null && !body.isBlank() ? body : e.getStatusText())
                .build();
    }

    private QualityReportUpsertResponse qualityReportUpsertErrorBody(RestClientResponseException e) {
        String body = e.getResponseBodyAsString();
        return QualityReportUpsertResponse.builder()
                .success(false)
                .message(body != null && !body.isBlank() ? body : e.getStatusText())
                .build();
    }

    private void recordReportRun(
            Long adminId,
            QualityReportRunRequest request,
            QualityReportRunResponse response,
            boolean success,
            String message) {
        try {
            OcrQualityReportRun run = qualityReportRunService.recordReportRun(adminId, request, response, success, message);
            auditLogService.recordLog(
                    adminId,
                    OcrQualityReportRunService.ACTION_REPORT_RUN,
                    "OCR_QUALITY_REPORT",
                    run.getId(),
                    null,
                    objectMapper.writeValueAsString(reportAuditMeta(response)));
        } catch (Exception e) {
            log.warn("Failed to record OCR quality report audit history", e);
        }
    }

    private void recordUpsertRun(
            Long adminId,
            QualityReportUpsertRequest request,
            QualityReportUpsertResponse response,
            boolean success,
            String message) {
        try {
            OcrQualityReportRun run = qualityReportRunService.recordUpsertRun(adminId, request, response, success, message);
            auditLogService.recordLog(
                    adminId,
                    OcrQualityReportRunService.ACTION_ALIAS_UPSERT,
                    "OCR_QUALITY_REPORT",
                    run.getId(),
                    null,
                    objectMapper.writeValueAsString(upsertAuditMeta(response)));
        } catch (Exception e) {
            log.warn("Failed to record OCR quality report upsert audit history", e);
        }
    }

    private Map<String, Object> reportAuditMeta(QualityReportRunResponse response) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (response == null) {
            return meta;
        }
        meta.put("success", response.getSuccess());
        meta.put("message", response.getMessage());
        meta.put("generated_at", response.getGeneratedAt());
        meta.put("alias_candidate_count", response.getAliasCandidateCount());
        meta.put("manual_review_count", response.getManualReviewCount());
        meta.put("normalization_candidate_count", response.getNormalizationCandidateCount());
        return meta;
    }

    private Map<String, Object> upsertAuditMeta(QualityReportUpsertResponse response) {
        Map<String, Object> meta = new LinkedHashMap<>();
        if (response == null) {
            return meta;
        }
        meta.put("success", response.getSuccess());
        meta.put("message", response.getMessage());
        meta.put("generated_at", response.getGeneratedAt());
        meta.put("candidate_count", response.getCandidateCount());
        meta.put("upserted_count", response.getUpsertedCount());
        meta.put("skipped_count", response.getSkippedCount());
        return meta;
    }
}
