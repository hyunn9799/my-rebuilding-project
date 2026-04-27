package com.aicc.silverlink.domain.notice.controller;

import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.entity.Notice.NoticeStatus;
import com.aicc.silverlink.domain.notice.service.NoticeService;
import com.aicc.silverlink.domain.user.entity.Role;
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
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableHandlerMethodArgumentResolver;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.support.WebDataBinderFactory;
import org.springframework.web.context.request.NativeWebRequest;
import org.springframework.web.method.support.HandlerMethodArgumentResolver;
import org.springframework.web.method.support.ModelAndViewContainer;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@Disabled("TODO: Fix mock setup for CI environment - @AuthenticationPrincipal returns null")
class NoticeControllerTest {

    @InjectMocks
    private NoticeController noticeController;

    @Mock
    private NoticeService noticeService;

    private MockMvc mockMvc;
    private ObjectMapper objectMapper; // 테스트 메서드에서 사용하기 위해 필드로 유지

    @BeforeEach
    void setUp() {
        // 1. ObjectMapper 설정 (LocalDateTime 등 날짜 타입 지원)
        objectMapper = new ObjectMapper();
        objectMapper.registerModule(new JavaTimeModule());

        // 2. Mock User 생성 (테스트용 사용자)
        User mockUser = User.builder()
                .id(1L)
                .loginId("testUser")
                .role(Role.ELDERLY)
                .build();

        // 3. MockMvc 설정
        mockMvc = MockMvcBuilders.standaloneSetup(noticeController)
                // setMessageConverters 제거 (기본 컨버터 사용)
                // Pageable 파라미터 해석기 설정
                .setCustomArgumentResolvers(
                        new PageableHandlerMethodArgumentResolver(),
                        // @AuthenticationPrincipal User 파라미터 해석기 설정
                        new HandlerMethodArgumentResolver() {
                            @Override
                            public boolean supportsParameter(MethodParameter parameter) {
                                return parameter.getParameterType().equals(User.class);
                            }

                            @Override
                            public Object resolveArgument(MethodParameter parameter,
                                                          ModelAndViewContainer mavContainer,
                                                          NativeWebRequest webRequest,
                                                          WebDataBinderFactory binderFactory) {
                                return mockUser;
                            }
                        })
                .build();
    }

    @Test
    @DisplayName("내 공지사항 목록 조회 테스트")
    void getMyNotices() throws Exception {
        // given
        NoticeResponse response = NoticeResponse.builder()
                .id(1L)
                .title("테스트 공지")
                .content("내용")
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .build();

        // 가변 리스트 사용 (직렬화 오류 방지)
        List<NoticeResponse> content = new ArrayList<>();
        content.add(response);

        // PageRequest를 포함하여 PageImpl 생성
        Pageable pageable = PageRequest.of(0, 10);
        Page<NoticeResponse> pageResult = new PageImpl<>(content, pageable, 1);

        // 검색 키워드 파라미터(null) 추가
        given(noticeService.getNoticesForUser(any(User.class), eq(null), any(Pageable.class)))
                .willReturn(pageResult);

        // when & then
        mockMvc.perform(get("/api/notices")
                        .param("page", "0")
                        .param("size", "10")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].title").value("테스트 공지"));
    }

    @Test
    @DisplayName("팝업 공지 조회 테스트")
    void getPopups() throws Exception {
        // given
        NoticeResponse popup = NoticeResponse.builder()
                .id(1L)
                .title("팝업1")
                .isPopup(true)
                .status(NoticeStatus.PUBLISHED)
                .build();

        // 가변 리스트 사용
        List<NoticeResponse> popups = new ArrayList<>();
        popups.add(popup);

        given(noticeService.getActivePopupsForUser(any(User.class)))
                .willReturn(popups);

        // when & then
        mockMvc.perform(get("/api/notices/popups")
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].title").value("팝업1"))
                // Lombok의 boolean getter는 isPopup() -> JSON 필드명 "popup"으로 매핑됨
                .andExpect(jsonPath("$[0].popup").value(true));
    }

    @Test
    @DisplayName("공지 상세 조회 테스트")
    void getNoticeDetail() throws Exception {
        // given
        Long noticeId = 1L;
        NoticeResponse response = NoticeResponse.builder()
                .id(noticeId)
                .title("상세 공지")
                .status(NoticeStatus.PUBLISHED)
                .build();

        given(noticeService.getNoticeDetail(eq(noticeId), any(User.class)))
                .willReturn(response);

        // when & then
        mockMvc.perform(get("/api/notices/{id}", noticeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("상세 공지"));
    }

    @Test
    @DisplayName("공지 읽음 처리 테스트")
    void readNotice() throws Exception {
        // given
        Long noticeId = 1L;

        // when & then
        mockMvc.perform(post("/api/notices/{id}/read", noticeId)
                        .contentType(MediaType.APPLICATION_JSON))
                .andDo(print())
                .andExpect(status().isOk());

        verify(noticeService).readNotice(eq(noticeId), any(User.class));
    }
}
//수정
