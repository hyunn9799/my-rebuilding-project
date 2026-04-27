package com.aicc.silverlink.domain.call.dto;

import com.aicc.silverlink.domain.call.entity.*;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 통화 리뷰 관련 DTO 클래스들
 */
public class CallReviewDto {

    // ===== Request DTOs =====

    /**
     * 상담사 통화 리뷰 생성/수정 요청
     */
    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class ReviewRequest {
        @NotNull(message = "통화 ID는 필수입니다.")
        private Long callId;

        @Size(max = 2000, message = "코멘트는 2000자 이내로 입력해주세요.")
        private String comment;

        private boolean urgent;
    }

    /**
     * 통화 목록 조회 필터
     */
    @Getter
    @Setter
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder
    public static class CallListFilter {
        private Long elderlyId;
        private LocalDateTime startDate;
        private LocalDateTime endDate;
        private Boolean reviewed; // 확인 여부 필터
        private Boolean hasDanger; // 위험 응답 여부 필터
        private EmotionLevel emotionLevel;
    }

    // ===== Response DTOs =====

    /**
     * 통화 기록 목록 응답 (요약)
     */
    @Getter
    @Builder
    public static class CallRecordSummaryResponse {
        private Long callId;
        private Long elderlyId;
        private String elderlyName;
        private LocalDateTime callAt;
        private String duration; // "분:초" 형식
        private String state;
        private String stateKorean;
        private String emotionLevel;
        private String emotionLevelKorean;
        private boolean hasDangerResponse;
        private boolean reviewed; // 상담사 확인 여부
        private String summaryPreview; // 요약 미리보기 (최대 100자)

        public static CallRecordSummaryResponse from(CallRecord callRecord, boolean reviewed) {
            // 최신 감정 상태
            CallEmotion latestEmotion = callRecord.getEmotions().isEmpty() ? null
                    : callRecord.getEmotions().get(callRecord.getEmotions().size() - 1);

            // 요약 미리보기
            String summary = callRecord.getSummaries().isEmpty() ? null : callRecord.getSummaries().get(0).getContent();
            String summaryPreview = summary != null && summary.length() > 100 ? summary.substring(0, 100) + "..."
                    : summary;

            return CallRecordSummaryResponse.builder()
                    .callId(callRecord.getId())
                    .elderlyId(callRecord.getElderly().getId())
                    .elderlyName(callRecord.getElderly().getUser().getName())
                    .callAt(callRecord.getCallAt())
                    .duration(callRecord.getFormattedDuration())
                    .state(callRecord.getState().name())
                    .stateKorean(callRecord.getState().getKorean())
                    .emotionLevel(latestEmotion != null ? latestEmotion.getEmotionLevel().name() : null)
                    .emotionLevelKorean(latestEmotion != null ? latestEmotion.getEmotionLevelKorean() : null)
                    .hasDangerResponse(callRecord.hasDangerResponse())
                    .reviewed(reviewed)
                    .summaryPreview(summaryPreview)
                    .build();
        }
    }

    /**
     * 통화 기록 상세 응답
     */
    @Getter
    @Builder
    public static class CallRecordDetailResponse {
        private Long callId;
        private ElderlyInfo elderly;
        private LocalDateTime callAt;
        private String duration;
        private Integer callTimeSec;
        private String state;
        private String stateKorean;
        private String recordingUrl; // 녹음 파일 URL 추가
        private List<ResponseItem> responses;
        private List<PromptItem> prompts; // AI 발화 추가
        private List<SummaryItem> summaries;
        private List<EmotionItem> emotions;
        private ReviewInfo review;
        private DailyStatusInfo dailyStatus; // 오늘의 상태 (식사, 건강, 수면)

        @Getter
        @Builder
        public static class ElderlyInfo {
            private Long id;
            private String name;
            private String phone;
            private Integer age;
            private String gender;
        }

        @Getter
        @Builder
        public static class ResponseItem {
            private Long responseId;
            private String content;
            private LocalDateTime respondedAt;
            private boolean danger;
            private String dangerReason;
        }

        @Getter
        @Builder
        public static class SummaryItem {
            private Long summaryId;
            private String content;
            private LocalDateTime createdAt;
        }

        @Getter
        @Builder
        public static class EmotionItem {
            private Long emotionId;
            private String emotionLevel;
            private String emotionLevelKorean;
            private LocalDateTime createdAt;
        }

        @Getter
        @Builder
        public static class PromptItem {
            private Long promptId;
            private String content;
            private LocalDateTime createdAt;
        }

        @Getter
        @Builder
        public static class ReviewInfo {
            private Long reviewId;
            private Long counselorId;
            private String counselorName;
            private LocalDateTime reviewedAt;
            private String comment;
            private boolean urgent;
        }

        public static CallRecordDetailResponse from(CallRecord callRecord, CounselorCallReview review,
                String presignedRecordingUrl, CallDailyStatus dailyStatus) {
            List<ResponseItem> responseItems = callRecord.getElderlyResponses().stream()
                    .map(r -> ResponseItem.builder()
                            .responseId(r.getId())
                            .content(r.getContent())
                            .respondedAt(r.getRespondedAt())
                            .danger(r.isDanger())
                            .dangerReason(r.getDangerReason())
                            .build())
                    .toList();

            List<SummaryItem> summaryItems = callRecord.getSummaries().stream()
                    .map(s -> SummaryItem.builder()
                            .summaryId(s.getId())
                            .content(s.getContent())
                            .createdAt(s.getCreatedAt())
                            .build())
                    .toList();

            List<EmotionItem> emotionItems = callRecord.getEmotions().stream()
                    .map(e -> EmotionItem.builder()
                            .emotionId(e.getId())
                            .emotionLevel(e.getEmotionLevel().name())
                            .emotionLevelKorean(e.getEmotionLevelKorean())
                            .createdAt(e.getCreatedAt())
                            .build())
                    .toList();

            List<PromptItem> promptItems = callRecord.getLlmModels().stream()
                    .map(p -> PromptItem.builder()
                            .promptId(p.getId())
                            .content(p.getPrompt())
                            .createdAt(p.getCreatedAt())
                            .build())
                    .toList();

            ReviewInfo reviewInfo = review == null ? null
                    : ReviewInfo.builder()
                            .reviewId(review.getId())
                            .counselorId(review.getCounselor().getId())
                            .counselorName(review.getCounselor().getUser().getName())
                            .reviewedAt(review.getReviewedAt())
                            .comment(review.getComment())
                            .urgent(review.isUrgent())
                            .build();

            var elderly = callRecord.getElderly();
            ElderlyInfo elderlyInfo = ElderlyInfo.builder()
                    .id(elderly.getId())
                    .name(elderly.getUser().getName())
                    .phone(elderly.getUser().getPhone())
                    .age(elderly.age())
                    .gender(elderly.getGender().name())
                    .build();

            return CallRecordDetailResponse.builder()
                    .callId(callRecord.getId())
                    .elderly(elderlyInfo)
                    .callAt(callRecord.getCallAt())
                    .duration(callRecord.getFormattedDuration())
                    .callTimeSec(callRecord.getCallTimeSec())
                    .state(callRecord.getState().name())
                    .stateKorean(callRecord.getState().getKorean())
                    .recordingUrl(presignedRecordingUrl)
                    .responses(responseItems)
                    .prompts(promptItems)
                    .summaries(summaryItems)
                    .emotions(emotionItems)
                    .review(reviewInfo)
                    .dailyStatus(DailyStatusInfo.from(dailyStatus))
                    .build();
        }
    }

    /**
     * 상담사 통화 리뷰 응답
     */
    @Getter
    @Builder
    public static class ReviewResponse {
        private Long reviewId;
        private Long callId;
        private Long counselorId;
        private String counselorName;
        private LocalDateTime reviewedAt;
        private String comment;
        private boolean urgent;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;

        public static ReviewResponse from(CounselorCallReview review) {
            return ReviewResponse.builder()
                    .reviewId(review.getId())
                    .callId(review.getCallRecord().getId())
                    .counselorId(review.getCounselor().getId())
                    .counselorName(review.getCounselor().getUser().getName())
                    .reviewedAt(review.getReviewedAt())
                    .comment(review.getComment())
                    .urgent(review.isUrgent())
                    .createdAt(review.getCreatedAt())
                    .updatedAt(review.getUpdatedAt())
                    .build();
        }
    }

    /**
     * 보호자용 통화 리뷰 응답 (상담사 코멘트 포함)
     */
    @Getter
    @Builder
    public static class GuardianCallReviewResponse {
        private Long callId;
        private String elderlyName;
        private LocalDateTime callAt;
        private String duration;
        private String state;
        private String stateKorean;
        private String summary;
        private String emotionLevel;
        private String emotionLevelKorean;
        private boolean hasDangerResponse;
        private String counselorName;
        private String counselorComment;
        private boolean urgent;
        private LocalDateTime reviewedAt;

        // 대화 내용
        private List<CallRecordDetailResponse.PromptItem> prompts;
        private List<CallRecordDetailResponse.ResponseItem> responses;

        // 오늘의 상태 (식사, 건강, 수면)
        private DailyStatusInfo dailyStatus;

        public static GuardianCallReviewResponse from(CallRecord callRecord, CounselorCallReview review, CallDailyStatus dailyStatus) {
            CallEmotion latestEmotion = callRecord.getEmotions().isEmpty() ? null
                    : callRecord.getEmotions().get(callRecord.getEmotions().size() - 1);
            String summary = callRecord.getSummaries().isEmpty() ? null : callRecord.getSummaries().get(0).getContent();

            List<CallRecordDetailResponse.PromptItem> promptItems = callRecord.getLlmModels().stream()
                    .map(p -> CallRecordDetailResponse.PromptItem.builder()
                            .promptId(p.getId())
                            .content(p.getPrompt())
                            .createdAt(p.getCreatedAt())
                            .build())
                    .toList();

            List<CallRecordDetailResponse.ResponseItem> responseItems = callRecord.getElderlyResponses().stream()
                    .map(r -> CallRecordDetailResponse.ResponseItem.builder()
                            .responseId(r.getId())
                            .content(r.getContent())
                            .respondedAt(r.getRespondedAt())
                            .danger(r.isDanger())
                            .dangerReason(r.getDangerReason())
                            .build())
                    .toList();

            return GuardianCallReviewResponse.builder()
                    .callId(callRecord.getId())
                    .elderlyName(callRecord.getElderly().getUser().getName())
                    .callAt(callRecord.getCallAt())
                    .duration(callRecord.getFormattedDuration())
                    .state(callRecord.getState().name())
                    .stateKorean(callRecord.getState().getKorean())
                    .summary(summary)
                    .emotionLevel(latestEmotion != null ? latestEmotion.getEmotionLevel().name() : null)
                    .emotionLevelKorean(latestEmotion != null ? latestEmotion.getEmotionLevelKorean() : null)
                    .hasDangerResponse(callRecord.hasDangerResponse())
                    .counselorName(review != null ? review.getCounselor().getUser().getName() : null)
                    .counselorComment(review != null ? review.getComment() : null)
                    .urgent(review != null && review.isUrgent())
                    .reviewedAt(review != null ? review.getReviewedAt() : null)
                    .prompts(promptItems)
                    .responses(responseItems)
                    .dailyStatus(DailyStatusInfo.from(dailyStatus))
                    .build();
        }
    }

    /**
     * 미확인 통화 건수 응답
     */
    @Getter
    @Builder
    public static class UnreviewedCountResponse {
        private long unreviewedCount;
        private long totalCount;
    }

    // ===== 오늘의 상태 (Daily Status) DTOs =====

    /**
     * 오늘의 상태 응답 (식사, 건강, 수면)
     */
    @Getter
    @Builder
    public static class DailyStatusInfo {
        private MealInfo meal;
        private HealthInfo health;
        private SleepInfo sleep;

        public static DailyStatusInfo from(CallDailyStatus dailyStatus) {
            if (dailyStatus == null) {
                return DailyStatusInfo.builder()
                        .meal(MealInfo.unknown())
                        .health(HealthInfo.unknown())
                        .sleep(SleepInfo.unknown())
                        .build();
            }

            return DailyStatusInfo.builder()
                    .meal(MealInfo.from(dailyStatus))
                    .health(HealthInfo.from(dailyStatus))
                    .sleep(SleepInfo.from(dailyStatus))
                    .build();
        }
    }

    @Getter
    @Builder
    public static class MealInfo {
        private Boolean taken;
        private String status;  // "식사함", "식사 안함", "미확인"

        public static MealInfo from(CallDailyStatus dailyStatus) {
            return MealInfo.builder()
                    .taken(dailyStatus.getMealTaken())
                    .status(dailyStatus.getMealStatusKorean())
                    .build();
        }

        public static MealInfo unknown() {
            return MealInfo.builder()
                    .taken(null)
                    .status("미확인")
                    .build();
        }
    }

    @Getter
    @Builder
    public static class HealthInfo {
        private String level;        // GOOD, NORMAL, BAD, or null
        private String levelKorean;  // 좋음, 보통, 나쁨, 미확인
        private String detail;

        public static HealthInfo from(CallDailyStatus dailyStatus) {
            return HealthInfo.builder()
                    .level(dailyStatus.getHealthStatus() != null ? dailyStatus.getHealthStatus().name() : null)
                    .levelKorean(dailyStatus.getHealthStatusKorean())
                    .detail(dailyStatus.getHealthDetail())
                    .build();
        }

        public static HealthInfo unknown() {
            return HealthInfo.builder()
                    .level(null)
                    .levelKorean("미확인")
                    .detail(null)
                    .build();
        }
    }

    @Getter
    @Builder
    public static class SleepInfo {
        private String level;        // GOOD, NORMAL, BAD, or null
        private String levelKorean;  // 좋음, 보통, 나쁨, 미확인
        private String detail;

        public static SleepInfo from(CallDailyStatus dailyStatus) {
            return SleepInfo.builder()
                    .level(dailyStatus.getSleepStatus() != null ? dailyStatus.getSleepStatus().name() : null)
                    .levelKorean(dailyStatus.getSleepStatusKorean())
                    .detail(dailyStatus.getSleepDetail())
                    .build();
        }

        public static SleepInfo unknown() {
            return SleepInfo.builder()
                    .level(null)
                    .levelKorean("미확인")
                    .detail(null)
                    .build();
        }
    }
}