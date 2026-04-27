package com.aicc.silverlink.domain.elderly.entity;

import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * 통화 스케줄 변경 요청 엔티티
 */
@Entity
@Table(name = "schedule_change_request")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor
public class ScheduleChangeRequest {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private Elderly elderly;

    @Column(name = "requested_call_time", length = 5, nullable = false)
    private String requestedCallTime; // "HH:mm" 형식

    @Column(name = "requested_call_days", length = 30, nullable = false)
    private String requestedCallDays; // 콤마 구분 (예: "MON,WED,FRI")

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    @Builder.Default
    private RequestStatus status = RequestStatus.PENDING;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by")
    private User processedBy;

    @Column(name = "reject_reason", length = 500)
    private String rejectReason;

    public enum RequestStatus {
        PENDING, // 대기중
        APPROVED, // 승인됨
        REJECTED // 거절됨
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    /**
     * 요청 승인
     */
    public void approve(User processor) {
        this.status = RequestStatus.APPROVED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = processor;
    }

    /**
     * 요청 거절
     */
    public void reject(User processor, String reason) {
        this.status = RequestStatus.REJECTED;
        this.processedAt = LocalDateTime.now();
        this.processedBy = processor;
        this.rejectReason = reason;
    }

    /**
     * 정적 팩토리 메서드
     */
    public static ScheduleChangeRequest create(Elderly elderly, String time, String days) {
        return ScheduleChangeRequest.builder()
                .elderly(elderly)
                .requestedCallTime(time)
                .requestedCallDays(days)
                .status(RequestStatus.PENDING)
                .build();
    }
}
