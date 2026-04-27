package com.aicc.silverlink.domain.emergency.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.assignment.entity.Assignment;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.emergency.dto.EmergencyAlertDto.*;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.AlertStatus;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.AlertType;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.Severity;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlertRecipient;
import com.aicc.silverlink.domain.emergency.repository.EmergencyAlertRecipientRepository;
import com.aicc.silverlink.domain.emergency.repository.EmergencyAlertRepository;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.notification.service.UnifiedSseService;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision.DivisionLevel;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("EmergencyAlertService 테스트")
class EmergencyAlertServiceTest {

    @InjectMocks
    private EmergencyAlertService emergencyAlertService;

    @Mock
    private EmergencyAlertRepository alertRepository;

    @Mock
    private EmergencyAlertRecipientRepository recipientRepository;

    @Mock
    private ElderlyRepository elderlyRepository;

    @Mock
    private CounselorRepository counselorRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private GuardianElderlyRepository guardianElderlyRepository;

    @Mock
    private UnifiedSseService unifiedSseService;

    @Mock
    private SmsService smsService;

    // 테스트 픽스처
    private User elderlyUser;
    private User counselorUser;
    private User guardianUser;
    private User adminUser;
    private AdministrativeDivision yeoksamDong;
    private Elderly elderly;
    private Counselor counselor;
    private Guardian guardian;
    private Admin admin;
    private GuardianElderly guardianElderly;
    private Assignment assignment;
    private EmergencyAlert emergencyAlert;

    @BeforeEach
    void setUp() {
        // 1. 행정구역
        yeoksamDong = AdministrativeDivision.builder()
                .admCode(1168010100L)
                .sidoName("서울특별시")
                .sigunguName("강남구")
                .dongName("역삼1동")
                .level(DivisionLevel.DONG)
                .build();

        // 2. 사용자들 생성
        elderlyUser = User.builder()
                .loginId("elderly1")
                .passwordHash("encoded")
                .name("박어르신")
                .phone("01087654321")
                .role(Role.ELDERLY)
                .status(UserStatus.ACTIVE)
                .build();
        setId(elderlyUser, 1L);

        counselorUser = User.builder()
                .loginId("counselor1")
                .passwordHash("encoded")
                .name("최상담")
                .phone("01033334444")
                .role(Role.COUNSELOR)
                .status(UserStatus.ACTIVE)
                .build();
        setId(counselorUser, 2L);

        guardianUser = User.builder()
                .loginId("guardian1")
                .passwordHash("encoded")
                .name("김보호")
                .phone("01012345678")
                .role(Role.GUARDIAN)
                .status(UserStatus.ACTIVE)
                .build();
        setId(guardianUser, 3L);

        adminUser = User.builder()
                .loginId("admin1")
                .passwordHash("encoded")
                .name("이관리")
                .phone("01011112222")
                .role(Role.ADMIN)
                .status(UserStatus.ACTIVE)
                .build();
        setId(adminUser, 4L);

        // 3. 어르신 엔티티
        elderly = Elderly.builder()
                .user(elderlyUser)
                .administrativeDivision(yeoksamDong)
                .birthDate(LocalDate.of(1940, 5, 15))
                .gender(Elderly.Gender.M)
                .addressLine1("서울시 강남구 역삼동")
                .build();
        setElderlyId(elderly, 1L);

        // 4. 상담사 엔티티
        counselor = mock(Counselor.class);
        given(counselor.getId()).willReturn(2L);
        given(counselor.getUser()).willReturn(counselorUser);

        // 5. 보호자 엔티티
        guardian = Guardian.builder()
                .user(guardianUser)
                .addressLine1("서울시 강남구")
                .build();

        // 6. 관리자 엔티티
        admin = Admin.builder()
                .user(adminUser)
                .administrativeDivision(yeoksamDong)
                .adminLevel(Admin.AdminLevel.DISTRICT)
                .build();

        // 7. 보호자-어르신 관계
        guardianElderly = GuardianElderly.builder()
                .guardian(guardian)
                .elderly(elderly)
                .relationType(RelationType.CHILD)
                .build();

        // 8. 배정 정보
        assignment = mock(Assignment.class);
        given(assignment.getCounselor()).willReturn(counselor);

        // 9. 긴급 알림
        emergencyAlert = EmergencyAlert.builder()
                .elderly(elderly)
                .severity(Severity.CRITICAL)
                .alertType(AlertType.HEALTH)
                .title("긴급: 건강 이상 감지")
                .description("어르신의 건강 상태에 이상이 감지되었습니다.")
                .status(AlertStatus.PENDING)
                .build();
        setAlertId(emergencyAlert, 100L);
    }

    // ========== 알림 조회 테스트 ==========

    @Nested
    @DisplayName("알림 조회")
    class GetAlertsTest {

        @Test
        @DisplayName("성공 - 상담사용 긴급 알림 목록 조회")
        void getAlertsForCounselor_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<EmergencyAlert> page = new PageImpl<>(List.of(emergencyAlert), pageable, 1);

            given(alertRepository.findByCounselorId(2L, pageable)).willReturn(page);
            given(guardianElderlyRepository.findByElderlyId(anyLong()))
                    .willReturn(Optional.of(guardianElderly));

            // when
            Page<SummaryResponse> result = emergencyAlertService.getAlertsForCounselor(2L, pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("성공 - 관리자용 긴급 알림 목록 조회")
        void getAlertsForAdmin_Success() {
            // given
            Pageable pageable = PageRequest.of(0, 10);
            Page<EmergencyAlert> page = new PageImpl<>(List.of(emergencyAlert), pageable, 1);

            given(alertRepository.findAllWithDetails(pageable)).willReturn(page);
            given(guardianElderlyRepository.findByElderlyId(anyLong()))
                    .willReturn(Optional.of(guardianElderly));

            // when
            Page<SummaryResponse> result = emergencyAlertService.getAlertsForAdmin(pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
        }

        @Test
        @DisplayName("성공 - 알림 상세 조회")
        void getAlertDetail_Success() {
            // given
            given(alertRepository.findByIdWithDetails(100L)).willReturn(Optional.of(emergencyAlert));
            given(guardianElderlyRepository.findByElderlyId(anyLong()))
                    .willReturn(Optional.of(guardianElderly));
            given(recipientRepository.findByEmergencyAlertId(100L)).willReturn(List.of());
            given(recipientRepository.findByEmergencyAlertIdAndReceiverId(100L, 2L))
                    .willReturn(Optional.empty());

            // when
            DetailResponse result = emergencyAlertService.getAlertDetail(100L, 2L);

            // then
            assertThat(result).isNotNull();
            assertThat(result.getElderly().getName()).isEqualTo("박어르신");
        }

        @Test
        @DisplayName("실패 - 존재하지 않는 알림")
        void getAlertDetail_Fail_NotFound() {
            // given
            given(alertRepository.findByIdWithDetails(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> emergencyAlertService.getAlertDetail(999L, 2L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("긴급 알림을 찾을 수 없습니다");
        }
    }

    // ========== 읽음 처리 테스트 ==========

    @Nested
    @DisplayName("읽음 처리")
    class MarkAsReadTest {

        @Test
        @DisplayName("성공 - 알림 읽음 처리")
        void markAsRead_Success() {
            // given
            EmergencyAlertRecipient realRecipient = EmergencyAlertRecipient.builder()
                    .emergencyAlert(emergencyAlert)
                    .receiver(guardianUser)
                    .build();

            given(recipientRepository.findByEmergencyAlertIdAndReceiverId(100L, 3L))
                    .willReturn(Optional.of(realRecipient));

            // when
            emergencyAlertService.markAsRead(100L, 3L);

            // then
            assertThat(realRecipient.isRead()).isTrue();
        }
    }

    // ========== 통계 테스트 ==========

    @Nested
    @DisplayName("통계 조회")
    class StatsTest {

        @Test
        @DisplayName("성공 - 전체 통계 조회")
        void getStats_Success() {
            // given
            given(alertRepository.countByStatus(AlertStatus.PENDING)).willReturn(5L);
            given(alertRepository.countByStatus(AlertStatus.IN_PROGRESS)).willReturn(3L);
            given(alertRepository.countByStatus(AlertStatus.RESOLVED)).willReturn(10L);
            given(alertRepository.countBySeverity(Severity.CRITICAL)).willReturn(8L);
            given(alertRepository.countBySeverity(Severity.WARNING)).willReturn(10L);

            // when
            StatsResponse stats = emergencyAlertService.getStats();

            // then
            assertThat(stats.getPendingCount()).isEqualTo(5);
            assertThat(stats.getInProgressCount()).isEqualTo(3);
            assertThat(stats.getResolvedCount()).isEqualTo(10);
            assertThat(stats.getCriticalCount()).isEqualTo(8);
            assertThat(stats.getWarningCount()).isEqualTo(10);
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

    private void setElderlyId(Elderly elderly, Long id) {
        try {
            var field = Elderly.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(elderly, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void setAlertId(EmergencyAlert alert, Long id) {
        try {
            var field = EmergencyAlert.class.getDeclaredField("id");
            field.setAccessible(true);
            field.set(alert, id);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
}
