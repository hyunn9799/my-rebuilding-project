package com.aicc.silverlink.domain.system.entity;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "administrative_division")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class AdministrativeDivision {

    @Id
    @Column(name = "adm_code")
    private Long admCode;

    @Column(name = "sido_code", nullable = false, length = 2)
    private String sidoCode;

    @Column(name = "sigungu_code", length = 3)
    private String sigunguCode;

    @Column(name = "dong_code", length = 3)
    private String dongCode;

    @Column(name = "sido_name", nullable = false, length = 20)
    private String sidoName;

    @Column(name = "sigungu_name", length = 20)
    private String sigunguName;

    @Column(name = "dong_name", length = 20)
    private String dongName;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private DivisionLevel level;

    @Column(name = "is_active", nullable = false)
    private Boolean isActive = true;

    /**
     * 행정구역 생성일 (해당 구역이 생성된 날짜)
     */
    @Column(name = "created_at", nullable = false, updatable = false)

    private LocalDateTime createdAt;

    @Column(name = "established_at")
    private LocalDate establishedAt;

    /**
     * 행정구역 말소일 (해당 구역이 폐지된 날짜)
     */
    @Column(name = "abolished_at")
    private LocalDate abolishedAt;




    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    public enum DivisionLevel {
        SIDO, SIGUNGU, DONG
    }

    @Builder
    public AdministrativeDivision(Long admCode, String sidoCode, String sigunguCode,
                                  String dongCode, String sidoName, String sigunguName,
                                  String dongName, DivisionLevel level,
                                  LocalDate establishedAt, LocalDate abolishedAt) {
        this.admCode = admCode;
        this.sidoCode = sidoCode;
        this.sigunguCode = sigunguCode;
        this.dongCode = dongCode;
        this.sidoName = sidoName;
        this.sigunguName = sigunguName;
        this.dongName = dongName;
        this.level = level;
        this.establishedAt = establishedAt;
        this.abolishedAt = abolishedAt;
        this.isActive = (abolishedAt == null);
    }

    public String getFullAddress() {
        StringBuilder sb = new StringBuilder();
        if (sidoName != null) sb.append(sidoName);
        if (sigunguName != null) sb.append(" ").append(sigunguName);
        if (dongName != null) sb.append(" ").append(dongName);
        return sb.toString().trim();
    }

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}