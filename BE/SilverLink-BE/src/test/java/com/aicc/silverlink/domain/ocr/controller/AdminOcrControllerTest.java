package com.aicc.silverlink.domain.ocr.controller;

import com.aicc.silverlink.domain.audit.service.AuditLogService;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunHistoryResponse;
import com.aicc.silverlink.domain.ocr.dto.VectorStatusResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunRequest;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportUpsertRequest;
import com.aicc.silverlink.domain.ocr.dto.QualityReportUpsertResponse;
import com.aicc.silverlink.domain.ocr.entity.OcrQualityReportRun;
import com.aicc.silverlink.domain.ocr.service.OcrQualityReportRunService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings({ "unchecked", "rawtypes" })
class AdminOcrControllerTest {

    private static final String PYTHON_URL = "http://localhost:8000";
    private static final String SECRET_HEADER = "X-SilverLink-Secret";
    private static final String SECRET_KEY = "test-secret";

    @Mock
    private RestTemplate restTemplate;

    @Mock
    private OcrQualityReportRunService qualityReportRunService;

    @Mock
    private AuditLogService auditLogService;

    private AdminOcrController controller;

    @BeforeEach
    void setUp() {
        controller = new AdminOcrController(restTemplate, qualityReportRunService, auditLogService, new ObjectMapper());
        ReflectionTestUtils.setField(controller, "pythonAiUrl", PYTHON_URL);
        ReflectionTestUtils.setField(controller, "secretHeader", SECRET_HEADER);
        ReflectionTestUtils.setField(controller, "secretKey", SECRET_KEY);
    }

    @Test
    @DisplayName("vector status request is proxied to Python internal endpoint with secret header")
    void getVectorStatusProxiesToPythonEndpointWithSecretHeader() {
        VectorStatusResponse body = VectorStatusResponse.builder()
                .collectionName("drug_embeddings")
                .persistDirectory("/code/chroma_db")
                .count(35291)
                .expectedCount(35291)
                .embeddingModel("text-embedding-3-small")
                .status("READY")
                .message("Vector DB is ready.")
                .isDegraded(false)
                .checkedAt("2026-04-30T00:00:00Z")
                .build();

        ArgumentCaptor<HttpEntity<Void>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/internal/vector/status"),
                eq(HttpMethod.GET),
                entityCaptor.capture(),
                eq(VectorStatusResponse.class)))
                .thenReturn(ResponseEntity.ok(body));

        ResponseEntity<VectorStatusResponse> response = controller.getVectorStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
        assertThat(entityCaptor.getValue().getHeaders().getFirst(SECRET_HEADER)).isEqualTo(SECRET_KEY);
    }

    @Test
    @DisplayName("AI 401 response status is preserved")
    void getVectorStatusPreservesUnauthorizedStatus() {
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/internal/vector/status"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(VectorStatusResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.UNAUTHORIZED, "missing secret"));

        ResponseEntity<VectorStatusResponse> response = controller.getVectorStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("ERROR");
        assertThat(response.getBody().getIsDegraded()).isTrue();
    }

    @Test
    @DisplayName("AI 500 response status is preserved")
    void getVectorStatusPreservesServerErrorStatus() {
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/internal/vector/status"),
                eq(HttpMethod.GET),
                org.mockito.ArgumentMatchers.<HttpEntity<Void>>any(),
                eq(VectorStatusResponse.class)))
                .thenThrow(new HttpServerErrorException(HttpStatus.INTERNAL_SERVER_ERROR, "vector failure"));

        ResponseEntity<VectorStatusResponse> response = controller.getVectorStatus();

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getStatus()).isEqualTo("ERROR");
        assertThat(response.getBody().getIsDegraded()).isTrue();
    }

    @Test
    @DisplayName("quality report run request is proxied to Python endpoint with secret header")
    void runQualityReportProxiesToPythonEndpointWithSecretHeader() {
        QualityReportRunResponse body = QualityReportRunResponse.builder()
                .success(true)
                .generatedAt("2026-04-30T00:00:00")
                .aliasCandidateCount(2)
                .manualReviewCount(1)
                .normalizationCandidateCount(1)
                .message("OCR quality report generated.")
                .build();

        ArgumentCaptor<HttpEntity<QualityReportRunRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/admin/quality-report/run"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(QualityReportRunResponse.class)))
                .thenReturn(ResponseEntity.ok(body));
        when(qualityReportRunService.recordReportRun(
                eq(1L),
                any(QualityReportRunRequest.class),
                eq(body),
                eq(true),
                eq(body.getMessage())))
                .thenReturn(OcrQualityReportRun.builder().id(10L).build());

        ResponseEntity<QualityReportRunResponse> response = controller.runQualityReport(
                QualityReportRunRequest.builder()
                        .limit(10)
                        .includeCandidates(true)
                        .persistFiles(false)
                        .build(),
                1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
        assertThat(entityCaptor.getValue().getHeaders().getFirst(SECRET_HEADER)).isEqualTo(SECRET_KEY);
        assertThat(entityCaptor.getValue().getBody()).isNotNull();
        assertThat(entityCaptor.getValue().getBody().getLimit()).isEqualTo(10);
        verify(auditLogService).recordLog(
                eq(1L),
                eq(OcrQualityReportRunService.ACTION_REPORT_RUN),
                eq("OCR_QUALITY_REPORT"),
                eq(10L),
                eq(null),
                any(String.class));
    }

    @Test
    @DisplayName("quality report upsert request is proxied to Python endpoint with confirm body")
    void upsertQualityReportAliasCandidatesProxiesToPythonEndpoint() {
        QualityReportUpsertResponse body = QualityReportUpsertResponse.builder()
                .success(true)
                .candidateCount(2)
                .upsertedCount(2)
                .skippedCount(1)
                .message("Upserted 2 alias candidate(s).")
                .build();

        ArgumentCaptor<HttpEntity<QualityReportUpsertRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/admin/quality-report/upsert-alias-candidates"),
                eq(HttpMethod.POST),
                entityCaptor.capture(),
                eq(QualityReportUpsertResponse.class)))
                .thenReturn(ResponseEntity.ok(body));
        when(qualityReportRunService.recordUpsertRun(
                eq(1L),
                any(QualityReportUpsertRequest.class),
                eq(body),
                eq(true),
                eq(body.getMessage())))
                .thenReturn(OcrQualityReportRun.builder().id(11L).build());

        ResponseEntity<QualityReportUpsertResponse> response = controller.upsertQualityReportAliasCandidates(
                QualityReportUpsertRequest.builder()
                        .limit(10)
                        .confirmWrite(true)
                        .build(),
                1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
        assertThat(entityCaptor.getValue().getHeaders().getFirst(SECRET_HEADER)).isEqualTo(SECRET_KEY);
        assertThat(entityCaptor.getValue().getBody()).isNotNull();
        assertThat(entityCaptor.getValue().getBody().getConfirmWrite()).isTrue();
        verify(auditLogService).recordLog(
                eq(1L),
                eq(OcrQualityReportRunService.ACTION_ALIAS_UPSERT),
                eq("OCR_QUALITY_REPORT"),
                eq(11L),
                eq(null),
                any(String.class));
    }

    @Test
    @DisplayName("quality report AI 400 response status is preserved")
    void upsertQualityReportAliasCandidatesPreservesBadRequestStatus() {
        when(restTemplate.exchange(
                eq(PYTHON_URL + "/api/ocr/admin/quality-report/upsert-alias-candidates"),
                eq(HttpMethod.POST),
                any(HttpEntity.class),
                eq(QualityReportUpsertResponse.class)))
                .thenThrow(new HttpClientErrorException(HttpStatus.BAD_REQUEST, "confirm required"));
        when(qualityReportRunService.recordUpsertRun(
                eq(1L),
                any(QualityReportUpsertRequest.class),
                any(QualityReportUpsertResponse.class),
                eq(false),
                any(String.class)))
                .thenReturn(OcrQualityReportRun.builder().id(12L).build());

        ResponseEntity<QualityReportUpsertResponse> response = controller.upsertQualityReportAliasCandidates(
                QualityReportUpsertRequest.builder().confirmWrite(false).build(),
                1L);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
        assertThat(response.getBody()).isNotNull();
        assertThat(response.getBody().getSuccess()).isFalse();
    }

    @Test
    @DisplayName("quality report run history is returned from service")
    void getQualityReportRunsReturnsRecentRuns() {
        QualityReportRunHistoryResponse body = QualityReportRunHistoryResponse.builder()
                .items(java.util.List.of())
                .trend(null)
                .build();
        when(qualityReportRunService.getRecentRuns(10)).thenReturn(body);

        ResponseEntity<QualityReportRunHistoryResponse> response = controller.getQualityReportRuns(10);

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isSameAs(body);
    }
}
