package com.aicc.silverlink.domain.elderly.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 통화 스케줄 변경 이력 엔티티
 * - 모든 스케줄 변경(직접 수정, 요청 승인/거절)을 기록
 */
@Entity
@Table(name = "call_schedule_history", indexes = {
        @Index(name = "idx_history_elderly", columnList = "elderly_user_id"),
        @Index(name = "idx_history_changed_by", columnList = "changed_by"),
        @Index(name = "idx_history_created_at", columnList = "created_at")
})
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class CallScheduleHistory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private Elderly elderly;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "changed_by", nullable = false)
    private User changedBy; // 변경자 (상담사 또는 관리자)

    @Enumerated(EnumType.STRING)
    @Column(name = "change_type", nullable = false, length = 30)
    private ChangeType changeType;

    // 이전 값
    @Column(name = "previous_time", length = 5)
    private String previousTime;

    @Column(name = "previous_days", length = 30)
    private String previousDays;

    @Column(name = "previous_enabled")
    private Boolean previousEnabled;

    // 새 값
    @Column(name = "new_time", length = 5)
    private String newTime;

    @Column(name = "new_days", length = 30)
    private String newDays;

    @Column(name = "new_enabled")
    private Boolean newEnabled;

    // 변경 사유
    @Column(name = "change_reason", length = 500)
    private String changeReason;

    // 관련 변경 요청 ID (요청 승인/거절인 경우)
    @Column(name = "related_request_id")
    private Long relatedRequestId;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 변경 유형
     */
    public enum ChangeType {
        DIRECT_UPDATE("직접 수정"), // 상담사/관리자가 직접 수정 (구두 요청 등)
        REQUEST_APPROVED("요청 승인"), // 어르신 요청 승인
        REQUEST_REJECTED("요청 거절"), // 어르신 요청 거절
        ELDERLY_SELF_UPDATE("본인 수정"); // 어르신 본인 수정

        private final String description;

        ChangeType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 직접 수정 이력 생성
     */
    public static CallScheduleHistory createDirectUpdate(
            Elderly elderly,
            User changedBy,
            String previousTime, String previousDays, Boolean previousEnabled,
            String newTime, String newDays, Boolean newEnabled,
            String reason) {

        return CallScheduleHistory.builder()
                .elderly(elderly)
                .changedBy(changedBy)
                .changeType(ChangeType.DIRECT_UPDATE)
                .previousTime(previousTime)
                .previousDays(previousDays)
                .previousEnabled(previousEnabled)
                .newTime(newTime)
                .newDays(newDays)
                .newEnabled(newEnabled)
                .changeReason(reason)
                .build();
    }

    /**
     * 요청 승인 이력 생성
     */
    public static CallScheduleHistory createRequestApproved(
            Elderly elderly,
            User changedBy,
            Long requestId,
            String previousTime, String previousDays, Boolean previousEnabled,
            String newTime, String newDays) {

        return CallScheduleHistory.builder()
                .elderly(elderly)
                .changedBy(changedBy)
                .changeType(ChangeType.REQUEST_APPROVED)
                .previousTime(previousTime)
                .previousDays(previousDays)
                .previousEnabled(previousEnabled)
                .newTime(newTime)
                .newDays(newDays)
                .newEnabled(true)
                .relatedRequestId(requestId)
                .build();
    }

    /**
     * 요청 거절 이력 생성
     */
    public static CallScheduleHistory createRequestRejected(
            Elderly elderly,
            User changedBy,
            Long requestId,
            String reason) {

        return CallScheduleHistory.builder()
                .elderly(elderly)
                .changedBy(changedBy)
                .changeType(ChangeType.REQUEST_REJECTED)
                .relatedRequestId(requestId)
                .changeReason(reason)
                .build();
    }
}
