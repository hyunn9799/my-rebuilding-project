package com.aicc.silverlink.domain.counselor.service;

import com.aicc.silverlink.domain.counselor.dto.CounselorRequest;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.dto.CounselorUpdateRequest;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CounselorServiceTest {

    @InjectMocks
    private CounselorService counselorService;

    @Mock
    private CounselorRepository counselorRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    @Mock
    private AdministrativeDivisionRepository divisionRepository;
    @Mock
    private com.aicc.silverlink.domain.assignment.repository.AssignmentRepository assignmentRepository;

    private AdministrativeDivision division;

    @BeforeEach
    void setUp() {
        // 공통으로 사용할 행정구역 더미 데이터
        division = AdministrativeDivision.builder()
                .admCode(1111051500L)
                .sidoName("서울특별시")
                .sigunguName("종로구")
                .dongName("청운효자동")
                .build();
    }

    private User createDummyUser(Long id, String loginId, String name) {
        User user = User.createLocal(
                loginId, "encodedPw", name, "010-1234-5678", loginId + "@email.com", Role.COUNSELOR, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Counselor createDummyCounselor(User user, AdministrativeDivision division) {
        Counselor counselor = Counselor.create(user, "2024001", "복지팀", "02-123-4567", LocalDate.now(), division);
        ReflectionTestUtils.setField(counselor, "id", user.getId());
        return counselor;
    }

    // --- [테스트 케이스 1: 등록] ---

    @Test
    @DisplayName("상담사 등록 성공")
    void register_success() {
        // given
        CounselorRequest request = new CounselorRequest(
                "counselor1", "pass1234", "김상담", "test@email.com",
                "010-1234-5678", "2024001", "복지팀", "02-123-4567",
                LocalDate.now(), 1111051500L);

        given(userRepository.existsByLoginId(request.getLoginId())).willReturn(false);
        given(divisionRepository.findById(request.getAdmCode())).willReturn(Optional.of(division));
        given(passwordEncoder.encode(request.getPassword())).willReturn("encodedPw");

        // when
        CounselorResponse response = counselorService.register(request);

        // then
        assertThat(response).isNotNull();
        assertThat(response.getLoginId()).isEqualTo(request.getLoginId());
        verify(userRepository, times(1)).save(any(User.class));
        verify(counselorRepository, times(1)).save(any(Counselor.class));
    }

    @Test
    @DisplayName("상담사 등록 실패 - 아이디 중복")
    void register_fail_duplicate_id() {
        // given
        CounselorRequest request = CounselorRequest.builder().loginId("duplicateId").build();
        given(userRepository.existsByLoginId("duplicateId")).willReturn(true);

        // when & then
        assertThatThrownBy(() -> counselorService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("이미 사용 중인 아이디입니다.");
    }

    // --- [테스트 케이스 2: 수정] ---

    @Test
    @DisplayName("상담사 수정 성공 - 더티 체킹으로 정보 업데이트")
    void updateCounselor_success() {
        // given
        Long counselorId = 1L;
        User user = createDummyUser(counselorId, "counselor1", "김상담");
        Counselor counselor = createDummyCounselor(user, division);

        CounselorUpdateRequest updateReq = new CounselorUpdateRequest();
        ReflectionTestUtils.setField(updateReq, "name", "이름수정");
        ReflectionTestUtils.setField(updateReq, "phone", "010-9999-9999");
        ReflectionTestUtils.setField(updateReq, "email", "new@test.com");
        ReflectionTestUtils.setField(updateReq, "department", "기획팀");
        ReflectionTestUtils.setField(updateReq, "officePhone", "02-111-2222");

        given(counselorRepository.findByIdWithUser(counselorId)).willReturn(Optional.of(counselor));

        // when
        CounselorResponse response = counselorService.updateCounselor(counselorId, updateReq);

        // then
        assertThat(response.getName()).isEqualTo("이름수정");
        assertThat(response.getPhone()).isEqualTo("01099999999"); // normalizePhone 검증
        assertThat(response.getDepartment()).isEqualTo("기획팀");
        assertThat(counselor.getUser().getEmail()).isEqualTo("new@test.com");
    }

    // --- [테스트 케이스 3: 조회] ---

    @Test
    @DisplayName("상담사 상세 조회 성공")
    void getCounselor_success() {
        // given
        Long counselorId = 1L;
        User user = createDummyUser(counselorId, "counselor1", "김상담");
        Counselor counselor = createDummyCounselor(user, division);

        given(counselorRepository.findByIdWithUser(counselorId)).willReturn(Optional.of(counselor));
        given(assignmentRepository.countActiveByCounselorId(any())).willReturn(5);

        // when
        CounselorResponse response = counselorService.getCounselor(counselorId);

        // then
        assertThat(response.getId()).isEqualTo(counselorId);
        assertThat(response.getName()).isEqualTo("김상담");
        assertThat(response.getSidoName()).isEqualTo("서울특별시");
    }

    @Test
    @DisplayName("상담사 상세 조회 실패 - 존재하지 않는 ID")
    void getCounselor_fail_not_found() {
        // given
        given(counselorRepository.findByIdWithUser(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> counselorService.getCounselor(999L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("해당 상담사를 찾을 수 없습니다");
    }

    @Test
    @DisplayName("상담사 전체 목록 조회 성공")
    void getAllCounselors_success() {
        // given
        User u1 = createDummyUser(1L, "c1", "상담1");
        User u2 = createDummyUser(2L, "c2", "상담2");
        given(counselorRepository.findAllWithUser()).willReturn(List.of(
                createDummyCounselor(u1, division),
                createDummyCounselor(u2, division)));
        given(assignmentRepository.countActiveByCounselorId(any())).willReturn(0);

        // when
        List<CounselorResponse> list = counselorService.getAllCounselors();

        // then
        assertThat(list).hasSize(2);
        assertThat(list.get(0).getName()).isEqualTo("상담1");
        assertThat(list.get(1).getName()).isEqualTo("상담2");
    }
}