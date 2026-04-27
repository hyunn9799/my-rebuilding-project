package com.aicc.silverlink.domain.admin.dto.request;

import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 생성 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminCreateRequest {

    @NotNull(message = "사용자 ID는 필수입니다.")
    private Long userId;

    @NotNull(message = "담당 행정구역 코드는 필수입니다.")
    private Long admCode;

    /**
     * 관리자 레벨 (선택사항)
     * null이면 행정구역 코드로 자동 결정
     * (entity.Admin 참조)
     */
    private AdminLevel adminLevel;
}