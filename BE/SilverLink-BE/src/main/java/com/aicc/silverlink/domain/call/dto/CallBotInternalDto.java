package com.aicc.silverlink.domain.call.dto;

import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * CallBot Internal API 요청/응답 DTO
 */
public class CallBotInternalDto {

    // ========== 통화 시작 ==========

    /**
     * 통화 시작 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class StartCallRequest {
        private Long elderlyId;
        private String name;
        private String phoneNumber;
        private LocalDateTime callAt;
    }

    /**
     * 통화 시작 응답
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class StartCallResponse {
        private Long callId;
        private Long elderlyId;
        private LocalDateTime callAt;
    }

    // ========== LLM Prompt 저장 ==========

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SavePromptRequest {
        private String prompt;
    }

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SavePromptResponse {
        private Long modelId;
    }

    // ========== 어르신 응답 저장 ==========

    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SaveReplyRequest {
        private String content;
        private Boolean danger;
    }

    // ========== 통화 로그 조회 ==========

    @Getter
    @Builder
    @AllArgsConstructor
    public static class CallLogResponse {
        private Long id;
        private String type; // PROMPT or REPLY
        private String content;
        private LocalDateTime timestamp;
    }

    // ========== 대화 메시지 저장 ==========

    /**
     * 대화 메시지 저장 요청 (CallBot 발화 또는 어르신 응답)
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MessageRequest {
        private String speaker; // "CALLBOT" or "ELDERLY"
        private String content; // 대화 내용
        private LocalDateTime timestamp;

        // 어르신 응답 시 위험 감지 정보 (선택)
        private Boolean danger;
        private String dangerReason;

        // CallBot 발화 ID (어르신 응답 시 연결)
        private Long llmModelId;
    }

    /**
     * 대화 메시지 저장 응답
     */
    @Getter
    @Builder
    @AllArgsConstructor
    public static class MessageResponse {
        private Long messageId;
        private String speaker;
        private LocalDateTime timestamp;
    }

    // ========== 대화 요약 저장 ==========

    /**
     * 통화 요약 저장 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SummaryRequest {
        private String content; // 요약 내용
    }

    // ========== 감정 분석 저장 ==========

    /**
     * 감정 분석 저장 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionRequest {
        private String emotionLevel; // GOOD, NORMAL, BAD, DEPRESSED
    }

    // ========== 일일 상태 저장 ==========

    /**
     * 일일 상태 저장 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStatusRequest {
        private Boolean mealTaken; // 식사 여부
        private String healthStatus; // GOOD, NORMAL, BAD
        private String healthDetail; // 건강 상태 상세
        private String sleepStatus; // GOOD, NORMAL, BAD
        private String sleepDetail; // 수면 상태 상세
    }

    // ========== 통화 종료 ==========

    /**
     * 통화 종료 요청
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EndCallRequest {
        private Integer callTimeSec; // 통화 시간 (초)
        private String recordingUrl; // 녹음 파일 URL

        // 선택: 종료 시 한번에 저장
        private SummaryRequest summary;
        private EmotionRequest emotion;
        private DailyStatusRequest dailyStatus;
    }

    // ========== 공통 응답 ==========

    @Getter
    @Builder
    @AllArgsConstructor
    public static class SimpleResponse {
        private boolean success;
        private String message;
        private Long id;
    }
}
