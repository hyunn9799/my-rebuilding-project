package com.aicc.silverlink.domain.guardian.dto;

import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.user.entity.User;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GuardianResponse {
    private Long id;
    private Long userId;
    private String name;
    private String email;
    private String phone;

    private String addressLine1;
    private String addressLine2;
    private String zipcode;

    private LocalDateTime createdAt;
    private int elderlyCount;
    private String elderlyName;

    public static GuardianResponse from(Guardian guardian) {
        return from(guardian, 0, null);
    }

    public static GuardianResponse from(Guardian guardian, int elderlyCount) {
        return from(guardian, elderlyCount, null);
    }

    public static GuardianResponse from(Guardian guardian, int elderlyCount, String elderlyName) {
        User user = guardian.getUser();

        return GuardianResponse.builder()
                .id(guardian.getId())
                .userId(user.getId())
                .name(user.getName())
                .email(user.getEmail())
                .phone(user.getPhone())
                .addressLine1(guardian.getAddressLine1())
                .addressLine2(guardian.getAddressLine2())
                .zipcode(guardian.getZipcode())
                .createdAt(user.getCreatedAt())
                .elderlyCount(elderlyCount)
                .elderlyName(elderlyName)
                .build();
    }
}
