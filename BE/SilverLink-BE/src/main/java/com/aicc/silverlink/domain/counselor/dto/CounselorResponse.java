package com.aicc.silverlink.domain.counselor.dto;

import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class CounselorResponse {

    private Long id;
    private Long userId;

    private String name;
    private String loginId;
    private String email;
    private String phone;

    private String employeeNo;
    private String department;

    private UserStatus status;
    private String officePhone;

    // 행정구역 정보
    private Long admCode;
    private String sidoName;
    private String sigunguName;
    private String dongName;
    private String fullAddress;

    // 추가 필드
    private LocalDateTime createdAt;
    private int assignedElderlyCount;

    public static CounselorResponse from(Counselor counselor) {
        return from(counselor, 0);
    }

    public static CounselorResponse from(Counselor counselor, int assignedElderlyCount) {
        User user = counselor.getUser();
        AdministrativeDivision division = counselor.getAdministrativeDivision();

        return CounselorResponse.builder()
                .id(counselor.getId())
                .userId(user.getId())
                .name(user.getName())
                .loginId(user.getLoginId())
                .email(user.getEmail())
                .phone(user.getPhone())
                .status(user.getStatus())
                .department(counselor.getDepartment())
                .employeeNo(counselor.getEmployeeNo())
                .officePhone(counselor.getOfficePhone())
                .admCode(counselor.getAdmCode())
                .sidoName(division != null ? division.getSidoName() : null)
                .sigunguName(division != null ? division.getSigunguName() : null)
                .dongName(division != null ? division.getDongName() : null)
                .fullAddress(division != null ? division.getFullAddress() : null)
                .createdAt(user.getCreatedAt())
                .assignedElderlyCount(assignedElderlyCount)
                .build();
    }
}
