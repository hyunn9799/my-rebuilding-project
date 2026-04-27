package com.aicc.silverlink.domain.admin.dto.response;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

/**
 * 관리자 정보 응답 DTO
 */
@Getter
@Builder
@AllArgsConstructor
public class AdminResponse {

    private Long userId;
    private String loginId;
    private String name;
    private String phone;
    private String email;
    private String status;

    // 행정구역 정보
    private Long admCode;
    private String sidoName;
    private String sigunguName;
    private String dongName;
    private String fullAddress;

    // 관리자 레벨 정보
    private AdminLevel adminLevel;
    private String adminLevelDescription;

    private LocalDateTime createdAt;
    private LocalDateTime lastLoginAt;

    /**
     * Entity를 DTO로 변환
     */
    public static AdminResponse from(Admin admin) {
        User user = admin.getUser();
        AdministrativeDivision division = admin.getAdministrativeDivision();

        return AdminResponse.builder()
                .userId(admin.getUserId())
                .loginId(user.getLoginId())
                .name(user.getName())
                .phone(user.getPhone())
                .email(user.getEmail())
                .status(user.getStatus().name())
                .admCode(admin.getAdmCode())
                .sidoName(division != null ? division.getSidoName() : null)
                .sigunguName(division != null ? division.getSigunguName() : null)
                .dongName(division != null ? division.getDongName() : null)
                .fullAddress(division != null ? division.getFullAddress() : null)
                .adminLevel(admin.getAdminLevel())
                .adminLevelDescription(admin.getAdminLevel().getDescription())
                .createdAt(user.getCreatedAt())
                .lastLoginAt(user.getLastLoginAt())
                .build();
    }
}