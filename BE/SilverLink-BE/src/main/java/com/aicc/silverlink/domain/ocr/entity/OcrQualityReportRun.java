package com.aicc.silverlink.domain.ocr.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "ocr_quality_report_runs")
@Getter
@Builder
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
public class OcrQualityReportRun {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "run_id")
    private Long id;

    @Column(name = "action_type", nullable = false, length = 50)
    private String actionType;

    @Column(name = "actor_user_id")
    private Long actorUserId;

    @Column(name = "limit_value")
    private Integer limit;

    @Column(name = "success", nullable = false)
    private Boolean success;

    @Column(name = "matched_count")
    private Integer matchedCount;

    @Column(name = "pending_review_count")
    private Integer pendingReviewCount;

    @Column(name = "alias_candidate_count")
    private Integer aliasCandidateCount;

    @Column(name = "manual_review_count")
    private Integer manualReviewCount;

    @Column(name = "normalization_candidate_count")
    private Integer normalizationCandidateCount;

    @Column(name = "candidate_count")
    private Integer candidateCount;

    @Column(name = "upserted_count")
    private Integer upsertedCount;

    @Column(name = "skipped_count")
    private Integer skippedCount;

    @Column(name = "message", length = 1000)
    private String message;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
