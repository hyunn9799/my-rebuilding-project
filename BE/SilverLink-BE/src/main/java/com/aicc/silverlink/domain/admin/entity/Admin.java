package com.aicc.silverlink.domain.admin.entity;

import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

/**
 * 관리자 엔티티
 * User 테이블과 1:1 관계
 * AdministrativeDivision과 N:1 관계
 */


@Entity
@Table(name = "admin")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Admin {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // 담당 행정 구역 - AdministrativeDivision과 FK 관계
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adm_code", nullable = false)
    private AdministrativeDivision administrativeDivision;

    // 관리자 레벨
    @Enumerated(EnumType.STRING)
    @Column(name = "admin_level", nullable = false)
    private AdminLevel adminLevel;



    @Builder
    public Admin(User user, AdministrativeDivision administrativeDivision, AdminLevel adminLevel) {
        this.user = user;
        this.administrativeDivision = administrativeDivision;
        this.adminLevel = adminLevel != null ? adminLevel : determineAdminLevel(administrativeDivision);

    }

    /**
     * 행정구역 레벨로 관리자 레벨 자동 결정
     */
    private static AdminLevel determineAdminLevel(AdministrativeDivision division) {
        if (division == null) {
            return AdminLevel.DISTRICT;
        }

        return switch (division.getLevel()) {
            case SIDO -> AdminLevel.PROVINCIAL;
            case SIGUNGU -> AdminLevel.CITY;
            case DONG -> AdminLevel.DISTRICT;
        };
    }

    /**
     * 행정동 코드로 관리자 레벨 자동 결정 (하위 호환용)
     */
    private static AdminLevel determineAdminLevelByCode(Long admCode) {
        if (admCode == null) {
            return AdminLevel.DISTRICT;
        }

        String code = String.format("%010d", admCode);

        // 전국 관리자 (00 0000 000 00)
        if (admCode == 0L) {
            return AdminLevel.NATIONAL;
        }

        // 시/도 레벨 (XX 0000 000 00)
        if (code.substring(2).equals("00000000")) {
            return AdminLevel.PROVINCIAL;
        }

        // 시/군/구 레벨 (XX XX00 000 00)
        if (code.substring(4).equals("000000")) {
            return AdminLevel.CITY;
        }

        // 읍/면/동 레벨
        return AdminLevel.DISTRICT;
    }

    /**
     * 관리자 레벨 Enum
     */
    public enum AdminLevel {
        NATIONAL("전국", 0),      // 중앙 관리자
        PROVINCIAL("시/도", 2),   // 서울특별시, 경기도 등
        CITY("시/군/구", 4),      // 강남구, 수원시 등
        DISTRICT("읍/면/동", 7);  // 역삼동 등

        private final String description;
        private final int codeLength;  // 유효한 코드 길이

        AdminLevel(String description, int codeLength) {
            this.description = description;
            this.codeLength = codeLength;
        }

        public String getDescription() {
            return description;
        }

        public int getCodeLength() {
            return codeLength;
        }
    }

    /**
     * 편의 메서드: 행정구역 코드 반환
     */
    public Long getAdmCode() {
        return administrativeDivision != null ? administrativeDivision.getAdmCode() : null;
    }

    /**
     * 비즈니스 로직: 담당 구역 변경
     */
    public void updateAdministrativeDivision(AdministrativeDivision newDivision) {
        this.administrativeDivision = newDivision;
        this.adminLevel = determineAdminLevel(newDivision);
    }

    /**
     * 특정 행정구역이 이 관리자의 관할 구역인지 확인
     */
    public boolean hasJurisdiction(Long targetAdmCode) {
        if (targetAdmCode == null) {
            return false;
        }

        // 전국 관리자는 모든 구역 관할
        if (this.adminLevel == AdminLevel.NATIONAL) {
            return true;
        }

        Long myAdmCode = getAdmCode();
        if (myAdmCode == null) {
            return false;
        }

        String myCode = String.format("%010d", myAdmCode);
        String targetCode = String.format("%010d", targetAdmCode);

        // 내 코드 길이만큼 비교
        int compareLength = this.adminLevel.getCodeLength();
        if (compareLength == 0) {
            return true;
        }

        return myCode.substring(0, compareLength)
                .equals(targetCode.substring(0, compareLength));
    }

    /**
     * 특정 행정구역이 이 관리자의 관할 구역인지 확인 (AdministrativeDivision으로)
     */
    public boolean hasJurisdiction(AdministrativeDivision targetDivision) {
        if (targetDivision == null) {
            return false;
        }
        return hasJurisdiction(targetDivision.getAdmCode());
    }

    /**
     * 다른 관리자가 내 상위 관리자인지 확인
     */
    public boolean isSubordinateOf(Admin other) {
        return other.hasJurisdiction(this.getAdmCode())
                && this.adminLevel.ordinal() > other.adminLevel.ordinal();
    }

    /**
     * 다른 관리자보다 상위 레벨인지 확인
     */
    public boolean hasHigherLevelThan(Admin other) {
        return this.adminLevel.ordinal() < other.adminLevel.ordinal();
    }
}