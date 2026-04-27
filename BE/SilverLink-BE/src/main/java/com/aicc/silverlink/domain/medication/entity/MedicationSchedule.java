package com.aicc.silverlink.domain.medication.entity;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_schedules")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationSchedule {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private Elderly elderly;

    @Column(name = "medication_name", nullable = false, length = 200)
    private String medicationName;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "source_ocr_log_id")
    private MedicationOcrLog sourceOcrLog;

    @Column(name = "start_date", nullable = false)
    private LocalDate startDate;

    @Column(name = "end_date")
    private LocalDate endDate;

    @Enumerated(EnumType.STRING)
    @Column(name = "intake_timing", nullable = false)
    private IntakeTiming intakeTiming;

    @Column(name = "dosage_text", length = 100)
    private String dosageText;

    @Column(name = "is_active", nullable = false)
    private boolean isActive;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by_user_id", nullable = false)
    private User createdBy;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public MedicationSchedule(Elderly elderly, String medicationName, String dosageText,
            LocalDate startDate, LocalDate endDate, IntakeTiming intakeTiming,
            User createdBy, MedicationOcrLog sourceOcrLog) {
        this.elderly = elderly;
        this.medicationName = medicationName;
        this.dosageText = dosageText;
        this.startDate = startDate != null ? startDate : LocalDate.now();
        this.endDate = endDate;
        this.intakeTiming = intakeTiming != null ? intakeTiming : IntakeTiming.ANYTIME;
        this.createdBy = createdBy;
        this.sourceOcrLog = sourceOcrLog;
        this.isActive = true;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.intakeTiming == null) {
            this.intakeTiming = IntakeTiming.ANYTIME;
        }
        if (!this.isActive) {
            this.isActive = true;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    /**
     * 복약 일정 비활성화 (논리적 삭제)
     */
    public void deactivate() {
        this.isActive = false;
    }

    /**
     * 알림 토글
     */
    public void toggleActive() {
        this.isActive = !this.isActive;
    }

    /**
     * 복약 정보 수정
     */
    public void update(String medicationName, String dosageText, LocalDate endDate) {
        if (medicationName != null)
            this.medicationName = medicationName;
        if (dosageText != null)
            this.dosageText = dosageText;
        this.endDate = endDate;
    }

    public enum IntakeTiming {
        BEFORE_MEAL, AFTER_MEAL, ANYTIME
    }
}
