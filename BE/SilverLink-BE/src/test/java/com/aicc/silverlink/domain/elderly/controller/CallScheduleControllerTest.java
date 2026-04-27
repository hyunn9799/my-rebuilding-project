package com.aicc.silverlink.domain.elderly.controller;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.*;
import com.aicc.silverlink.domain.elderly.service.CallScheduleService;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.global.security.principal.UserPrincipal;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
@DisplayName("CallScheduleController 테스트")
class CallScheduleControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CallScheduleService callScheduleService;

    // Helper method to create authentication with Long userId as principal
    private UsernamePasswordAuthenticationToken elderlyAuth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_ELDERLY")));
    }

    private UsernamePasswordAuthenticationToken counselorAuth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_COUNSELOR")));
    }

    private UsernamePasswordAuthenticationToken adminAuth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Nested
    @DisplayName("어르신 API")
    class ElderlyApiTest {

        @Test
        @DisplayName("GET /api/call-schedules/me - 본인 스케줄 조회")
        void getMySchedule() throws Exception {
            // given
            Response response = Response.builder()
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .preferredCallTime("09:00")
                    .preferredCallDays(List.of("MON", "WED", "FRI"))
                    .callScheduleEnabled(true)
                    .build();

            given(callScheduleService.getSchedule(anyLong())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/call-schedules/me")
                            .with(authentication(elderlyAuth(1L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.elderlyId").value(1))
                    .andExpect(jsonPath("$.data.preferredCallTime").value("09:00"));
        }

        @Test
        @DisplayName("PUT /api/call-schedules/me - 본인 스케줄 수정")
        void updateMySchedule() throws Exception {
            // given
            UpdateRequest request = new UpdateRequest();
            request.setPreferredCallTime("10:00");
            request.setPreferredCallDays(List.of("TUE", "THU"));
            request.setCallScheduleEnabled(true);

            Response response = Response.builder()
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .preferredCallTime("10:00")
                    .preferredCallDays(List.of("TUE", "THU"))
                    .callScheduleEnabled(true)
                    .build();

            given(callScheduleService.updateSchedule(anyLong(), any(UpdateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/call-schedules/me")
                            .with(authentication(elderlyAuth(1L)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.preferredCallTime").value("10:00"));
        }
    }

    @Nested
    @DisplayName("상담사 API")
    class CounselorApiTest {

        @Test
        @DisplayName("GET /api/call-schedules/counselor/elderly/{id} - 담당 어르신 스케줄 조회")
        void getCounselorElderlySchedule() throws Exception {
            // given
            Response response = Response.builder()
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .preferredCallTime("09:00")
                    .preferredCallDays(List.of("MON", "WED", "FRI"))
                    .callScheduleEnabled(true)
                    .build();

            given(callScheduleService.getSchedule(1L)).willReturn(response);

            UserPrincipal principal = UserPrincipal.of(100L, "counselor1", Role.COUNSELOR);

            // when & then
            mockMvc.perform(get("/api/call-schedules/counselor/elderly/1")
                            .with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.elderlyName").value("홍길동"));
        }

        @Test
        @DisplayName("PUT /api/call-schedules/counselor/elderly/{id} - 직접 스케줄 수정")
        void directUpdateByCounselor() throws Exception {
            // given
            DirectUpdateRequest request = new DirectUpdateRequest();
            request.setPreferredCallTime("14:00");
            request.setPreferredCallDays(List.of("MON", "FRI"));
            request.setCallScheduleEnabled(true);
            request.setChangeReason("어르신 구두 요청");

            Response response = Response.builder()
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .preferredCallTime("14:00")
                    .preferredCallDays(List.of("MON", "FRI"))
                    .callScheduleEnabled(true)
                    .build();

            given(callScheduleService.directUpdateSchedule(anyLong(), anyLong(),
                    any(DirectUpdateRequest.class)))
                    .willReturn(response);

            UserPrincipal principal = UserPrincipal.of(100L, "counselor1", Role.COUNSELOR);

            // when & then
            mockMvc.perform(put("/api/call-schedules/counselor/elderly/1")
                            .with(user(principal))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.preferredCallTime").value("14:00"));
        }

        @Test
        @DisplayName("GET /api/call-schedules/counselor/history - 담당 어르신 변경 이력")
        void getCounselorHistory() throws Exception {
            // given
            given(callScheduleService.getHistoryByCounselor(anyLong())).willReturn(List.of());

            UserPrincipal principal = UserPrincipal.of(100L, "counselor1", Role.COUNSELOR);

            // when & then
            mockMvc.perform(get("/api/call-schedules/counselor/history")
                            .with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }

    @Nested
    @DisplayName("관리자 API")
    class AdminApiTest {

        @Test
        @DisplayName("GET /api/call-schedules/admin/history - 전체 변경 이력")
        void getAllHistory() throws Exception {
            // given
            given(callScheduleService.getAllHistory()).willReturn(List.of());

            UserPrincipal principal = UserPrincipal.of(1000L, "admin1", Role.ADMIN);

            // when & then
            mockMvc.perform(get("/api/call-schedules/admin/history")
                            .with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("GET /api/call-schedules/elderly/{id}/history - 특정 어르신 이력")
        void getElderlyHistory() throws Exception {
            // given
            given(callScheduleService.getHistoryByElderly(1L)).willReturn(List.of());

            UserPrincipal principal = UserPrincipal.of(1000L, "admin1", Role.ADMIN);

            // when & then
            mockMvc.perform(get("/api/call-schedules/elderly/1/history")
                            .with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }

        @Test
        @DisplayName("GET /api/call-schedules - 전체 스케줄 목록")
        void getAllSchedules() throws Exception {
            // given
            given(callScheduleService.getAllSchedules()).willReturn(List.of());

            UserPrincipal principal = UserPrincipal.of(1000L, "admin1", Role.ADMIN);

            // when & then
            mockMvc.perform(get("/api/call-schedules")
                            .with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray());
        }
    }
}
