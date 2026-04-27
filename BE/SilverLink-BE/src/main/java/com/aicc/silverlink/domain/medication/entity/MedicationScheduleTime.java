package com.aicc.silverlink.domain.medication.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_schedule_times")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationScheduleTime {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "schedule_time_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MedicationSchedule schedule;

    @Column(name = "dose_seq", nullable = false)
    private Short doseSeq;

    @Column(name = "intake_time", nullable = false)
    private LocalTime intakeTime;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public MedicationScheduleTime(MedicationSchedule schedule, Short doseSeq, LocalTime intakeTime) {
        this.schedule = schedule;
        this.doseSeq = doseSeq != null ? doseSeq : 1;
        this.intakeTime = intakeTime;
    }

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
