package com.aicc.silverlink.domain.admin.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * 관리자 정보 수정 요청 DTO
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
public class AdminUpdateRequest {

    @NotNull(message = "담당 행정구역 코드는 필수입니다.")
    private Long admCode;
}