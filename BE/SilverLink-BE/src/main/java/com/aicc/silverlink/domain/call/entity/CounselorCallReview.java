package com.aicc.silverlink.domain.call.entity;

import com.aicc.silverlink.domain.counselor.entity.Counselor;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 상담사가 CallBot 통화를 확인하고 남긴 코멘트
 * - 상담사가 통화를 확인했음을 체크하는 용도
 * - 보호자에게 통화에 대한 부연 설명을 제공
 */
@Entity
@Table(name = "counselor_call_reviews",
        uniqueConstraints = @UniqueConstraint(columnNames = {"call_id", "counselor_user_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class CounselorCallReview {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "review_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false)
    private CallRecord callRecord;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_user_id", nullable = false)
    private Counselor counselor;

    @Column(name = "reviewed_at", nullable = false)
    private LocalDateTime reviewedAt;

    @Column(name = "comment", columnDefinition = "TEXT")
    private String comment;

    @Column(name = "is_urgent", nullable = false)
    private boolean urgent;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        LocalDateTime now = LocalDateTime.now();
        if (this.reviewedAt == null) {
            this.reviewedAt = now;
        }
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 상담사 통화 리뷰 생성
     */
    public static CounselorCallReview create(CallRecord callRecord, Counselor counselor,
                                             String comment, boolean urgent) {
        if (callRecord == null) throw new IllegalArgumentException("통화 기록은 필수입니다.");
        if (counselor == null) throw new IllegalArgumentException("상담사 정보는 필수입니다.");

        return CounselorCallReview.builder()
                .callRecord(callRecord)
                .counselor(counselor)
                .reviewedAt(LocalDateTime.now())
                .comment(comment)
                .urgent(urgent)
                .build();
    }

    /**
     * 코멘트 수정
     */
    public void updateComment(String comment, boolean urgent) {
        this.comment = comment;
        this.urgent = urgent;
    }
}