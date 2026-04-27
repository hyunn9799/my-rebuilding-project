package com.aicc.silverlink.domain.call.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 통화별 일일 상태 엔티티
 * - 식사, 건강, 수면 상태 저장
 */
@Entity
@Table(name = "call_daily_status",
        indexes = {
                @Index(name = "idx_daily_status_call", columnList = "call_id")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CallDailyStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "status_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id", nullable = false, unique = true)
    private CallRecord callRecord;

    /**
     * 식사 여부 (직전 식사를 했는지)
     */
    @Column(name = "meal_taken")
    private Boolean mealTaken;

    /**
     * 건강 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "health_status", length = 20)
    private StatusLevel healthStatus;

    /**
     * 건강 상태 상세 내용
     */
    @Column(name = "health_detail", columnDefinition = "TEXT")
    private String healthDetail;

    /**
     * 수면 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_status", length = 20)
    private StatusLevel sleepStatus;

    /**
     * 수면 상태 상세 내용
     */
    @Column(name = "sleep_detail", columnDefinition = "TEXT")
    private String sleepDetail;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    @Builder
    public CallDailyStatus(CallRecord callRecord, Boolean mealTaken,
                           StatusLevel healthStatus, String healthDetail,
                           StatusLevel sleepStatus, String sleepDetail) {
        this.callRecord = callRecord;
        this.mealTaken = mealTaken;
        this.healthStatus = healthStatus;
        this.healthDetail = healthDetail;
        this.sleepStatus = sleepStatus;
        this.sleepDetail = sleepDetail;
    }

    /**
     * 상태 레벨 Enum
     */
    public enum StatusLevel {
        GOOD("좋음"),
        NORMAL("보통"),
        BAD("나쁨");

        private final String korean;

        StatusLevel(String korean) {
            this.korean = korean;
        }

        public String getKorean() {
            return korean;
        }
    }

    // ===== 비즈니스 메서드 =====

    public String getHealthStatusKorean() {
        return healthStatus != null ? healthStatus.getKorean() : "미확인";
    }

    public String getSleepStatusKorean() {
        return sleepStatus != null ? sleepStatus.getKorean() : "미확인";
    }

    public String getMealStatusKorean() {
        if (mealTaken == null) return "미확인";
        return mealTaken ? "식사함" : "식사 안함";
    }
}