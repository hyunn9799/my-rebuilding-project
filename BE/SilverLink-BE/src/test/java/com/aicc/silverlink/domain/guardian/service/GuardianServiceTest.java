package com.aicc.silverlink.domain.guardian.service;

import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.guardian.dto.*;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class GuardianServiceTest {

    @InjectMocks
    private GuardianService guardianService;
    @Mock
    private GuardianRepository guardianRepository;
    @Mock
    private GuardianElderlyRepository guardianElderlyRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ElderlyRepository elderlyRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    // --- 헬퍼 메소드: 테스트용 객체 생성 ---

    private User createTestUser(Long id, String name, Role role) {
        User user = User.createLocal("testId", "hash", name, "01011112222", "test@test.com", role, null);
        ReflectionTestUtils.setField(user, "id", id);
        return user;
    }

    private Guardian createTestGuardian(Long id, String name) {
        User user = createTestUser(id, name, Role.GUARDIAN);
        Guardian guardian = Guardian.builder().user(user).build();
        ReflectionTestUtils.setField(guardian, "id", id);
        return guardian;
    }

    @Nested
    @DisplayName("보호자 등록 및 조회 테스트")
    class BasicOperation {

        @Test
        @DisplayName("성공: 보호자 회원가입 시 유저와 보호자 정보가 모두 저장된다")
        void register_Success() {
            // given
            // 💡 팩토리 메서드나 빌더에서 필수 값(phone, password 등)을 누락하지 않도록 주의해야 합니다.
            GuardianRequest request = GuardianRequest.builder()
                    .loginId("newGuardian")
                    .password("rawPass")
                    .name("박보호")
                    .phone("010-1111-2222") // 👈 필수값 추가
                    .email("test@test.com")
                    .build();

            given(userRepository.existsByLoginId(any())).willReturn(false);
            given(passwordEncoder.encode(any())).willReturn("encodedPass");

            // when
            guardianService.register(request);

            // then
            verify(userRepository, times(1)).save(any(User.class));
            verify(guardianRepository, times(1)).save(any(Guardian.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 아이디로 가입 시도")
        void register_Fail_DuplicateId() {
            // given
            given(userRepository.existsByLoginId(any())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> guardianService.register(GuardianRequest.builder().loginId("dup").build()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 사용 중인 아이디");
        }
    }

    @Nested
    @DisplayName("수정 및 탈퇴 로직 테스트")
    class UpdateAndWithdrawTests {

        @Test
        @DisplayName("성공: 보호자 정보 수정 (더티 체킹 확인)")
        void updateGuardianProfile_Success() {
            // given
            Long gId = 1L;
            Guardian guardian = createTestGuardian(gId, "이전이름");
            GuardianUpdateRequest updateReq = new GuardianUpdateRequest(
                    "수정이름", "01099998888", "new@test.com", "서울", "상세", "123");

            given(guardianRepository.findByIdWithUser(gId)).willReturn(Optional.of(guardian));

            // when
            guardianService.updateGuardianProfile(gId, updateReq);

            // then
            assertThat(guardian.getUser().getName()).isEqualTo("수정이름");
            assertThat(guardian.getUser().getPhone()).isEqualTo("01099998888");
            assertThat(guardian.getAddressLine1()).isEqualTo("서울");
        }

        @Test
        @DisplayName("성공: 보호자 탈퇴 시 관계 데이터는 즉시 삭제되고 유저는 Soft Delete 된다")
        void withdrawGuardian_Success() {
            // given
            Long gId = 1L;
            Guardian guardian = createTestGuardian(gId, "탈퇴자");
            given(guardianRepository.findById(gId)).willReturn(Optional.of(guardian));

            // when
            guardianService.withdrawGuardian(gId);

            // then
            // 1. 관계 데이터가 삭제되었는지 확인 (Hard Delete)
            verify(guardianElderlyRepository, times(1)).deleteByGuardianId(gId);
            // 2. 유저 상태가 DELETED로 변했는지 확인 (Soft Delete)
            assertThat(guardian.getUser().getStatus()).isEqualTo(UserStatus.DELETED);
            assertThat(guardian.getUser().getDeletedAt()).isNotNull();
        }
    }

    @Nested
    @DisplayName("상담사 권한 검증 테스트")
    class CounselorAuthTests {

        @Test
        @DisplayName("성공: 담당 어르신의 보호자일 경우 상세 정보 반환")
        void getGuardianForCounselor_Success() {
            // given
            Long gId = 1L;
            Long cId = 100L;
            Long eId = 2L;
            Guardian guardian = createTestGuardian(gId, "보호자A");

            Elderly elderly = Elderly.builder().build();
            ReflectionTestUtils.setField(elderly, "id", eId);

            GuardianElderly relation = GuardianElderly.builder().elderly(elderly).build();

            given(guardianRepository.findByIdWithUser(gId)).willReturn(Optional.of(guardian));
            given(guardianElderlyRepository.findByGuardianId(gId)).willReturn(Optional.of(relation));
            given(assignmentRepository.existsByCounselor_IdAndElderly_IdAndStatus(cId, eId, AssignmentStatus.ACTIVE))
                    .willReturn(true);

            // when
            GuardianResponse result = guardianService.getGuardianForCounselor(gId, cId);

            // then
            assertThat(result.getName()).isEqualTo("보호자A");
            assertThat(result.getId()).isEqualTo(gId);
        }

        @Test
        @DisplayName("실패: 상담사가 담당하지 않는 어르신의 보호자 조회 시 에러")
        void getGuardianForCounselor_Fail_NotAssigned() {
            // given
            Long gId = 1L;
            Long cId = 100L;
            Long eId = 999L;
            Guardian guardian = createTestGuardian(gId, "보호자A");

            Elderly elderly = Elderly.builder().build();
            ReflectionTestUtils.setField(elderly, "id", eId);

            GuardianElderly relation = GuardianElderly.builder().elderly(elderly).build();

            given(guardianRepository.findByIdWithUser(gId)).willReturn(Optional.of(guardian));
            given(guardianElderlyRepository.findByGuardianId(gId)).willReturn(Optional.of(relation));
            given(assignmentRepository.existsByCounselor_IdAndElderly_IdAndStatus(cId, eId, AssignmentStatus.ACTIVE))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> guardianService.getGuardianForCounselor(gId, cId))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("본인이 담당하는 어르신");
        }
    }

    @Nested
    @DisplayName("어르신 연결 테스트")
    class ConnectionTests {

        @Test
        @DisplayName("실패: 이미 다른 보호자와 연결된 어르신은 연결 불가")
        void connectElderly_Fail_AlreadyConnected() {
            // given
            given(guardianElderlyRepository.existsByElderly_Id(any())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> guardianService.connectElderly(1L, 2L, RelationType.CHILD))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 다른 보호자가 등록");
        }
    }
}