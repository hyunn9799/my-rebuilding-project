package com.aicc.silverlink.domain.guardian.controller;

import com.aicc.silverlink.domain.guardian.dto.*;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.service.GuardianService;
import com.aicc.silverlink.domain.user.entity.Role;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("ci")
class GuardianControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @MockitoBean
    private GuardianService guardianService;

    // --- 테스트용 픽스처 생성기 ---
    private GuardianResponse createGuardianResponse(Long id, String name) {
        return GuardianResponse.builder()
                .id(id).name(name).email("test@silver.com").phone("01012345678")
                .addressLine1("서울시").addressLine2("강남구").zipcode("12345").build();
    }

    private GuardianElderlyResponse createElderlyResponse() {
        return GuardianElderlyResponse.builder()
                .id(1L).guardianId(1L).guardianName("김보호")
                .elderlyId(2L).elderlyName("이노인")
                .relationType(RelationType.CHILD).connectedAt(LocalDateTime.now()).build();
    }

    private void mockAuthentication(Long userId, String role) {
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(
                userId, null, Collections.singletonList(new SimpleGrantedAuthority("ROLE_" + role)));
        SecurityContextHolder.getContext().setAuthentication(auth);
    }

    @Nested
    @DisplayName("회원가입 및 본인 계정 관리 API")
    class GuardianSelfManagementTests {

        @Test
        @DisplayName("성공: 보호자 회원가입")
        void signup_Success() throws Exception {
            GuardianRequest request = GuardianRequest.builder().loginId("guardian01").name("김보호").password("pass123")
                    .build();
            given(guardianService.register(any())).willReturn(createGuardianResponse(1L, "김보호"));

            mockMvc.perform(post("/api/guardians/signup").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isCreated())
                    .andExpect(header().string("Location", "/api/guardians/1"));
        }

        @Test
        @DisplayName("성공: 내 정보 수정")
        void updateMe_Success() throws Exception {
            Long myId = 1L;
            mockAuthentication(myId, "GUARDIAN");
            GuardianUpdateRequest updateReq = new GuardianUpdateRequest("수정이름", "01099998888", "new@test.com", "서울",
                    "강남", "111");
            given(guardianService.updateGuardianProfile(eq(myId), any()))
                    .willReturn(createGuardianResponse(myId, "수정이름"));

            mockMvc.perform(put("/api/guardians/me").with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(updateReq)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("수정이름"));
        }

        @Test
        @DisplayName("성공: 회원 탈퇴")
        void withdraw_Success() throws Exception {
            Long myId = 1L;
            mockAuthentication(myId, "GUARDIAN");

            mockMvc.perform(delete("/api/guardians/me").with(csrf()))
                    .andExpect(status().isNoContent());
        }
    }

    @Nested
    @DisplayName("상담사 및 관리자 전용 조회 API")
    class AdminCounselorTests {

        @Test
        @DisplayName("성공: 상담사가 담당 어르신의 보호자 조회")
        void getGuardianByCounselor_Success() throws Exception {
            Long counselorId = 10L;
            Long guardianId = 1L;
            mockAuthentication(counselorId, "COUNSELOR");
            given(guardianService.getGuardianForCounselor(guardianId, counselorId))
                    .willReturn(createGuardianResponse(guardianId, "김보호"));

            mockMvc.perform(get("/api/guardians/counselor/{id}", guardianId))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("김보호"));
        }

        @Test
        @DisplayName("성공: 관리자가 어르신 ID로 보호자 조회")
        @Disabled("TODO: Fix mock setup for CI environment")
        void getGuardianByElderlyAdmin_Success() throws Exception {
            mockAuthentication(999L, "ADMIN");
            given(guardianService.getGuardianByElderly(2L)).willReturn(createGuardianResponse(1L, "김보호"));

            mockMvc.perform(get("/api/guardians/admin/elderly/2"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.name").value("김보호"));
        }

        @Test
        @DisplayName("성공: 관리자가 어르신-보호자 관계 생성")
        void connectElderly_Success() throws Exception {
            mockAuthentication(999L, "ADMIN");

            mockMvc.perform(post("/api/guardians/1/connect")
                            .param("elderlyId", "2")
                            .param("relationType", "CHILD")
                            .with(csrf()))
                    .andExpect(status().isOk());
        }

        @Test
        @DisplayName("성공: 관리자가 전체 목록 조회")
        void getAllGuardians_Success() throws Exception {
            mockAuthentication(999L, "ADMIN");
            given(guardianService.getAllGuardian()).willReturn(List.of(createGuardianResponse(1L, "김보호")));

            mockMvc.perform(get("/api/guardians"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.size()").value(1));
        }
    }
}