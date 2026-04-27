package com.aicc.silverlink.domain.user.controller;

import com.aicc.silverlink.domain.user.dto.UserRequests;
import com.aicc.silverlink.domain.user.dto.UserResponses;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.service.UserCommandService;
import com.aicc.silverlink.global.util.SecurityUtils;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import io.swagger.v3.oas.annotations.tags.Tag;

@Tag(name = "사용자", description = "사용자 프로필 관리 API")
@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserCommandService userCommandService;

    @GetMapping("/me")
    public UserResponses.MyProfileResponse me() {
        Long userId = SecurityUtils.currentUserId();
        return userCommandService.getMyProfile(userId);
    }

    @PatchMapping("/me")
    public UserResponses.MyProfileResponse updateMe(@Valid @RequestBody UserRequests.UpdateMyProfileRequest req) {
        Long userId = SecurityUtils.currentUserId();
        return userCommandService.updateMyProfile(userId, req);
    }

    // 관리자
    @PreAuthorize("hasRole('ADMIN')")
    @PatchMapping("/{userId}/status")
    public void changeStatus(@PathVariable Long userId, @Valid @RequestBody UserRequests.ChangeStatusRequest req) {
        UserStatus status = UserStatus.valueOf(req.status());
        userCommandService.ChangeStatus(userId, status);
    }

}
