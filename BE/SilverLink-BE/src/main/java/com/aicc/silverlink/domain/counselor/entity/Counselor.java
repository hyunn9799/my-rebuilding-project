package com.aicc.silverlink.domain.counselor.entity;

import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Entity
@Table(name = "counselors")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Counselor {
    @Id
    @Column(name = "user_id")
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    @Column(name = "employee_no", length = 20)
    private String employeeNo;

    @Column(name = "department", length = 100)
    private String department;

    @Column(name = "office_phone", length = 20)
    private String officePhone;

    @Column(name = "joined_at")
    private LocalDate joinedAt;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "adm_code", nullable = false)
    private AdministrativeDivision administrativeDivision;

    public Long getAdmCode() {
        return administrativeDivision != null ? administrativeDivision.getAdmCode() : null;
    }

    /**
     * [수정] 상담사 정보 업데이트
     * 서비스 레이어의 요구에 맞춰 행정구역은 선택적으로 수정 가능하게 변경했습니다.
     */
    public void updateInfo(String department, String officePhone) {
        if (department != null && !department.isBlank()) {
            this.department = department;
        }
        if (officePhone != null && !officePhone.isBlank()) {
            this.officePhone = officePhone;
        }
    }

    /**
     * [추가] 관리자용 행정구역 변경 메서드
     */
    public void updateAdministrativeDivision(AdministrativeDivision administrativeDivision) {
        if (administrativeDivision != null) {
            this.administrativeDivision = administrativeDivision;
        }
    }

    public static Counselor create(User user, String employeeNo, String department,
                                   String officePhone, LocalDate joinedAt,
                                   AdministrativeDivision administrativeDivision) {
        if (user == null) throw new IllegalArgumentException("사용자 정보는 필수입니다.");
        if (administrativeDivision == null) throw new IllegalArgumentException("담당 행정구역은 필수입니다.");

        return Counselor.builder()
                .user(user)
                .employeeNo(employeeNo)
                .department(department)
                .officePhone(officePhone)
                .administrativeDivision(administrativeDivision)
                .joinedAt(joinedAt)
                .build();
    }
}