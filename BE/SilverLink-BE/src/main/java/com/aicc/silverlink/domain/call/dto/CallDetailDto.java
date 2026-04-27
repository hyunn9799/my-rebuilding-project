package com.aicc.silverlink.domain.call.dto;

import com.aicc.silverlink.domain.call.entity.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

/**
 * 통화 상세 조회 관련 DTO
 */
public class CallDetailDto {

    // ===== Response DTOs =====

    /**
     * 통화 상세 전체 응답 (메인)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CallDetailResponse {
        private Long callId;
        private LocalDateTime callAt;
        private String duration; // "15분 32초" 형식
        private int durationSeconds;
        private String state;
        private String stateKorean;

        // 어르신 정보
        private Long elderlyId;
        private String elderlyName;

        // 통화 요약
        private String summary;

        // 녹음 파일 URL
        private String recordingUrl;

        // 대화 내용
        private List<ConversationMessage> conversations;

        // 오늘의 상태
        private DailyStatusResponse dailyStatus;

        // AI 분석 결과
        private AiAnalysisResponse aiAnalysis;

        // 접근 권한 여부 (true: 전체 공개, false: 요약만 공개)
        private boolean isAccessGranted;

        public static CallDetailResponse from(CallRecord callRecord,
                String summary,
                List<ConversationMessage> conversations,
                DailyStatusResponse dailyStatus,
                AiAnalysisResponse aiAnalysis,
                boolean isAccessGranted) {
            return CallDetailResponse.builder()
                    .callId(callRecord.getId())
                    .callAt(callRecord.getCallAt())
                    .duration(callRecord.getFormattedDuration())
                    .durationSeconds(callRecord.getCallTimeSec())
                    .state(callRecord.getState().name())
                    .stateKorean(callRecord.getState().getKorean())
                    .elderlyId(callRecord.getElderly().getId())
                    .elderlyName(callRecord.getElderly().getUser().getName())
                    .summary(summary)
                    .recordingUrl(callRecord.getRecordingUrl())
                    .conversations(conversations)
                    .dailyStatus(dailyStatus)
                    .aiAnalysis(aiAnalysis)
                    .isAccessGranted(isAccessGranted)
                    .build();
        }

        // 기존 코드 호환용 (기본값 true)
        public static CallDetailResponse from(CallRecord callRecord,
                String summary,
                List<ConversationMessage> conversations,
                DailyStatusResponse dailyStatus,
                AiAnalysisResponse aiAnalysis) {
            return from(callRecord, summary, conversations, dailyStatus, aiAnalysis, true);
        }
    }

    /**
     * 대화 메시지 (단일)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ConversationMessage {
        private Long id;
        private Speaker speaker; // CALLBOT or ELDERLY
        private String content;
        private LocalDateTime timestamp;
        private int offsetSeconds; // 통화 시작부터 경과 시간 (00:08 표시용)

        // 어르신 응답의 경우 위험 감지 여부
        private Boolean isDanger;
        private String dangerReason;

        public enum Speaker {
            CALLBOT("AI"),
            ELDERLY("어르신");

            private final String korean;

            Speaker(String korean) {
                this.korean = korean;
            }

            public String getKorean() {
                return korean;
            }
        }

        /**
         * LlmModel(CallBot 발화)에서 변환
         */
        public static ConversationMessage fromLlmModel(LlmModel model, LocalDateTime callStartTime) {
            int offset = (int) java.time.Duration.between(callStartTime, model.getCreatedAt()).getSeconds();
            return ConversationMessage.builder()
                    .id(model.getId())
                    .speaker(Speaker.CALLBOT)
                    .content(model.getPrompt())
                    .timestamp(model.getCreatedAt())
                    .offsetSeconds(Math.max(0, offset))
                    .isDanger(false)
                    .build();
        }

        /**
         * ElderlyResponse(어르신 응답)에서 변환
         */
        public static ConversationMessage fromElderlyResponse(ElderlyResponse response, LocalDateTime callStartTime) {
            int offset = (int) java.time.Duration.between(callStartTime, response.getRespondedAt()).getSeconds();
            return ConversationMessage.builder()
                    .id(response.getId())
                    .speaker(Speaker.ELDERLY)
                    .content(response.getContent())
                    .timestamp(response.getRespondedAt())
                    .offsetSeconds(Math.max(0, offset))
                    .isDanger(response.isDanger())
                    .dangerReason(response.getDangerReason())
                    .build();
        }

        /**
         * offset을 "mm:ss" 형식으로 반환
         */
        public String getOffsetFormatted() {
            int minutes = offsetSeconds / 60;
            int seconds = offsetSeconds % 60;
            return String.format("%02d:%02d", minutes, seconds);
        }
    }

    /**
     * 오늘의 상태 응답
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class DailyStatusResponse {
        // 감정 상태
        private EmotionStatus emotion;

        // 식사 여부 (단순화: 먹었는지 여부만)
        private MealStatus meal;

        // 건강 상태
        private HealthStatus health;

        // 수면 상태
        private SleepStatus sleep;

        public static DailyStatusResponse from(CallEmotion emotion, CallDailyStatus dailyStatus) {
            return DailyStatusResponse.builder()
                    .emotion(EmotionStatus.from(emotion))
                    .meal(MealStatus.from(dailyStatus))
                    .health(HealthStatus.from(dailyStatus))
                    .sleep(SleepStatus.from(dailyStatus))
                    .build();
        }
    }

    /**
     * 감정 상태
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class EmotionStatus {
        private String level; // GOOD, NORMAL, BAD, DEPRESSED
        private String levelKorean; // 좋음, 보통, 나쁨, 우울
        private Integer score; // 0-100 점수 (nullable)

        public static EmotionStatus from(CallEmotion emotion) {
            if (emotion == null) {
                return EmotionStatus.builder()
                        .level("UNKNOWN")
                        .levelKorean("미확인")
                        .build();
            }

            int score = calculateScore(emotion.getEmotionLevel());

            return EmotionStatus.builder()
                    .level(emotion.getEmotionLevel().name())
                    .levelKorean(emotion.getEmotionLevelKorean())
                    .score(score)
                    .build();
        }

        private static int calculateScore(EmotionLevel level) {
            return switch (level) {
                case GOOD -> 85;
                case NORMAL -> 65;
                case BAD -> 40;
                case DEPRESSED -> 20;
            };
        }
    }

    /**
     * 식사 여부 (단순화)
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MealStatus {
        private Boolean taken; // 식사 여부 (null: 미확인)
        private String status; // "식사함", "식사 안함", "미확인"

        public static MealStatus from(CallDailyStatus dailyStatus) {
            if (dailyStatus == null || dailyStatus.getMealTaken() == null) {
                return MealStatus.builder()
                        .taken(null)
                        .status("미확인")
                        .build();
            }

            boolean taken = dailyStatus.getMealTaken();
            return MealStatus.builder()
                    .taken(taken)
                    .status(taken ? "식사함" : "식사 안함")
                    .build();
        }
    }

    /**
     * 건강 상태
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HealthStatus {
        private String level; // GOOD, NORMAL, BAD
        private String levelKorean; // 좋음, 보통, 나쁨
        private String detail; // 상세 내용

        public static HealthStatus from(CallDailyStatus dailyStatus) {
            if (dailyStatus == null || dailyStatus.getHealthStatus() == null) {
                return HealthStatus.builder()
                        .level("UNKNOWN")
                        .levelKorean("미확인")
                        .build();
            }

            return HealthStatus.builder()
                    .level(dailyStatus.getHealthStatus().name())
                    .levelKorean(dailyStatus.getHealthStatusKorean())
                    .detail(dailyStatus.getHealthDetail())
                    .build();
        }
    }

    /**
     * 수면 상태
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class SleepStatus {
        private String level; // GOOD, NORMAL, BAD
        private String levelKorean; // 좋음, 보통, 나쁨
        private String detail; // 상세 내용

        public static SleepStatus from(CallDailyStatus dailyStatus) {
            if (dailyStatus == null || dailyStatus.getSleepStatus() == null) {
                return SleepStatus.builder()
                        .level("UNKNOWN")
                        .levelKorean("미확인")
                        .build();
            }

            return SleepStatus.builder()
                    .level(dailyStatus.getSleepStatus().name())
                    .levelKorean(dailyStatus.getSleepStatusKorean())
                    .detail(dailyStatus.getSleepDetail())
                    .build();
        }
    }

    /**
     * AI 분석 결과
     */
    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiAnalysisResponse {
        private EmotionStatus emotion;
        private boolean hasDangerSignal; // 위험 신호 감지 여부
        private List<String> dangerReasons; // 위험 신호 사유들
        private List<String> keywords; // 주요 키워드
        private String overallAssessment; // 종합 평가

        public static AiAnalysisResponse from(CallEmotion emotion,
                List<ElderlyResponse> responses) {
            // 위험 신호 수집
            List<String> dangerReasons = responses.stream()
                    .filter(ElderlyResponse::isDanger)
                    .map(ElderlyResponse::getDangerReason)
                    .filter(reason -> reason != null && !reason.isBlank())
                    .toList();

            boolean hasDanger = !dangerReasons.isEmpty();

            return AiAnalysisResponse.builder()
                    .emotion(EmotionStatus.from(emotion))
                    .hasDangerSignal(hasDanger)
                    .dangerReasons(dangerReasons)
                    .overallAssessment(generateAssessment(emotion, hasDanger))
                    .build();
        }

        private static String generateAssessment(CallEmotion emotion, boolean hasDanger) {
            if (hasDanger) {
                return "주의가 필요합니다. 위험 신호가 감지되었습니다.";
            }
            if (emotion == null) {
                return "분석 정보가 부족합니다.";
            }
            return switch (emotion.getEmotionLevel()) {
                case GOOD -> "전반적으로 좋은 상태입니다.";
                case NORMAL -> "평소와 비슷한 상태입니다.";
                case BAD -> "컨디션이 좋지 않은 것으로 보입니다.";
                case DEPRESSED -> "우울감이 감지됩니다. 상담사 확인이 필요합니다.";
            };
        }
    }

    // ===== Utility Methods =====

    /**
     * LlmModel과 ElderlyResponse를 시간순으로 병합하여 대화 목록 생성
     */
    public static List<ConversationMessage> mergeConversations(
            List<LlmModel> llmModels,
            List<ElderlyResponse> elderlyResponses,
            LocalDateTime callStartTime) {

        List<ConversationMessage> messages = new ArrayList<>();

        // LlmModel -> ConversationMessage
        for (LlmModel model : llmModels) {
            messages.add(ConversationMessage.fromLlmModel(model, callStartTime));
        }

        // ElderlyResponse -> ConversationMessage
        for (ElderlyResponse response : elderlyResponses) {
            messages.add(ConversationMessage.fromElderlyResponse(response, callStartTime));
        }

        // 시간순 정렬
        messages.sort(Comparator.comparing(ConversationMessage::getTimestamp));

        return messages;
    }
}