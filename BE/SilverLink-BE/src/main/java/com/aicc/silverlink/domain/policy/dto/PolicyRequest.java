package com.aicc.silverlink.domain.policy.dto;

import com.aicc.silverlink.domain.policy.entity.Policy;
import com.aicc.silverlink.domain.policy.entity.PolicyType;
import com.aicc.silverlink.domain.user.entity.User;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class PolicyRequest {

    @NotNull(message = "정책 종류는 필수입니다.")
    private PolicyType policyType;

    @NotBlank(message = "버전은 필수입니다.")
    private String version;

    @NotBlank(message = "내용은 필수입니다.")
    private String content;

    @NotNull(message = "필수 동의 여부는 필수입니다.")
    private Boolean isMandatory;

    // 💡 추가: 약관에 대한 부가 설명 (선택 사항)
    private String description;

    public Policy toEntity(User user) {
        // 💡 Policy.create 메서드에 description도 전달하도록 수정
        return Policy.create(this.policyType, this.version, this.content, this.isMandatory, this.description, user);
    }
}