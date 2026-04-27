package com.aicc.silverlink.domain.call.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 감정 분석 결과 엔티티
 */
@Entity
@Table(name = "call_emotions",
        indexes = {
                @Index(name = "idx_emotion_call_time", columnList = "call_id, created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallEmotion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "emotion_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private CallRecord callRecord;

    /**
     * 감정 레벨
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "emotion_level", nullable = false)
    private EmotionLevel emotionLevel;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public CallEmotion(CallRecord callRecord, EmotionLevel emotionLevel) {
        this.callRecord = callRecord;
        this.emotionLevel = emotionLevel;
    }

    /**
     * 감정 레벨 한글명 반환
     */
    public String getEmotionLevelKorean() {
        return emotionLevel != null ? emotionLevel.getKorean() : "미확인";
    }
}