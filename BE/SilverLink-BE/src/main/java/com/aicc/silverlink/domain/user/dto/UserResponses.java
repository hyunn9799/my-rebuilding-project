package com.aicc.silverlink.domain.user.dto;

import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class UserResponses {

    public record MyProfileResponse(
            Long id,
            String loginId,
            Role role,
            UserStatus status,
            String name,
            String phone,
            String email,
            boolean phoneVerified,
            LocalDateTime phoneVerifiedAt,
            LocalDateTime lastLoginAt,
            LocalDateTime createdAt
    ) {
        public static MyProfileResponse from(User user){
            return new MyProfileResponse(
                    user.getId(),
                    user.getLoginId(),
                    user.getRole(),
                    user.getStatus(),
                    user.getName(),
                    user.getPhone(),
                    user.getEmail(),
                    user.isPhoneVerified(),
                    user.getPhoneVerifiedAt(),
                    user.getLastLoginAt(),
                    user.getCreatedAt()
            );
        }
    }
}
