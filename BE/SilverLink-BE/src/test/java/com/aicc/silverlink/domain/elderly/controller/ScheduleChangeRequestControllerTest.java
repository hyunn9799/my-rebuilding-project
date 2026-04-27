package com.aicc.silverlink.domain.elderly.controller;

import com.aicc.silverlink.domain.elderly.dto.ScheduleChangeRequestDto.*;
import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest.RequestStatus;
import com.aicc.silverlink.domain.elderly.service.ScheduleChangeRequestService;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.global.exception.BusinessException;
import com.aicc.silverlink.global.exception.ErrorCode;
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

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * ScheduleChangeRequestController 통합 테스트
 */
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("ci")
@DisplayName("ScheduleChangeRequestController 테스트")
class ScheduleChangeRequestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ScheduleChangeRequestService changeRequestService;

    // Helper method to create authentication with Long userId as principal
    private UsernamePasswordAuthenticationToken elderlyAuth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_ELDERLY")));
    }

    private UsernamePasswordAuthenticationToken counselorAuth(Long userId) {
        return new UsernamePasswordAuthenticationToken(
                userId, null, List.of(new SimpleGrantedAuthority("ROLE_COUNSELOR")));
    }

    // ===== 어르신 API 테스트 =====

    @Nested
    @DisplayName("어르신 API")
    class ElderlyApiTest {

        @Test
        @DisplayName("POST /api/schedule-change-requests - 변경 요청 생성 성공")
        void createRequest_success() throws Exception {
            // given
            CreateRequest request = new CreateRequest();
            request.setPreferredCallTime("14:00");
            request.setPreferredCallDays(List.of("TUE", "THU"));

            Response response = Response.builder()
                    .id(1L)
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .requestedCallTime("14:00")
                    .requestedCallDays(List.of("TUE", "THU"))
                    .status(RequestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(changeRequestService.createRequest(anyLong(), any(CreateRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(post("/api/schedule-change-requests")
                            .with(authentication(elderlyAuth(1L)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(1))
                    .andExpect(jsonPath("$.data.requestedCallTime").value("14:00"))
                    .andExpect(jsonPath("$.data.status").value("PENDING"));
        }

        @Test
        @DisplayName("POST /api/schedule-change-requests - 중복 요청 시 에러")
        void createRequest_duplicate_throwsError() throws Exception {
            // given
            CreateRequest request = new CreateRequest();
            request.setPreferredCallTime("14:00");
            request.setPreferredCallDays(List.of("TUE", "THU"));

            given(changeRequestService.createRequest(anyLong(), any(CreateRequest.class)))
                    .willThrow(new BusinessException(ErrorCode.DUPLICATE_RESOURCE,
                            "이미 대기 중인 변경 요청이 있습니다."));

            // when & then
            mockMvc.perform(post("/api/schedule-change-requests")
                            .with(authentication(elderlyAuth(1L)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("GET /api/schedule-change-requests/me - 본인 요청 목록 조회")
        void getMyRequests_success() throws Exception {
            // given
            Response response = Response.builder()
                    .id(1L)
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .requestedCallTime("14:00")
                    .requestedCallDays(List.of("TUE", "THU"))
                    .status(RequestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(changeRequestService.getMyRequests(anyLong()))
                    .willReturn(List.of(response));

            // when & then
            mockMvc.perform(get("/api/schedule-change-requests/me")
                            .with(authentication(elderlyAuth(1L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].requestedCallTime").value("14:00"))
                    .andExpect(jsonPath("$.data[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("GET /api/schedule-change-requests/me - 요청 없으면 빈 배열 반환")
        void getMyRequests_empty() throws Exception {
            // given
            given(changeRequestService.getMyRequests(anyLong()))
                    .willReturn(List.of());

            // when & then
            mockMvc.perform(get("/api/schedule-change-requests/me")
                            .with(authentication(elderlyAuth(1L))))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data").isEmpty());
        }
    }

    // ===== 상담사 API 테스트 =====

    @Nested
    @DisplayName("상담사 API")
    class CounselorApiTest {

        @Test
        @DisplayName("GET /api/schedule-change-requests/pending - 대기 중인 요청 목록 조회")
        void getPendingRequests_success() throws Exception {
            // given
            Response response = Response.builder()
                    .id(1L)
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .requestedCallTime("14:00")
                    .requestedCallDays(List.of("TUE", "THU"))
                    .status(RequestStatus.PENDING)
                    .createdAt(LocalDateTime.now())
                    .build();

            given(changeRequestService.getPendingRequests())
                    .willReturn(List.of(response));

            UserPrincipal principal = UserPrincipal.of(100L, "counselor1", Role.COUNSELOR);

            // when & then
            mockMvc.perform(get("/api/schedule-change-requests/pending")
                            .with(user(principal)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data").isArray())
                    .andExpect(jsonPath("$.data[0].status").value("PENDING"));
        }

        @Test
        @DisplayName("PUT /api/schedule-change-requests/{id}/approve - 요청 승인")
        void approveRequest_success() throws Exception {
            // given
            Response response = Response.builder()
                    .id(1L)
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .requestedCallTime("14:00")
                    .requestedCallDays(List.of("TUE", "THU"))
                    .status(RequestStatus.APPROVED)
                    .processedAt(LocalDateTime.now())
                    .processedByName("상담사")
                    .build();

            given(changeRequestService.approveRequest(eq(1L), anyLong()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/schedule-change-requests/1/approve")
                            .with(authentication(counselorAuth(100L)))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("APPROVED"))
                    .andExpect(jsonPath("$.data.processedByName").value("상담사"));
        }

        @Test
        @DisplayName("PUT /api/schedule-change-requests/{id}/approve - 이미 처리된 요청 시 에러")
        void approveRequest_alreadyProcessed_throwsError() throws Exception {
            // given
            given(changeRequestService.approveRequest(eq(1L), anyLong()))
                    .willThrow(new BusinessException(ErrorCode.INVALID_INPUT_VALUE,
                            "이미 처리된 요청입니다."));

            // when & then
            mockMvc.perform(put("/api/schedule-change-requests/1/approve")
                            .with(authentication(counselorAuth(100L)))
                            .with(csrf()))
                    .andExpect(status().is4xxClientError());
        }

        @Test
        @DisplayName("PUT /api/schedule-change-requests/{id}/reject - 요청 거절 (사유 있음)")
        void rejectRequest_withReason_success() throws Exception {
            // given
            RejectRequest rejectRequest = new RejectRequest();
            rejectRequest.setReason("업무 시간 외 통화 요청");

            Response response = Response.builder()
                    .id(1L)
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .requestedCallTime("14:00")
                    .requestedCallDays(List.of("TUE", "THU"))
                    .status(RequestStatus.REJECTED)
                    .processedAt(LocalDateTime.now())
                    .processedByName("상담사")
                    .rejectReason("업무 시간 외 통화 요청")
                    .build();

            given(changeRequestService.rejectRequest(eq(1L), anyLong(), any(RejectRequest.class)))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/schedule-change-requests/1/reject")
                            .with(authentication(counselorAuth(100L)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(rejectRequest)))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REJECTED"))
                    .andExpect(jsonPath("$.data.rejectReason").value("업무 시간 외 통화 요청"));
        }

        @Test
        @DisplayName("PUT /api/schedule-change-requests/{id}/reject - 요청 거절 (사유 없음)")
        void rejectRequest_withoutReason_success() throws Exception {
            // given
            Response response = Response.builder()
                    .id(1L)
                    .elderlyId(1L)
                    .elderlyName("홍길동")
                    .requestedCallTime("14:00")
                    .requestedCallDays(List.of("TUE", "THU"))
                    .status(RequestStatus.REJECTED)
                    .processedAt(LocalDateTime.now())
                    .processedByName("상담사")
                    .build();

            given(changeRequestService.rejectRequest(eq(1L), anyLong(), any()))
                    .willReturn(response);

            // when & then
            mockMvc.perform(put("/api/schedule-change-requests/1/reject")
                            .with(authentication(counselorAuth(100L)))
                            .with(csrf()))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.status").value("REJECTED"));
        }
    }

    // ===== 권한 테스트 =====

    @Nested
    @DisplayName("권한 테스트")
    class AuthorizationTest {

        @Test
        @DisplayName("어르신이 아닌 사용자가 변경 요청 생성 시 403 에러")
        void createRequest_notElderly_forbidden() throws Exception {
            // given
            CreateRequest request = new CreateRequest();
            request.setPreferredCallTime("14:00");
            request.setPreferredCallDays(List.of("TUE", "THU"));

            // when & then
            mockMvc.perform(post("/api/schedule-change-requests")
                            .with(authentication(counselorAuth(100L)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("상담사가 아닌 사용자가 대기 요청 조회 시 403 에러")
        void getPendingRequests_notCounselor_forbidden() throws Exception {
            // when & then
            mockMvc.perform(get("/api/schedule-change-requests/pending")
                            .with(authentication(elderlyAuth(1L))))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("상담사가 아닌 사용자가 승인 시도 시 403 에러")
        void approveRequest_notCounselor_forbidden() throws Exception {
            // when & then
            mockMvc.perform(put("/api/schedule-change-requests/1/approve")
                            .with(authentication(elderlyAuth(1L)))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("상담사가 아닌 사용자가 거절 시도 시 403 에러")
        void rejectRequest_notCounselor_forbidden() throws Exception {
            // when & then
            mockMvc.perform(put("/api/schedule-change-requests/1/reject")
                            .with(authentication(elderlyAuth(1L)))
                            .with(csrf()))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증되지 않은 사용자 요청 시 401 에러")
        void unauthenticated_unauthorized() throws Exception {
            // when & then
            mockMvc.perform(get("/api/schedule-change-requests/me"))
                    .andExpect(status().isUnauthorized());
        }
    }

    // ===== 유효성 검증 테스트 =====

    @Nested
    @DisplayName("유효성 검증 테스트")
    class ValidationTest {

        @Test
        @DisplayName("통화 시간 없이 요청 시 400 에러")
        void createRequest_withoutTime_badRequest() throws Exception {
            // given
            CreateRequest request = new CreateRequest();
            request.setPreferredCallDays(List.of("TUE", "THU"));
            // preferredCallTime 없음

            // when & then
            mockMvc.perform(post("/api/schedule-change-requests")
                            .with(authentication(elderlyAuth(1L)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("통화 요일 없이 요청 시 400 에러")
        void createRequest_withoutDays_badRequest() throws Exception {
            // given
            CreateRequest request = new CreateRequest();
            request.setPreferredCallTime("14:00");
            // preferredCallDays 없음

            // when & then
            mockMvc.perform(post("/api/schedule-change-requests")
                            .with(authentication(elderlyAuth(1L)))
                            .with(csrf())
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andExpect(status().isBadRequest());
        }
    }
}
