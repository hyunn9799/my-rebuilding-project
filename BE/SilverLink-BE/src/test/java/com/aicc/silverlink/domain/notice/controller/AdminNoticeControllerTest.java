package com.aicc.silverlink.domain.notice.controller;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.notice.dto.NoticeRequest;
import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.entity.Notice.TargetMode;
import com.aicc.silverlink.domain.notice.entity.NoticeCategory;
import com.aicc.silverlink.domain.notice.service.NoticeService;
import com.aicc.silverlink.domain.user.entity.User;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.core.MethodParameter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.util.Collections;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled("TODO: Fix AdminRepository mock injection for CI environment")
class AdminNoticeControllerTest {

    @InjectMocks
    private AdminNoticeController adminNoticeController;

    @Mock
    private NoticeService noticeService;

    @Mock
    private com.aicc.silverlink.domain.admin.repository.AdminRepository adminRepository;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper;
    private User mockUser;

    @BeforeEach
    void setUp() {
        // ObjectMapper 설정 (LocalDateTime 지원)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // Mock User 생성
        mockUser = mock(User.class);
        lenient().when(mockUser.getId()).thenReturn(1L);

        // MockMvc 설정 (AuthenticationPrincipal 처리 포함)
        mockMvc = MockMvcBuilders.standaloneSetup(adminNoticeController)
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().equals(User.class)
                                        && parameter.hasParameterAnnotation(
                                        org.springframework.security.core.annotation.AuthenticationPrincipal.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return mockUser; // Return mock User object
                            }
                        })
                .build();
    }

    @Test
    @DisplayName("관리자 공지사항 목록 조회 테스트")
    void getAllNotices() throws Exception {
        // given
        NoticeResponse response = NoticeResponse.builder()
                .id(1L)
                .title("Admin Notice")
                .content("Content")
                .build();

        List<NoticeResponse> content = Collections.singletonList(response);
        Page<NoticeResponse> pageResponse = new PageImpl<>(content,
                org.springframework.data.domain.PageRequest.of(0, 10), content.size());

        given(noticeService.getAllNoticesForAdmin(any(Pageable.class))).willReturn(pageResponse);

        // when & then
        mockMvc.perform(get("/api/admin/notices")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(1))
                .andExpect(jsonPath("$.content[0].title").value("Admin Notice"));
    }

    @Test
    @DisplayName("공지사항 등록 테스트")
    void createNotice() throws Exception {
        // given
        NoticeRequest request = new NoticeRequest();
        request.setTitle("New Notice");
        request.setContent("New Content");
        request.setCategory(NoticeCategory.NOTICE); // 필수 값 추가
        request.setTargetMode(TargetMode.ALL); // 필수 값 추가

        Admin mockAdmin = mock(Admin.class);
        given(adminRepository.findByUserId(1L)).willReturn(java.util.Optional.of(mockAdmin));
        given(noticeService.createNotice(any(NoticeRequest.class), any(Admin.class))).willReturn(1L);

        // when & then
        mockMvc.perform(post("/api/admin/notices")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(content().string("1"));
    }

    @Test
    @DisplayName("공지사항 상세 조회 테스트")
    void getNotice() throws Exception {
        // given
        Long noticeId = 1L;
        NoticeResponse response = NoticeResponse.builder()
                .id(noticeId)
                .title("Detail Notice")
                .content("Detail Content")
                .build();

        given(noticeService.getNoticeDetail(eq(noticeId), any())).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/admin/notices/{id}", noticeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(noticeId))
                .andExpect(jsonPath("$.title").value("Detail Notice"));
    }

    @Test
    @DisplayName("공지사항 삭제 테스트")
    void deleteNotice() throws Exception {
        // given
        Long noticeId = 1L;
        Admin mockAdmin = mock(Admin.class);
        given(adminRepository.findByUserId(1L)).willReturn(java.util.Optional.of(mockAdmin));

        // when & then
        mockMvc.perform(delete("/api/admin/notices/{id}", noticeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(noticeService).deleteNotice(eq(noticeId), any(Admin.class));
    }
}
