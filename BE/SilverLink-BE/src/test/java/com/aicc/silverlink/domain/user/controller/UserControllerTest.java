package com.aicc.silverlink.domain.user.controller;

import com.aicc.silverlink.domain.user.dto.UserRequests;
import com.aicc.silverlink.domain.user.dto.UserResponses;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.service.UserCommandService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
@Disabled("TODO: Fix authentication principal type mismatch in CI environment")
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private UserCommandService userCommandService;

    @Test
    @DisplayName("내 프로필 조회 성공")
    void me_Success() throws Exception {
        UserResponses.MyProfileResponse response = new UserResponses.MyProfileResponse(
                1L, "testUser", Role.ELDERLY, UserStatus.ACTIVE, "홍길동", "01012345678",
                "test@example.com", true, LocalDateTime.now(), LocalDateTime.now(),
                LocalDateTime.now());

        given(userCommandService.getMyProfile(any())).willReturn(response);

        mockMvc.perform(get("/api/users/me")
                        .with(user("1").roles("USER"))) // Principal ID를 1로 인식하게 함
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("홍길동"));
    }

    @Test
    @DisplayName("내 프로필 수정 성공 - 전화번호 필드 포함")
    void updateMe_Success() throws Exception {
        UserRequests.UpdateMyProfileRequest request = new UserRequests.UpdateMyProfileRequest("김철수",
                "01099998888", "new@example.com");
        UserResponses.MyProfileResponse response = new UserResponses.MyProfileResponse(
                1L, "testUser", Role.ELDERLY, UserStatus.ACTIVE, "김철수", "01099998888",
                "new@example.com", true, LocalDateTime.now(), LocalDateTime.now(), LocalDateTime.now());

        given(userCommandService.updateMyProfile(any(), any())).willReturn(response);

        mockMvc.perform(patch("/api/users/me")
                        .with(user("1").roles("USER"))
                        .with(csrf()) // Patch 요청 시 CSRF 필수
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.phone").value("01099998888"));
    }

    @Test
    @DisplayName("회원 상태 변경 성공 - 관리자 권한")
    void changeStatus_Success() throws Exception {
        Long targetUserId = 2L;
        UserRequests.ChangeStatusRequest request = new UserRequests.ChangeStatusRequest("LOCKED");

        mockMvc.perform(patch("/api/users/{userId}/status", targetUserId)
                        .with(user("1").roles("ADMIN"))
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        verify(userCommandService).ChangeStatus(eq(targetUserId), eq(UserStatus.LOCKED));
    }

    @Test
    @DisplayName("회원 상태 변경 실패 - 권한 없음")
    void changeStatus_Fail_Forbidden() throws Exception {
        Long targetUserId = 2L;
        UserRequests.ChangeStatusRequest request = new UserRequests.ChangeStatusRequest("LOCKED");

        mockMvc.perform(patch("/api/users/{userId}/status", targetUserId)
                        .with(user("1").roles("USER")) // ADMIN이 아닌 일반 USER 권한
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }
}