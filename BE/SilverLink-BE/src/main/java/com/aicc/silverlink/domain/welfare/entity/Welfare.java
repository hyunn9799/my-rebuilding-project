package com.aicc.silverlink.domain.welfare.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "welfare_services") // SQL의 테이블명과 일치
@Getter
@NoArgsConstructor(access = AccessLevel.PUBLIC)
@Builder
@Setter
@AllArgsConstructor(access = AccessLevel.PROTECTED)
public class Welfare {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "welfare_service_id") // PK 컬럼명 명시
    private Long id;

    @Column(name = "serv_id", nullable = false, length = 50)
    private String servId;

    @Enumerated(EnumType.STRING)
    @Column(name = "source", nullable = false) // ENUM('CENTRAL', 'LOCAL')
    private Source source;

    @Column(name = "serv_nm", length = 255)
    private String servNm;

    @Column(name = "jur_mnof_nm", length = 255)
    private String jurMnofNm;

    @Column(name = "serv_dgst", columnDefinition = "TEXT")
    private String servDgst;

    @Column(name = "target_dtl_cn", columnDefinition = "TEXT")
    private String targetDtlCn;

    @Column(name = "slct_crit_cn", columnDefinition = "TEXT")
    private String slctCritCn;

    @Column(name = "alw_serv_cn", columnDefinition = "TEXT")
    private String alwServCn;

    @Column(name = "rprs_ctadr", length = 100)
    private String rprsCtadr;

    @Column(name = "serv_dtl_link", columnDefinition = "TEXT") // 필드명 dtl 확인
    private String servDtlLink;

    @Column(name = "district_code", length = 20)
    private String districtCode;

    @Column(name = "category", length = 50)
    private String category;

    @Builder.Default
    @Column(name = "is_active", nullable = false) // TINYINT(1) 매핑
    private boolean isActive = true;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
        // 별도 설정 없으면 기본값 true 유지
    }

    @PreUpdate
    protected void onUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}