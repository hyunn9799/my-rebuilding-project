package com.aicc.silverlink.domain.call.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * LLM 모델 실행 기록 (CallBot 발화)
 * - CallBot이 어르신에게 한 질문/말을 저장
 */
@Entity
@Table(name = "llm_models",
        indexes = {
                @Index(name = "idx_models_call", columnList = "call_id, created_at")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class LlmModel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "model_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private CallRecord callRecord;

    /**
     * CallBot이 어르신에게 한 말 (프롬프트/발화)
     */
    @Column(nullable = false, columnDefinition = "TEXT")
    private String prompt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public LlmModel(CallRecord callRecord, String prompt) {
        this.callRecord = callRecord;
        this.prompt = prompt;
    }
}