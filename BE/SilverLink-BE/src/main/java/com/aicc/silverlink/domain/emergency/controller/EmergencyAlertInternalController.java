package com.aicc.silverlink.domain.emergency.controller;

import com.aicc.silverlink.domain.emergency.dto.EmergencyAlertDto.CreateRequest;
import com.aicc.silverlink.domain.emergency.dto.EmergencyAlertDto.RealtimeResponse;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import com.aicc.silverlink.domain.emergency.service.EmergencyAlertService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * 긴급 알림 내부 API 컨트롤러
 * CallBot 시스템에서 호출하는 내부 전용 API
 *
 * 보안: 이 API는 내부 네트워크에서만 접근 가능해야 함
 * - API Gateway에서 /api/internal/** 경로 차단
 * - 또는 별도의 API Key 인증 적용
 */
@Slf4j
@RestController
@RequestMapping("/api/internal/emergency-alerts")
@RequiredArgsConstructor
@Tag(name = "긴급 알림 (내부)", description = "CallBot 전용 긴급 알림 생성 API")
public class EmergencyAlertInternalController {

        private final EmergencyAlertService alertService;

        /**
         * 긴급 알림 생성
         *
         * CallBot에서 위험 감지 시 호출
         * - 통화 중 실시간 위험 키워드 감지 시
         * - 연속 미응답 감지 시
         */
        @PostMapping
        @Operation(summary = "긴급 알림 생성", description = "CallBot에서 위험 감지 시 긴급 알림을 생성합니다. " +
                        "알림 생성 후 자동으로 관리자, 상담사, 보호자에게 웹/SMS 알림이 발송됩니다.")
        public ResponseEntity<?> createAlert(
                        @Valid @RequestBody CreateRequest request,
                        @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {

                // TODO: 내부 API 키 검증 (실제 구현 필요)
                // validateInternalApiKey(apiKey);

                log.info("[Internal API] POST /api/internal/emergency-alerts - 긴급 알림 생성 요청. " +
                                "elderlyId={}, severity={}, alertType={}",
                                request.getElderlyUserId(),
                                request.getSeverity(),
                                request.getAlertType());

                try {
                        EmergencyAlert alert = alertService.createAlert(request);

                        RealtimeResponse response = RealtimeResponse.from(alert);

                        log.info("[Internal API] 긴급 알림 생성 완료. alertId={}", alert.getId());
                        return ResponseEntity.status(HttpStatus.CREATED)
                                        .body(ApiResponse.success(response));

                } catch (IllegalArgumentException e) {
                        log.error("[Internal API] 긴급 알림 생성 실패 - 잘못된 요청. error={}", e.getMessage());
                        return ResponseEntity.badRequest()
                                        .body(ApiResponse.error(e.getMessage()));

                } catch (Exception e) {
                        log.error("[Internal API] 긴급 알림 생성 실패 - 서버 오류. error={}", e.getMessage(), e);
                        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                                        .body(ApiResponse.error("긴급 알림 생성 중 오류가 발생했습니다."));
                }
        }

        /**
         * 건강 위험 알림 생성 (간편 API)
         *
         * CallBot에서 건강 관련 위험 키워드 감지 시 호출
         */
        @PostMapping("/health")
        @Operation(summary = "건강 위험 알림 생성", description = "건강 관련 위험 키워드 감지 시 CRITICAL 수준의 긴급 알림을 생성합니다.")
        public ResponseEntity<ApiResponse<RealtimeResponse>> createHealthAlert(
                        @RequestBody HealthAlertRequest request,
                        @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {

                log.info("[Internal API] POST /api/internal/emergency-alerts/health - 건강 위험 알림. " +
                                "elderlyId={}, callId={}",
                                request.elderlyUserId(),
                                request.callId());

                CreateRequest createRequest = CreateRequest.builder()
                                .elderlyUserId(request.elderlyUserId())
                                .callId(request.callId())
                                .severity(EmergencyAlert.Severity.CRITICAL)
                                .alertType(EmergencyAlert.AlertType.HEALTH)
                                .title(request.title() != null ? request.title() : "건강 위험 감지")
                                .description(request.description())
                                .dangerKeywords(request.dangerKeywords())
                                .relatedSttContent(request.sttContent())
                                .build();

                EmergencyAlert alert = alertService.createAlert(createRequest);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(RealtimeResponse.from(alert)));
        }

        /**
         * 정서 위험 알림 생성 (간편 API)
         *
         * CallBot에서 정서적 위험 표현 감지 시 호출
         */
        @PostMapping("/mental")
        @Operation(summary = "정서 위험 알림 생성", description = "정서적 위험 표현 감지 시 긴급 알림을 생성합니다. " +
                        "자해/자살 암시는 CRITICAL, 우울감/외로움은 WARNING으로 생성됩니다.")
        public ResponseEntity<ApiResponse<RealtimeResponse>> createMentalAlert(
                        @RequestBody MentalAlertRequest request,
                        @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {

                log.info("[Internal API] POST /api/internal/emergency-alerts/mental - 정서 위험 알림. " +
                                "elderlyId={}, callId={}, isCritical={}",
                                request.elderlyUserId(),
                                request.callId(),
                                request.isCritical());

                EmergencyAlert.Severity severity = request.isCritical()
                                ? EmergencyAlert.Severity.CRITICAL
                                : EmergencyAlert.Severity.WARNING;

                CreateRequest createRequest = CreateRequest.builder()
                                .elderlyUserId(request.elderlyUserId())
                                .callId(request.callId())
                                .severity(severity)
                                .alertType(EmergencyAlert.AlertType.MENTAL)
                                .title(request.title() != null ? request.title() : "정서 위험 감지")
                                .description(request.description())
                                .dangerKeywords(request.dangerKeywords())
                                .relatedSttContent(request.sttContent())
                                .build();

                EmergencyAlert alert = alertService.createAlert(createRequest);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(RealtimeResponse.from(alert)));
        }

        /**
         * 연속 미응답 알림 생성 (간편 API)
         *
         * CallBot에서 연속 미응답 감지 시 호출
         */
        @PostMapping("/no-response")
        @Operation(summary = "연속 미응답 알림 생성", description = "연속 미응답 감지 시 WARNING 수준의 긴급 알림을 생성합니다.")
        public ResponseEntity<ApiResponse<RealtimeResponse>> createNoResponseAlert(
                        @RequestBody NoResponseAlertRequest request,
                        @RequestHeader(value = "X-Internal-Api-Key", required = false) String apiKey) {

                log.info("[Internal API] POST /api/internal/emergency-alerts/no-response - 연속 미응답 알림. " +
                                "elderlyId={}, attemptCount={}",
                                request.elderlyUserId(),
                                request.attemptCount());

                String title = String.format("%d회 연속 통화 미응답", request.attemptCount());
                String description = String.format(
                                "어르신이 %d회 연속 통화에 응답하지 않았습니다. 마지막 시도: %s",
                                request.attemptCount(),
                                request.lastAttemptTime());

                CreateRequest createRequest = CreateRequest.builder()
                                .elderlyUserId(request.elderlyUserId())
                                .callId(null) // 미응답이므로 통화 기록 없음
                                .severity(EmergencyAlert.Severity.WARNING)
                                .alertType(EmergencyAlert.AlertType.NO_RESPONSE)
                                .title(title)
                                .description(description)
                                .build();

                EmergencyAlert alert = alertService.createAlert(createRequest);
                return ResponseEntity.status(HttpStatus.CREATED)
                                .body(ApiResponse.success(RealtimeResponse.from(alert)));
        }

        // ========== Request DTOs ==========

        public record HealthAlertRequest(
                        Long elderlyUserId,
                        Long callId,
                        String title,
                        String description,
                        java.util.List<String> dangerKeywords,
                        String sttContent) {
        }

        public record MentalAlertRequest(
                        Long elderlyUserId,
                        Long callId,
                        boolean isCritical, // true: 자해/자살 암시, false: 우울감/외로움
                        String title,
                        String description,
                        java.util.List<String> dangerKeywords,
                        String sttContent) {
        }

        public record NoResponseAlertRequest(
                        Long elderlyUserId,
                        int attemptCount,
                        String lastAttemptTime) {
        }
}
