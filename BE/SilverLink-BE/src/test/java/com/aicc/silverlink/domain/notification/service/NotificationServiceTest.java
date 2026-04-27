package com.aicc.silverlink.domain.notification.service;

import com.aicc.silverlink.domain.notification.dto.NotificationDto.*;
import com.aicc.silverlink.domain.notification.entity.Notification;
import com.aicc.silverlink.domain.notification.entity.Notification.NotificationType;
import com.aicc.silverlink.domain.notification.repository.NotificationRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("NotificationService 테스트")
class NotificationServiceTest {

    @InjectMocks
    private NotificationService notificationService;

    @Mock
    private NotificationRepository notificationRepository;

    @Mock
    private UserRepository userRepository;

    @Mock
    private UnifiedSseService unifiedSseService;

    @Mock
    private NotificationSmsService notificationSmsService;

    // 테스트 픽스처
    private User guardianUser;
    private User adminUser;
    private Notification notification;

    @BeforeEach
    void setUp() {
        // 보호자 사용자
        guardianUser = User.builder()
                .loginId("guardian1")
                .passwordHash("encoded")
                .name("김보호")
                .phone("01012345678")
                .email("guardian@test.com")
                .role(Role.GUARDIAN)
                .status(UserStatus.ACTIVE)
                .build();
        setId(guardianUser, 1L);

        // 관리자 사용자
        adminUser = User.builder()
                .loginId("admin1")
                .passwordHash("encoded")
                .name("이관리")
                .phone("01011112222")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        setId(adminUser, 2L);

        // 테스트용 알림
        notification = Notification.builder()
                .receiver(guardianUser)
                .notificationType(NotificationType.INQUIRY_REPLY)
                .title("문의 답변이 등록되었습니다")
                .content("문의하신 내용에 대한 답변이 등록되었습니다.")
                .referenceType("inquiries")
                .referenceId(100L)
                .build();
        setNotificationId(notification, 1L);
    }

    // ========== 알림 생성 테스트 ==========

    @Nested
    @DisplayName("알림 생성")
    class CreateNotificationTest {

        @Test
        @DisplayName("성공 - 문의 답변 알림 생성")
        void createInquiryReplyNotification_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(guardianUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
                Notification n = invocation.getArgument(0);
                setNotificationId(n, 1L);
                return n;
            });

            // when
            Notification result = notificationService.createInquiryReplyNotification(1L, 100L, "테스트 문의");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNotificationType()).isEqualTo(NotificationType.INQUIRY_REPLY);
            assertThat(result.getReferenceType()).isEqualTo("inquiries");
            assertThat(result.getReferenceId()).isEqualTo(100L);
            then(notificationRepository).should().save(any(Notification.class));
        }

        @Test
        @DisplayName("성공 - 민원 답변 알림 생성")
        void createComplaintReplyNotification_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(guardianUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
                Notification n = invocation.getArgument(0);
                setNotificationId(n, 2L);
                return n;
            });

            // when
            Notification result = notificationService.createComplaintReplyNotification(1L, 200L, "테스트 민원");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNotificationType()).isEqualTo(NotificationType.COMPLAINT_REPLY);
            assertThat(result.getReferenceType()).isEqualTo("complaints");
            assertThat(result.getReferenceId()).isEqualTo(200L);
        }

        @Test
        @DisplayName("성공 - 접근권한 승인 알림 생성")
        void createAccessApprovedNotification_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(guardianUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
                Notification n = invocation.getArgument(0);
                setNotificationId(n, 3L);
                return n;
            });

            // when
            Notification result = notificationService.createAccessApprovedNotification(1L, 300L, "박어르신");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNotificationType()).isEqualTo(NotificationType.ACCESS_APPROVED);
            assertThat(result.getReferenceType()).isEqualTo("access_requests");
        }

        @Test
        @DisplayName("성공 - 접근권한 거절 알림 생성")
        void createAccessRejectedNotification_Success() {
            // given
            given(userRepository.findById(1L)).willReturn(Optional.of(guardianUser));
            given(notificationRepository.save(any(Notification.class))).willAnswer(invocation -> {
                Notification n = invocation.getArgument(0);
                setNotificationId(n, 4L);
                return n;
            });

            // when
            Notification result = notificationService.createAccessRejectedNotification(1L, 400L, "박어르신", "서류 불충분");

            // then
            assertThat(result).isNotNull();
            assertThat(result.getNotificationType()).isEqualTo(NotificationType.ACCESS_REJECTED);
            assertThat(result.getContent()).contains("거절");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 사용자")
        void createNotification_Fail_UserNotFound() {
            // given
            given(userRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> notificationService.createInquiryReplyNotification(999L, 100L, "테스트"))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("사용자를 찾을 수 없습니다");
        }
    }

    // ========== 알림 조회 테스트 ==========

    @Nested
    @DisplayName("알림 조회")
    class GetNotificationsTest {

        @Test
        @DisplayName("성공 - 사용자별 알림 목록 조회")
        void getNotifications_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<Notification> page = new PageImpl<>(List.of(notification), pageable, 1);

            given(notificationRepository.findByReceiverId(1L, pageable)).willReturn(page);

            // when
            Page<SummaryResponse> result = notificationService.getNotifications(1L, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getTitle()).isEqualTo("문의 답변이 등록되었습니다");
        }

        @Test
        @DisplayName("성공 - 미확인 알림 목록 조회")
        void getUnreadNotifications_Success() {
            // given
            given(notificationRepository.findUnreadByReceiverId(1L)).willReturn(List.of(notification));

            // when
            List<SummaryResponse> result = notificationService.getUnreadNotifications(1L);

            // then
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("성공 - 미확인 알림 수 조회")
        void getUnreadCount_Success() {
            // given
            given(notificationRepository.countUnreadByReceiverId(1L)).willReturn(5L);

            // when
            long count = notificationService.getUnreadCount(1L);

            // then
            assertThat(count).isEqualTo(5);
        }
    }

    // ========== 읽음 처리 테스트 ==========

    @Nested
    @DisplayName("읽음 처리")
    class MarkAsReadTest {

        @Test
        @DisplayName("성공 - 단건 읽음 처리")
        void markAsRead_Success() {
            // given
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            // when
            notificationService.markAsRead(1L, 1L);

            // then
            assertThat(notification.getIsRead()).isTrue();
            assertThat(notification.getReadAt()).isNotNull();
        }

        @Test
        @DisplayName("실패 - 다른 사용자의 알림 읽음 처리 시도")
        void markAsRead_Fail_NotOwner() {
            // given
            given(notificationRepository.findById(1L)).willReturn(Optional.of(notification));

            // when & then
            assertThatThrownBy(() -> notificationService.markAsRead(1L, 999L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("접근 권한이 없습니다");
        }

        @Test
        @DisplayName("성공 - 전체 읽음 처리")
        void markAllAsRead_Success() {
            // given
            given(notificationRepository.markAllAsReadByReceiverId(eq(1L), any()))
                    .willReturn(3);

            // when
            notificationService.markAllAsRead(1L);

            // then
            then(notificationRepository).should().markAllAsReadByReceiverId(eq(1L), any());
        }
    }

    // ========== 알림 통계 테스트 ==========

    @Nested
    @DisplayName("알림 통계")
    class StatsTest {

        @Test
        @DisplayName("성공 - 사용자별 알림 통계 조회")
        void getStats_Success() {
            // given
            given(notificationRepository.count()).willReturn(10L);
            given(notificationRepository.countUnreadByReceiverId(1L)).willReturn(3L);
            given(notificationRepository.countByTypeForUser(1L)).willReturn(List.of());

            // when
            StatsResponse stats = notificationService.getStats(1L);

            // then
            assertThat(stats.getUnreadCount()).isEqualTo(3);
        }
    }

    // ========== Helper Methods ==========

    private void setId(User user, Long id) {
        try {
            var field = User.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(user, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setNotificationId(Notification notification, Long id) {
        try {
            var field = Notification.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(notification, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
