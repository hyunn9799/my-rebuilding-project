package com.aicc.silverlink.domain.ocr.controller;

import com.aicc.silverlink.domain.ocr.dto.AliasSuggestionActionResponse;
import com.aicc.silverlink.domain.ocr.dto.AliasSuggestionPageResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

/**
 * 관리자 Alias 승인/거부 프록시 컨트롤러.
 * Python AI 서버의 /api/ocr/admin/* 엔드포인트를 프록시합니다.
 */
@Slf4j
@RestController
@RequestMapping("/api/admin/alias-suggestions")
@RequiredArgsConstructor
@Tag(name = "Admin - Alias Management", description = "관리자 Alias 제안 관리 API")
public class AdminAliasController {

    private final RestTemplate restTemplate;

    @Value("${chatbot.python.url:http://localhost:8000}")
    private String pythonAiUrl;

    @Value("${chatbot.secret.header:X-SilverLink-Secret}")
    private String secretHeader;

    @Value("${chatbot.secret.key:X-SilverLink-Key!}")
    private String secretKey;

    private HttpHeaders createJsonHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set(secretHeader, secretKey);
        return headers;
    }

    /**
     * PENDING alias 제안 목록 (페이징).
     */
    @GetMapping
    @Operation(summary = "PENDING alias 제안 목록", description = "관리자가 검토할 alias 제안을 페이징으로 조회합니다.")
    public ResponseEntity<AliasSuggestionPageResponse> getAliasSuggestions(
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "PENDING") String reviewStatus) {

        log.info("Admin alias suggestions request: page={}, size={}, status={}", page, size, reviewStatus);

        try {
            String pythonUrl = pythonAiUrl + "/api/ocr/admin/alias-suggestions"
                    + "?page=" + page + "&size=" + size + "&review_status=" + reviewStatus;
            HttpEntity<Void> entity = new HttpEntity<>(createJsonHeaders());

            ResponseEntity<AliasSuggestionPageResponse> response = restTemplate.exchange(
                    pythonUrl,
                    HttpMethod.GET,
                    entity,
                    AliasSuggestionPageResponse.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to get alias suggestions from AI server", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AliasSuggestionPageResponse.builder()
                            .total(0).page(page).size(size).build());
        }
    }

    /**
     * alias 제안 승인 → 실제 alias/error_alias 테이블에 등록.
     */
    @PutMapping("/{suggestionId}/approve")
    @Operation(summary = "alias 제안 승인", description = "PENDING 제안을 승인하여 실제 alias 테이블에 등록합니다.")
    public ResponseEntity<AliasSuggestionActionResponse> approveSuggestion(
            @PathVariable Long suggestionId,
            @RequestParam(defaultValue = "admin") String reviewedBy) {

        log.info("Admin approve alias suggestion: id={}, by={}", suggestionId, reviewedBy);

        try {
            String pythonUrl = pythonAiUrl + "/api/ocr/admin/alias-suggestions/"
                    + suggestionId + "/approve?reviewed_by=" + reviewedBy;
            HttpEntity<Void> entity = new HttpEntity<>(createJsonHeaders());

            ResponseEntity<AliasSuggestionActionResponse> response = restTemplate.exchange(
                    pythonUrl,
                    HttpMethod.PUT,
                    entity,
                    AliasSuggestionActionResponse.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Failed to approve alias suggestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AliasSuggestionActionResponse.builder()
                            .success(false)
                            .message("승인 처리 실패: " + e.getMessage())
                            .build());
        }
    }

    /**
     * alias 제안 거부.
     */
    @PutMapping("/{suggestionId}/reject")
    @Operation(summary = "alias 제안 거부", description = "PENDING 제안을 거부합니다.")
    public ResponseEntity<AliasSuggestionActionResponse> rejectSuggestion(
            @PathVariable Long suggestionId,
            @RequestParam(defaultValue = "admin") String reviewedBy) {

        log.info("Admin reject alias suggestion: id={}, by={}", suggestionId, reviewedBy);

        try {
            String pythonUrl = pythonAiUrl + "/api/ocr/admin/alias-suggestions/"
                    + suggestionId + "/reject?reviewed_by=" + reviewedBy;
            HttpEntity<Void> entity = new HttpEntity<>(createJsonHeaders());

            ResponseEntity<AliasSuggestionActionResponse> response = restTemplate.exchange(
                    pythonUrl,
                    HttpMethod.PUT,
                    entity,
                    AliasSuggestionActionResponse.class);

            return ResponseEntity.status(response.getStatusCode()).body(response.getBody());
        } catch (Exception e) {
            log.error("Failed to reject alias suggestion", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AliasSuggestionActionResponse.builder()
                            .success(false)
                            .message("거부 처리 실패: " + e.getMessage())
                            .build());
        }
    }

    /**
     * 승인 후 LocalDrugIndex 리로드 요청.
     */
    @PostMapping("/reload-dictionary")
    @Operation(summary = "LocalDrugIndex 리로드", description = "alias 승인 후 인메모리 약품 인덱스를 갱신합니다.")
    public ResponseEntity<AliasSuggestionActionResponse> reloadDictionary() {

        log.info("Admin reload dictionary request");

        try {
            String pythonUrl = pythonAiUrl + "/api/ocr/admin/reload-dictionary";
            HttpEntity<Void> entity = new HttpEntity<>(createJsonHeaders());

            ResponseEntity<AliasSuggestionActionResponse> response = restTemplate.exchange(
                    pythonUrl,
                    HttpMethod.POST,
                    entity,
                    AliasSuggestionActionResponse.class);

            return ResponseEntity.ok(response.getBody());
        } catch (Exception e) {
            log.error("Failed to reload dictionary", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(AliasSuggestionActionResponse.builder()
                            .success(false)
                            .message("리로드 실패: " + e.getMessage())
                            .build());
        }
    }
}
