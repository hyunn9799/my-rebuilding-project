package com.aicc.silverlink.domain.user.service;

import com.aicc.silverlink.domain.user.dto.UserRequests;
import com.aicc.silverlink.domain.user.dto.UserResponses;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

// UserCommandService.java
@Service
@RequiredArgsConstructor
public class UserCommandService {

    private final UserRepository userRepo;

    @Cacheable(value = "users", key = "#userId", unless = "#result == null")
    @Transactional(readOnly = true)
    public UserResponses.MyProfileResponse getMyProfile(Long userId) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));
        return UserResponses.MyProfileResponse.from(user);
    }

    @CacheEvict(value = "users", key = "#userId")
    @Transactional
    public UserResponses.MyProfileResponse updateMyProfile(Long userId, UserRequests.UpdateMyProfileRequest req) {
        User user = userRepo.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        // 활성화된 유저만 수정 가능하도록 방어 로직 강화
        if (!user.isActive()) {
            throw new IllegalStateException("USER_NOT_ACTIVE_STATUS: " + user.getStatus());
        }

        // 💡 엔티티의 통합 수정 메서드 호출 (이름, 전화번호, 이메일 한 번에 처리)
        user.updateProfile(req.name(), req.phone(), req.email());

        return UserResponses.MyProfileResponse.from(user);
    }

    @Transactional
    public void ChangeStatus(Long targetUserId, UserStatus status) {
        User user = userRepo.findById(targetUserId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        switch (status) {
            case ACTIVE -> user.activate();
            case LOCKED -> user.suspend();
            case DELETED -> user.softDelete();
            default -> throw new IllegalArgumentException("INVALID_STATUS_TRANSITION");
        }
    }
}
