package com.aicc.silverlink.domain.counselor.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@AllArgsConstructor
@Builder
@Getter
@NoArgsConstructor
public class CounselorRequest {

    @NotBlank(message = "아이디는 필수입니다.")
    private String loginId;

    @NotBlank(message = "비밀번호는 필수입니다.")
    private String password;

    @NotBlank(message = "이름은 필수입니다.")
    private String name;

    private String email;

    @NotBlank(message = "전화번호는 필수입니다.")
    private String phone;

    private String employeeNo;

    private String department;
    private String officePhone;
    private LocalDate joinedAt;

    @NotNull(message = "담당 행정구역 코드는 필수입니다.")
    private Long admCode;
}