package com.aicc.silverlink.domain.notice.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.notice.dto.NoticeRequest;
import com.aicc.silverlink.domain.notice.dto.NoticeResponse;
import com.aicc.silverlink.domain.notice.entity.Notice;
import com.aicc.silverlink.domain.notice.entity.Notice.NoticeStatus;
import com.aicc.silverlink.domain.notice.entity.Notice.TargetMode;
import com.aicc.silverlink.domain.notice.entity.NoticeCategory;
import com.aicc.silverlink.domain.notice.repository.NoticeAttachmentRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeReadLogRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeRepository;
import com.aicc.silverlink.domain.notice.repository.NoticeTargetRoleRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;
import org.mockito.ArgumentCaptor;

@ExtendWith(MockitoExtension.class)
class NoticeServiceTest {

    @InjectMocks
    private NoticeService noticeService;

    @Mock
    private NoticeRepository noticeRepository;

    @Mock
    private NoticeReadLogRepository noticeReadLogRepository;

    @Mock
    private NoticeTargetRoleRepository noticeTargetRoleRepository;

    @Mock
    private NoticeAttachmentRepository noticeAttachmentRepository;

    @Test
    @DisplayName("공지사항 생성 테스트")
    void createNotice() {
        // given
        Admin admin = mock(Admin.class);
        NoticeRequest request = new NoticeRequest();
        request.setTitle("Test Title");
        request.setContent("Test Content");
        request.setCategory(NoticeCategory.NOTICE); // 카테고리 설정
        request.setTargetMode(TargetMode.ALL);
        request.setPriority(true);
        request.setPopup(false);
        request.setAttachments(new ArrayList<>());

        Notice savedNotice = Notice.builder()
                .id(1L)
                .title("Test Title")
                .content("Test Content")
                .category(NoticeCategory.NOTICE)
                .targetMode(TargetMode.ALL)
                .status(NoticeStatus.PUBLISHED)
                .build();

        given(noticeRepository.save(any(Notice.class))).willReturn(savedNotice);

        // when
        Long noticeId = noticeService.createNotice(request, admin);

        // then
        assertNotNull(noticeId);
        assertEquals(1L, noticeId);
        verify(noticeRepository, times(1)).save(any(Notice.class));
    }

    @Test
    @DisplayName("공지사항 삭제 테스트")
    void deleteNotice() {
        // given
        Long noticeId = 1L;

        Long adminId = 100L;

        // Admin Mocking
        Admin admin = mock(Admin.class);
        // Notice Mocking
        Notice notice = Notice.builder()
                .id(noticeId)
                .title("Delete Test")
                .content("Content")
                .targetMode(TargetMode.ALL)
                .status(NoticeStatus.PUBLISHED)
                .build();

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));

        // when
        noticeService.deleteNotice(noticeId, admin); // admin 파라미터 추가

        // then
        verify(noticeRepository, times(1)).findById(noticeId);

        ArgumentCaptor<Notice> noticeCaptor = ArgumentCaptor.forClass(Notice.class);
        verify(noticeRepository, times(1)).save(noticeCaptor.capture());

        Notice capturedNotice = noticeCaptor.getValue();
        assertEquals(NoticeStatus.DELETED, capturedNotice.getStatus());
    }

    @Test
    @DisplayName("관리자용 공지사항 목록 조회 테스트")
    void getAllNoticesForAdmin() {
        // given
        Pageable pageable = PageRequest.of(0, 10);
        Notice notice = Notice.builder()
                .id(1L)
                .title("Admin Notice")
                .content("Content")
                .targetMode(TargetMode.ALL)
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Page<Notice> noticePage = new PageImpl<>(Collections.singletonList(notice));

        given(noticeRepository.findAllByStatusNot(NoticeStatus.DELETED, pageable)).willReturn(noticePage);
        given(noticeTargetRoleRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());
        given(noticeAttachmentRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());

        // when
        Page<NoticeResponse> result = noticeService.getAllNoticesForAdmin(pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertEquals("Admin Notice", result.getContent().get(0).getTitle());
    }

    @Test
    @DisplayName("사용자용 공지사항 목록 조회 테스트 (검색 포함)")
    void getNoticesForUser() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);
        given(user.getRole()).willReturn(Role.ELDERLY);

        String keyword = "Test"; // 검색 키워드
        Pageable pageable = PageRequest.of(0, 10);
        Notice notice = Notice.builder()
                .id(1L)
                .title("User Notice Test")
                .content("Content")
                .targetMode(TargetMode.ALL)
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Page<Notice> noticePage = new PageImpl<>(Collections.singletonList(notice));

        // 검색 키워드 파라미터 추가
        given(noticeRepository.findAllForUser(Role.ELDERLY, keyword, pageable)).willReturn(noticePage);
        given(noticeReadLogRepository.existsByNoticeIdAndUserId(1L, 100L)).willReturn(false);
        given(noticeTargetRoleRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());
        given(noticeAttachmentRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());

        // when
        Page<NoticeResponse> result = noticeService.getNoticesForUser(user, keyword, pageable);

        // then
        assertNotNull(result);
        assertEquals(1, result.getTotalElements());
        assertFalse(result.getContent().get(0).isRead());
    }

    @Test
    @DisplayName("사용자용 팝업 공지 조회 테스트")
    void getActivePopupsForUser() {
        // given
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);
        given(user.getRole()).willReturn(Role.ELDERLY);

        Notice notice = Notice.builder()
                .id(1L)
                .title("Popup Notice")
                .content("Popup Content")
                .targetMode(TargetMode.ALL)
                .isPopup(true)
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(noticeRepository.findActivePopups(eq(Role.ELDERLY), any(LocalDateTime.class)))
                .willReturn(Collections.singletonList(notice));
        given(noticeReadLogRepository.existsByNoticeIdAndUserId(1L, 100L)).willReturn(false);
        given(noticeTargetRoleRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());
        given(noticeAttachmentRepository.findAllByNoticeId(anyLong())).willReturn(Collections.emptyList());

        // when
        List<NoticeResponse> result = noticeService.getActivePopupsForUser(user);

        // then
        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).isPopup());
    }

    @Test
    @DisplayName("공지사항 읽음 처리 테스트")
    void readNotice() {
        // given
        Long noticeId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);

        given(noticeReadLogRepository.existsByNoticeIdAndUserId(noticeId, 100L)).willReturn(false);
        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(mock(Notice.class)));

        // when
        noticeService.readNotice(noticeId, user);

        // then
        verify(noticeRepository, times(1)).findById(noticeId);
    }

    @Test
    @DisplayName("공지사항 상세 조회 테스트 (이전/다음 글 포함)")
    void getNoticeDetail() {
        // given
        Long noticeId = 1L;
        User user = mock(User.class);
        given(user.getId()).willReturn(100L);
        given(user.getRole()).willReturn(Role.ELDERLY);

        Notice notice = Notice.builder()
                .id(noticeId)
                .title("Detail Notice")
                .content("Detail Content")
                .targetMode(TargetMode.ALL)
                .status(NoticeStatus.PUBLISHED)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        given(noticeRepository.findById(noticeId)).willReturn(Optional.of(notice));
        given(noticeReadLogRepository.existsByNoticeIdAndUserId(noticeId, 100L)).willReturn(true);
        given(noticeTargetRoleRepository.findAllByNoticeId(noticeId)).willReturn(Collections.emptyList());
        given(noticeAttachmentRepository.findAllByNoticeId(noticeId)).willReturn(Collections.emptyList());

        // 이전/다음 글 ID Mocking
        given(noticeRepository.findPrevNoticeId(Role.ELDERLY, noticeId)).willReturn(Optional.of(0L));
        given(noticeRepository.findNextNoticeId(Role.ELDERLY, noticeId)).willReturn(Optional.of(2L));

        // when
        NoticeResponse response = noticeService.getNoticeDetail(noticeId, user);

        // then
        assertNotNull(response);
        assertEquals("Detail Notice", response.getTitle());
        assertTrue(response.isRead());
        assertEquals(0L, response.getPrevNoticeId());
        assertEquals(2L, response.getNextNoticeId());
    }
}
