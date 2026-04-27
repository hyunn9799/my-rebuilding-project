package com.aicc.silverlink.domain.user.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class UserRequests {
    public record UpdateMyProfileRequest(
            @NotBlank(message = "이름은 필수입니다.") @Size(max = 50) String name,
            @NotBlank(message = "전화번호는 필수입니다.") String phone, // 💡 추가됨
            @Email(message = "이메일 형식이 올바르지 않습니다.") @Size(max = 100) String email
    ) {}

    public record ChangeStatusRequest(
            @NotBlank(message = "상태값은 필수입니다.") String status
    ){}
}