package com.aicc.silverlink.domain.call.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 어르신 응답 엔티티
 * - 통화 중 어르신이 한 말 (STT 결과)
 */
@Entity
@Table(name = "elderly_responses", indexes = {
        @Index(name = "idx_elderly_responses_call_time", columnList = "call_id, responded_at"),
        @Index(name = "idx_elderly_responses_model_time", columnList = "model_id, responded_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class ElderlyResponse {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "response_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "model_id", nullable = true)
    private LlmModel llmModel;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private CallRecord callRecord;

    /**
     * 어르신이 한 말 (STT 결과)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "responded_at", nullable = false)
    private LocalDateTime respondedAt;

    /**
     * 위험 감지 여부
     */
    @Column(nullable = false)
    private boolean danger;

    /**
     * 위험 감지 사유
     */
    @Column(name = "danger_reason", columnDefinition = "TEXT")
    private String dangerReason;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        if (this.respondedAt == null) {
            this.respondedAt = LocalDateTime.now();
        }
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    @Builder
    public ElderlyResponse(LlmModel llmModel, CallRecord callRecord, String content,
            LocalDateTime respondedAt, boolean danger, String dangerReason) {
        this.llmModel = llmModel;
        this.callRecord = callRecord;
        this.content = content;
        this.respondedAt = respondedAt != null ? respondedAt : LocalDateTime.now();
        this.danger = danger;
        this.dangerReason = dangerReason;
    }

    /**
     * 위험 여부 확인
     */
    public boolean isDanger() {
        return this.danger;
    }
}