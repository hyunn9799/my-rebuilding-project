package com.aicc.silverlink.domain.call.entity;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 통화 기록 엔티티
 */
@Entity
@Table(name = "call_records",
        indexes = {
                @Index(name = "idx_call_records_elderly_time", columnList = "elderly_user_id, call_at"),
                @Index(name = "idx_call_records_state_time", columnList = "state, call_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "call_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private Elderly elderly;

    @Column(name = "call_at", nullable = false)
    private LocalDateTime callAt;

    @Column(name = "call_time_sec", nullable = false)
    private Integer callTimeSec;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CallState state;

    /**
     * 통화 녹음 파일 URL (S3)
     */
    @Column(name = "recording_url", length = 500)
    private String recordingUrl;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    // ===== 연관 관계 =====

    @OneToMany(mappedBy = "callRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt ASC")
    private List<LlmModel> llmModels = new ArrayList<>();

    @OneToMany(mappedBy = "callRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("respondedAt ASC")
    private List<ElderlyResponse> elderlyResponses = new ArrayList<>();

    @OneToMany(mappedBy = "callRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<CallSummary> summaries = new ArrayList<>();

    @OneToMany(mappedBy = "callRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    @OrderBy("createdAt DESC")
    private List<CallEmotion> emotions = new ArrayList<>();

    /**
     * 일일 상태 (1:1 관계)
     */
    @OneToOne(mappedBy = "callRecord", cascade = CascadeType.ALL, orphanRemoval = true)
    private CallDailyStatus dailyStatus;

    @Builder
    public CallRecord(Elderly elderly, LocalDateTime callAt, Integer callTimeSec,
                      CallState state, String recordingUrl) {
        this.elderly = elderly;
        this.callAt = callAt;
        this.callTimeSec = callTimeSec;
        this.state = state;
        this.recordingUrl = recordingUrl;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }


    // ===== 비즈니스 메서드 =====

    /**
     * 통화 시간을 "MM분 SS초" 형식으로 반환
     */
    public String getFormattedDuration() {
        if (callTimeSec == null || callTimeSec <= 0) {
            return "0분 0초";
        }
        int minutes = callTimeSec / 60;
        int seconds = callTimeSec % 60;

        if (minutes == 0) {
            return seconds + "초";
        } else if (seconds == 0) {
            return minutes + "분";
        }
        return minutes + "분 " + seconds + "초";
    }

    /**
     * 위험 응답이 있는지 확인
     */
    public boolean hasDangerResponse() {
        return elderlyResponses.stream()
                .anyMatch(ElderlyResponse::isDanger);
    }

    /**
     * 최신 감정 분석 결과
     */
    public CallEmotion getLatestEmotion() {
        return emotions.isEmpty() ? null : emotions.get(0);
    }

    /**
     * 최신 통화 요약
     */
    public CallSummary getLatestSummary() {
        return summaries.isEmpty() ? null : summaries.get(0);
    }

    /**
     * 통화 완료 여부
     */
    public boolean isCompleted() {
        return this.state == CallState.COMPLETED;
    }

    /**
     * 녹음 파일 URL 설정
     */
    public void setRecordingUrl(String recordingUrl) {
        this.recordingUrl = recordingUrl;
    }

    /**
     * 통화 시간 설정
     */
    public void setCallTimeSec(Integer callTimeSec) {
        this.callTimeSec = callTimeSec;
    }

    /**
     * 통화 상태 변경
     */
    public void updateState(CallState state) {
        this.state = state;
    }

    /**
     * 일일 상태 설정
     */
    public void setDailyStatus(CallDailyStatus dailyStatus) {
        this.dailyStatus = dailyStatus;
    }
}