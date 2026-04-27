package com.aicc.silverlink.domain.policy.dto;

import com.aicc.silverlink.domain.policy.entity.Policy;
import com.aicc.silverlink.domain.policy.entity.PolicyType;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class PolicyResponse {

    private Long id;
    private PolicyType policyType;
    private String policyName;
    private String version;
    private String content;
    private boolean isMandatory;
    private String description; // 💡 추가
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt; // 💡 추가: 수정일 확인용

    public static PolicyResponse from(Policy policy) {
        return PolicyResponse.builder()
                .id(policy.getId())
                .policyType(policy.getPolicyType())
                .policyName(policy.getPolicyType().getDescription())
                .version(policy.getVersion())
                .content(policy.getContent())
                .isMandatory(policy.isMandatory())
                .description(policy.getDescription()) // 💡 추가
                .createdAt(policy.getCreatedAt())
                .updatedAt(policy.getUpdatedAt()) // 💡 추가
                .build();
    }
}