package com.aicc.silverlink.domain.counseling.entity;

import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.global.common.entity.BaseTimeEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
@Table(name = "counseling_record")
public class CounselingRecord extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "counselor_id", nullable = false)
    private Counselor counselor;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_id", nullable = false)
    private Elderly elderly;

    @Column(nullable = false)
    private LocalDate counselingDate;

    @Column(nullable = false)
    private LocalTime counselingTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounselingType type;

    @Column(nullable = false)
    private String category;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String result;

    @Column(columnDefinition = "TEXT")
    private String followUp;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private CounselingStatus status;

    public void update(LocalDate counselingDate, LocalTime counselingTime, CounselingType type,
                       String category, String summary, String content, String result, String followUp, CounselingStatus status) {
        this.counselingDate = counselingDate;
        this.counselingTime = counselingTime;
        this.type = type;
        this.category = category;
        this.summary = summary;
        this.content = content;
        this.result = result;
        this.followUp = followUp;
        this.status = status;
    }

    public enum CounselingType {
        PHONE, VISIT, VIDEO
    }

    public enum CounselingStatus {
        COMPLETED, IN_PROGRESS, SCHEDULED
    }
}
