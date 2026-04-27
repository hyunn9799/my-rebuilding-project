package com.aicc.silverlink.domain.medication.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.LocalDateTime;

@Entity
@Table(name = "medication_intake_logs")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class MedicationIntakeLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "intake_log_id")
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "schedule_id", nullable = false)
    private MedicationSchedule schedule;

    @Column(name = "intake_date", nullable = false)
    private LocalDate intakeDate;

    @Column(name = "intake_time")
    private LocalTime intakeTime;

    @Column(name = "taken", nullable = false)
    private boolean taken;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false)
    private Source source;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }

    public enum Source {
        ELDERLY, CALLBOT, COUNSELOR
    }
}
