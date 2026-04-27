package com.aicc.silverlink.domain.counselor.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CounselorUpdateRequest {
    // 본인이 수정 가능한 필드들
    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String email;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    private String department;
    private String officePhone;
}