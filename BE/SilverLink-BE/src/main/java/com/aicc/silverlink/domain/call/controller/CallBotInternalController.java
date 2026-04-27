package com.aicc.silverlink.domain.call.controller;

import com.aicc.silverlink.domain.call.dto.CallBotInternalDto.*;
import com.aicc.silverlink.domain.call.service.CallBotInternalService;
import com.aicc.silverlink.global.common.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

/**
 * CallBot Internal API Controller
 * 
 * Python CallBot에서 호출하여 통화 데이터를 저장하는 API
 * 
 * 주의: 이 API는 내부 시스템(CallBot)에서만 호출되어야 합니다.
 * 추후 API Key 인증 또는 IP 화이트리스트 적용 권장.
 */
@Tag(name = "CallBot Internal", description = "Python CallBot용 Internal API")
@RestController
@RequestMapping("/api/internal/callbot")
@RequiredArgsConstructor
@Slf4j
public class CallBotInternalController {

    private final CallBotInternalService callBotInternalService;
    private final com.aicc.silverlink.global.sse.CallBotSseService sseService;

    // ========== SSE 연결 ==========

    @Operation(summary = "SSE 연결", description = "통화 모니터링을 위한 SSE 연결")
    @GetMapping(value = "/calls/{callId}/sse", produces = "text/event-stream")
    public org.springframework.web.servlet.mvc.method.annotation.SseEmitter subscribe(@PathVariable Long callId) {
        return sseService.connect(callId);
    }

    // ========== 통화 시작 ==========

    @Operation(summary = "통화 시작", description = "새로운 통화를 시작하고 CallRecord를 생성합니다")
    @PostMapping("/calls")
    public ResponseEntity<ApiResponse<StartCallResponse>> startCall(
            @Valid @RequestBody StartCallRequest request) {

        StartCallResponse response = callBotInternalService.startCall(request);
        log.info("[CallBot API] 통화 시작: elderlyId={}", request.getElderlyId());
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== LLM Prompt 저장 ==========

    @PostMapping("/calls/{callId}/llm/prompt")
    public ResponseEntity<ApiResponse<Void>> savePrompt(
            @PathVariable Long callId,
            @RequestBody SavePromptRequest request) {
        callBotInternalService.savePrompt(callId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @PostMapping("/calls/{callId}/llm/reply")
    public ResponseEntity<ApiResponse<Void>> saveReply(
            @PathVariable Long callId,
            @RequestBody SaveReplyRequest request) {
        callBotInternalService.saveReply(callId, request);
        return ResponseEntity.ok(ApiResponse.success(null));
    }

    @Operation(summary = "통화 로그 조회", description = "저장된 통화 내용(Prompt + Reply) 전체를 시간순으로 조회합니다")
    @GetMapping("/calls/{callId}/logs")
    public ResponseEntity<ApiResponse<java.util.List<CallLogResponse>>> getCallLogs(@PathVariable Long callId) {
        return ResponseEntity.ok(ApiResponse.success(callBotInternalService.getCallLogs(callId)));
    }

    // ========== 대화 메시지 저장 ==========

    @Operation(summary = "대화 메시지 저장", description = "CallBot 발화 또는 어르신 응답을 저장합니다. speaker: 'CALLBOT' 또는 'ELDERLY'")
    @PostMapping("/calls/{callId}/messages")
    public ResponseEntity<ApiResponse<MessageResponse>> saveMessage(
            @PathVariable Long callId,
            @Valid @RequestBody MessageRequest request) {

        MessageResponse response = callBotInternalService.saveMessage(callId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 통화 요약 저장 ==========

    @Operation(summary = "통화 요약 저장", description = "통화 내용 요약을 저장합니다")
    @PostMapping("/calls/{callId}/summary")
    public ResponseEntity<ApiResponse<SimpleResponse>> saveSummary(
            @PathVariable Long callId,
            @Valid @RequestBody SummaryRequest request) {

        SimpleResponse response = callBotInternalService.saveSummary(callId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 감정 분석 저장 ==========

    @Operation(summary = "감정 분석 저장", description = "감정 분석 결과를 저장합니다. emotionLevel: GOOD, NORMAL, BAD, DEPRESSED")
    @PostMapping("/calls/{callId}/emotion")
    public ResponseEntity<ApiResponse<SimpleResponse>> saveEmotion(
            @PathVariable Long callId,
            @Valid @RequestBody EmotionRequest request) {

        SimpleResponse response = callBotInternalService.saveEmotion(callId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 일일 상태 저장 ==========

    @Operation(summary = "일일 상태 저장", description = "식사, 건강, 수면 상태를 저장합니다. statusLevel: GOOD, NORMAL, BAD")
    @PostMapping("/calls/{callId}/daily-status")
    public ResponseEntity<ApiResponse<SimpleResponse>> saveDailyStatus(
            @PathVariable Long callId,
            @Valid @RequestBody DailyStatusRequest request) {

        SimpleResponse response = callBotInternalService.saveDailyStatus(callId, request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ========== 통화 종료 ==========

    @Operation(summary = "통화 종료", description = "통화를 종료하고 최종 데이터(요약, 감정, 일일상태)를 저장합니다")
    @PostMapping("/calls/{callId}/end")
    public ResponseEntity<ApiResponse<SimpleResponse>> endCall(
            @PathVariable Long callId,
            @Valid @RequestBody EndCallRequest request) {

        SimpleResponse response = callBotInternalService.endCall(callId, request);
        log.info("[CallBot API] 통화 종료: callId={}, duration={}sec", callId, request.getCallTimeSec());
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
