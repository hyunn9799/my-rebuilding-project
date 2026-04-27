package com.aicc.silverlink.domain.emergency.entity;

import com.aicc.silverlink.domain.call.entity.CallRecord;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * 긴급 알림 엔티티
 * CallBot에서 위험 감지 시 생성되는 알림
 */
@Entity
@Table(name = "emergency_alerts",
        indexes = {
                @Index(name = "idx_ea_elderly_time", columnList = "elderly_user_id, created_at DESC"),
                @Index(name = "idx_ea_status_severity", columnList = "status, severity, created_at DESC"),
                @Index(name = "idx_ea_counselor_status", columnList = "assigned_counselor_id, status")
        })
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class EmergencyAlert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "alert_id")
    private Long id;

    /**
     * 대상 어르신
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "elderly_user_id", nullable = false)
    private Elderly elderly;

    /**
     * 관련 통화 기록 (미응답의 경우 null 가능)
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "call_id")
    private CallRecord callRecord;

    /**
     * 위험도 (CRITICAL: 긴급, WARNING: 주의)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "severity", nullable = false, length = 20)
    private Severity severity;

    /**
     * 알림 유형 (HEALTH: 건강위험, MENTAL: 정서위험, NO_RESPONSE: 연속미응답)
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "alert_type", nullable = false, length = 20)
    private AlertType alertType;

    /**
     * 알림 제목
     */
    @Column(name = "title", nullable = false, length = 200)
    private String title;

    /**
     * 상세 내용 (STT 기반)
     */
    @Column(name = "description", nullable = false, columnDefinition = "TEXT")
    private String description;

    /**
     * 감지된 위험 키워드 목록 (JSON)
     */
    @Column(name = "danger_keywords", columnDefinition = "JSON")
    private String dangerKeywords;

    /**
     * 관련 STT 원문 내용
     */
    @Column(name = "related_stt_content", columnDefinition = "TEXT")
    private String relatedSttContent;

    /**
     * 처리 상태
     */
    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private AlertStatus status = AlertStatus.PENDING;

    /**
     * 담당 상담사
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "assigned_counselor_id")
    private Counselor assignedCounselor;

    /**
     * 처리한 사용자
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "processed_by_user_id")
    private User processedBy;

    /**
     * 처리 일시
     */
    @Column(name = "processed_at")
    private LocalDateTime processedAt;

    /**
     * 처리 메모
     */
    @Column(name = "resolution_note", columnDefinition = "TEXT")
    private String resolutionNote;

    /**
     * 생성 일시
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * 수정 일시
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * 알림 수신자 목록
     */
    @OneToMany(mappedBy = "emergencyAlert", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<EmergencyAlertRecipient> recipients = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        if (this.status == null) {
            this.status = AlertStatus.PENDING;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }

    // ========== Enum 정의 ==========

    /**
     * 위험도
     */
    public enum Severity {
        CRITICAL("긴급"),
        WARNING("주의");

        private final String description;

        Severity(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 알림 유형
     */
    public enum AlertType {
        HEALTH("건강위험"),
        MENTAL("정서위험"),
        NO_RESPONSE("연속미응답");

        private final String description;

        AlertType(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    /**
     * 처리 상태
     */
    public enum AlertStatus {
        PENDING("미처리"),
        IN_PROGRESS("처리중"),
        RESOLVED("처리완료"),
        ESCALATED("상위보고");

        private final String description;

        AlertStatus(String description) {
            this.description = description;
        }

        public String getDescription() {
            return description;
        }
    }

    // ========== 비즈니스 메서드 ==========

    /**
     * 처리 시작
     */
    public void startProcessing(User processor) {
        if (this.status != AlertStatus.PENDING) {
            throw new IllegalStateException("미처리 상태의 알림만 처리를 시작할 수 있습니다.");
        }
        this.status = AlertStatus.IN_PROGRESS;
        this.processedBy = processor;
    }

    /**
     * 처리 완료
     */
    public void resolve(User processor, String note) {
        this.status = AlertStatus.RESOLVED;
        this.processedBy = processor;
        this.processedAt = LocalDateTime.now();
        this.resolutionNote = note;
    }

    /**
     * 상위 보고
     */
    public void escalate(User processor, String note) {
        this.status = AlertStatus.ESCALATED;
        this.processedBy = processor;
        this.processedAt = LocalDateTime.now();
        this.resolutionNote = note;
    }

    /**
     * 수신자 추가
     */
    public void addRecipient(EmergencyAlertRecipient recipient) {
        this.recipients.add(recipient);
        recipient.setEmergencyAlert(this);
    }

    /**
     * 긴급 여부 확인
     */
    public boolean isCritical() {
        return this.severity == Severity.CRITICAL;
    }

    /**
     * 미처리 여부 확인
     */
    public boolean isPending() {
        return this.status == AlertStatus.PENDING;
    }
}
