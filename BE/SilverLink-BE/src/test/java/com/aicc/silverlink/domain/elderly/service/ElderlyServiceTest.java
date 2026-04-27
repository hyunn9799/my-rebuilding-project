package com.aicc.silverlink.domain.elderly.service;

import com.aicc.silverlink.domain.assignment.entity.Assignment;
import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope;
import com.aicc.silverlink.domain.consent.repository.AccessRequestRepository;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.service.CounselorService;
import com.aicc.silverlink.domain.elderly.dto.request.ElderlyCreateRequest;
import com.aicc.silverlink.domain.elderly.dto.request.ElderlyUpdateRequest;
import com.aicc.silverlink.domain.elderly.dto.request.HealthInfoUpdateRequest;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlyAdminDetailResponse;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlySummaryResponse;
import com.aicc.silverlink.domain.elderly.dto.response.HealthInfoResponse;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.entity.ElderlyHealthInfo;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.elderly.repository.HealthInfoRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ElderlyServiceTest {

    @InjectMocks
    private ElderlyService elderlyService;

    @Mock
    private ElderlyRepository elderlyRepo;
    @Mock
    private HealthInfoRepository healthRepo;
    @Mock
    private UserRepository userRepo;
    @Mock
    private AdministrativeDivisionRepository divisionRepository;
    @Mock
    private GuardianElderlyRepository guardianElderlyRepo;
    @Mock
    private AssignmentRepository assignmentRepo;
    @Mock
    private CounselorService counselorService;
    @Mock
    private AccessRequestRepository accessRequestRepo; // 누락된 Mock 추가

    private AdministrativeDivision division;
    private User elderlyUser;

    @BeforeEach
    void setUp() {
        division = AdministrativeDivision.builder()
                .admCode(11110L).sidoName("서울시").build();

        elderlyUser = User.createLocal("elder01", "pw", "이노인", "01011112222", null, Role.ELDERLY, null);
        ReflectionTestUtils.setField(elderlyUser, "id", 10L);
    }

    @Test
    @DisplayName("성공: 어르신 등록 시 행정구역 및 유저 정보가 정상 매핑된다")
    void createElderly() {
        // given
        ElderlyCreateRequest req = new ElderlyCreateRequest(10L, 11110L, LocalDate.of(1950, 1, 1), Elderly.Gender.M,
                "주소1", "주소2", "123", null, null, null);
        given(userRepo.findById(10L)).willReturn(Optional.of(elderlyUser));
        given(divisionRepository.findById(11110L)).willReturn(Optional.of(division));

        // 💡 thenAnswer 대신 BDD 스타일인 willAnswer로 수정
        given(elderlyRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        ElderlySummaryResponse response = elderlyService.createElderly(req);

        // then
        assertThat(response.name()).isEqualTo("이노인");
        assertThat(response.admCode()).isEqualTo(11110L);
        verify(elderlyRepo, times(1)).save(any());
    }

    @Test
    @DisplayName("성공: 관리자가 전체 어르신 목록을 조회한다")
    void getAllElderlyForAdmin() {
        // given
        Elderly elderly = Elderly.create(elderlyUser, division, LocalDate.of(1950, 1, 1), Elderly.Gender.M);
        given(elderlyRepo.findAllWithUserAndDivision()).willReturn(List.of(elderly));

        // when
        List<ElderlySummaryResponse> result = elderlyService.getAllElderlyForAdmin();

        // then
        assertThat(result).hasSize(1);
        assertThat(result.get(0).name()).isEqualTo("이노인");
    }

    @Test
    @DisplayName("성공: 관리자가 어르신 통합 상세 정보를 조회한다(보호자/상담사 포함)")
    void getElderlyDetailForAdmin() {
        // given
        Long eId = 10L;
        Elderly elderly = Elderly.create(elderlyUser, division, LocalDate.of(1950, 1, 1), Elderly.Gender.M);
        given(elderlyRepo.findWithUserById(eId)).willReturn(Optional.of(elderly));
        given(guardianElderlyRepo.findByElderlyId(eId)).willReturn(Optional.empty());
        given(assignmentRepo.findActiveByElderlyId(eId)).willReturn(Optional.empty());

        // when
        ElderlyAdminDetailResponse result = elderlyService.getElderlyDetailForAdmin(eId);

        // then
        assertThat(result.elderly().name()).isEqualTo("이노인");
        assertThat(result.guardian()).isNull();
        assertThat(result.counselor()).isNull();
    }

    @Test
    @DisplayName("성공: 담당 상담사가 어르신의 건강 정보를 조회한다")
    void getHealthInfo_Success() {
        // given
        Long counselorId = 1L;
        Long eId = 10L;
        User counselorUser = User.createLocal("c1", "p", "상담사", "010", null, Role.COUNSELOR, null);
        ElderlyHealthInfo hi = ElderlyHealthInfo.create(mock(Elderly.class));

        given(userRepo.findById(counselorId)).willReturn(Optional.of(counselorUser));
        given(assignmentRepo.existsByCounselor_IdAndElderly_IdAndStatus(counselorId, eId, AssignmentStatus.ACTIVE))
                .willReturn(true);
        given(healthRepo.findById(eId)).willReturn(Optional.of(hi));

        // when
        HealthInfoResponse result = elderlyService.getHealthInfo(counselorId, eId);

        // then
        assertThat(result).isNotNull();
    }

    @Test
    @DisplayName("실패: 담당이 아닌 상담사가 어르신 건강 정보 조회 시 예외 발생 (IDOR 방어)")
    void getHealthInfo_Fail_Forbidden() {
        // given
        Long counselorId = 1L;
        Long eId = 10L;
        User counselorUser = User.createLocal("c1", "p", "상담사", "010", null, Role.COUNSELOR, null);

        given(userRepo.findById(counselorId)).willReturn(Optional.of(counselorUser));
        given(assignmentRepo.existsByCounselor_IdAndElderly_IdAndStatus(counselorId, eId, AssignmentStatus.ACTIVE))
                .willReturn(false);

        // when & then
        assertThatThrownBy(() -> elderlyService.getHealthInfo(counselorId, eId))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("성공: 건강 정보를 등록하거나 수정한다(Upsert)")
    void upsertHealthInfo() {
        // given
        Long adminId = 999L;
        Long eId = 10L;
        User admin = User.createLocal("admin", "p", "관", "010", null, Role.ADMIN, null);
        Elderly elderly = Elderly.create(elderlyUser, division, LocalDate.of(1950, 1, 1), Elderly.Gender.M);
        HealthInfoUpdateRequest req = new HealthInfoUpdateRequest("당뇨", "양호", "특이사항");

        given(userRepo.findById(adminId)).willReturn(Optional.of(admin));
        given(elderlyRepo.findById(eId)).willReturn(Optional.of(elderly));
        given(healthRepo.findById(eId)).willReturn(Optional.empty());

        // 💡 willAnswer로 수정
        given(healthRepo.save(any())).willAnswer(invocation -> invocation.getArgument(0));

        // when
        HealthInfoResponse result = elderlyService.upsertHealthInfo(adminId, eId, req);

        // then
        assertThat(result.chronicDiseases()).isEqualTo("당뇨");
        verify(healthRepo, times(1)).save(any());
    }

    @Test
    @DisplayName("성공: 어르신 프로필 및 유저 정보 수정 (더티 체킹)")
    void updateElderlyProfile() {
        // given
        Elderly elderly = Elderly.create(elderlyUser, division, LocalDate.of(1950, 1, 1), Elderly.Gender.M);
        ElderlyUpdateRequest req = new ElderlyUpdateRequest("이름수정", "01099998888", "새주소", "상세", "555", null, null, null,
                null);
        given(elderlyRepo.findWithUserById(10L)).willReturn(Optional.of(elderly));

        // when
        elderlyService.updateElderlyProfile(10L, req);

        // then
        assertThat(elderly.getUser().getName()).isEqualTo("이름수정");
        assertThat(elderly.getAddressLine1()).isEqualTo("새주소");
    }

    @Test
    @DisplayName("성공: 어르신 탈퇴 시 배정은 종료되고 관계는 삭제되며 유저는 Soft Delete 된다")
    void withdrawElderly() {
        // given
        Long eId = 10L;
        Elderly elderly = Elderly.create(elderlyUser, division, LocalDate.of(1950, 1, 1), Elderly.Gender.M);
        Assignment assignment = mock(Assignment.class);

        given(elderlyRepo.findById(eId)).willReturn(Optional.of(elderly));
        given(assignmentRepo.findActiveByElderlyId(eId)).willReturn(Optional.of(assignment));

        // when
        elderlyService.withdrawElderly(eId);

        // then
        verify(assignment, times(1)).endAssignment();
        verify(guardianElderlyRepo, times(1)).deleteByElderlyId(eId);
        assertThat(elderly.getUser().getStatus()).isEqualTo(UserStatus.DELETED);
    }
}