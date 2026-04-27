package com.aicc.silverlink.domain.assignment.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.assignment.dto.AssignmentRequest;
import com.aicc.silverlink.domain.assignment.dto.AssignmentResponse;
import com.aicc.silverlink.domain.assignment.entity.Assignment;
import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.audit.service.AuditLogService;
import com.aicc.silverlink.domain.notification.service.NotificationService;
import com.aicc.silverlink.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT) // 가이드라인 준수: 불필요한 stubbing 에러 방지
class AssignmentServiceTest {

    @Mock
    private AssignmentRepository assignmentRepository;

    @Mock
    private CounselorRepository counselorRepository;

    @Mock
    private ElderlyRepository elderlyRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AuditLogService auditLogService;

    @Mock
    private NotificationService notificationService;

    @InjectMocks
    private AssignmentService assignmentService;

    // Mock Entities
    private Counselor mockCounselor;
    private Elderly mockElderly;
    private Admin mockAdmin;
    private Assignment mockAssignment;
    private User mockUserCounselor;
    private User mockUserElderly;
    private User mockUserAdmin;

    @BeforeEach
    void setUp() {
        // 1. User Mock 설정 (AssignmentResponse.from() 내부 접근용)
        mockUserCounselor = mock(User.class);
        given(mockUserCounselor.getName()).willReturn("김상담");

        mockUserElderly = mock(User.class);
        given(mockUserElderly.getName()).willReturn("이노인");

        mockUserAdmin = mock(User.class);
        given(mockUserAdmin.getName()).willReturn("박관리");

        // 2. Domain Entity Mock 설정
        mockCounselor = mock(Counselor.class);
        given(mockCounselor.getId()).willReturn(1L);
        given(mockCounselor.getUser()).willReturn(mockUserCounselor);

        mockElderly = mock(Elderly.class);
        given(mockElderly.getId()).willReturn(2L);
        given(mockElderly.getUser()).willReturn(mockUserElderly);

        mockAdmin = mock(Admin.class);
        given(mockAdmin.getUserId()).willReturn(3L);
        given(mockAdmin.getUser()).willReturn(mockUserAdmin);

        // 3. Assignment Mock 설정
        mockAssignment = mock(Assignment.class);
        given(mockAssignment.getId()).willReturn(100L);
        given(mockAssignment.getCounselor()).willReturn(mockCounselor);
        given(mockAssignment.getElderly()).willReturn(mockElderly);
        given(mockAssignment.getAssignedBy()).willReturn(mockAdmin);
        given(mockAssignment.getStatus()).willReturn(AssignmentStatus.ACTIVE);
    }

    @Test
    @DisplayName("배정 성공 - 상담사, 어르신, 관리자가 존재하고 중복 배정이 아닐 때")
    void assignCounselor_Success() {
        // given
        AssignmentRequest request = new AssignmentRequest(1L, 2L, 3L); // Request 생성자 필요 (혹은 Setter/Mock 활용)

        given(counselorRepository.findById(1L)).willReturn(Optional.of(mockCounselor));
        given(elderlyRepository.findById(2L)).willReturn(Optional.of(mockElderly));
        given(adminRepository.findById(3L)).willReturn(Optional.of(mockAdmin));

        // 중복 없음
        given(assignmentRepository.existsByElderly_IdAndStatus(2L, AssignmentStatus.ACTIVE))
                .willReturn(false);

        // save 호출 시 mockAssignment 반환
        given(assignmentRepository.save(any(Assignment.class))).willReturn(mockAssignment);

        // when
        AssignmentResponse response = assignmentService.assignCounselor(request, 3L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getAssignmentId()).isEqualTo(100L);
        assertThat(response.getCounselorName()).isEqualTo("김상담");
        assertThat(response.getElderlyName()).isEqualTo("이노인");

        verify(assignmentRepository).save(any(Assignment.class));
    }

    @Test
    @DisplayName("배정 실패 - 상담사가 존재하지 않음")
    void assignCounselor_Fail_CounselorNotFound() {
        // given
        AssignmentRequest request = new AssignmentRequest(99L, 2L, 3L);
        given(counselorRepository.findById(99L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> assignmentService.assignCounselor(request, 99L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("존재하지 않은 상담사입니다.");
    }

    @Test
    @DisplayName("배정 실패 - 이미 배정된 어르신")
    void assignCounselor_Fail_AlreadyAssigned() {
        // given
        AssignmentRequest request = new AssignmentRequest(1L, 2L, 3L);

        given(counselorRepository.findById(1L)).willReturn(Optional.of(mockCounselor));
        given(elderlyRepository.findById(2L)).willReturn(Optional.of(mockElderly));
        given(adminRepository.findById(3L)).willReturn(Optional.of(mockAdmin));

        // 중복 존재
        given(assignmentRepository.existsByElderly_IdAndStatus(2L, AssignmentStatus.ACTIVE))
                .willReturn(true);

        // when & then
        assertThatThrownBy(() -> assignmentService.assignCounselor(request, 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("해당 어르신은 이미 담당 상담사가 배정되었습니다.");
    }

    @Test
    @DisplayName("배정 해제 성공 - 활성화된 배정 정보를 찾아 종료 처리")
    void unassignCounselor_Success() {
        // given
        given(assignmentRepository.findByCounselorAndElderlyAndStatus(1L, 2L, AssignmentStatus.ACTIVE))
                .willReturn(Optional.of(mockAssignment));

        // when
        assignmentService.unassignCounselor(1L, 2L, 3L);

        // then
        verify(mockAssignment).endAssignment(); // Mock 객체의 메서드 호출 검증
    }

    @Test
    @DisplayName("배정 해제 실패 - 활성화된 배정 정보가 없음")
    void unassignCounselor_Fail_NotFound() {
        // given
        given(assignmentRepository.findByCounselorAndElderlyAndStatus(1L, 2L, AssignmentStatus.ACTIVE))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> assignmentService.unassignCounselor(1L, 2L, 3L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("현재 활성화된 배정 정보가 없습니다.");
    }

    @Test
    @DisplayName("상담사별 배정 목록 조회 성공")
    void getAssignmentsByCounselor_Success() {
        // given
        given(assignmentRepository.findAllActiveByCounselorId(1L))
                .willReturn(List.of(mockAssignment));

        // when
        List<AssignmentResponse> responses = assignmentService.getAssignmentsByCounselor(1L);

        // then
        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getCounselorName()).isEqualTo("김상담");
    }

    @Test
    @DisplayName("어르신별 담당 상담사 조회 성공")
    void getAssignmentByElderly_Success() {
        // given
        given(assignmentRepository.findActiveByElderlyId(2L))
                .willReturn(Optional.of(mockAssignment));

        // when
        AssignmentResponse response = assignmentService.getAssignmentByElderly(2L);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getElderlyName()).isEqualTo("이노인");
    }
}