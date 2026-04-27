package com.aicc.silverlink.domain.policy.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.audit.service.AuditLogService;
import com.aicc.silverlink.domain.policy.dto.PolicyRequest;
import com.aicc.silverlink.domain.policy.dto.PolicyResponse;
import com.aicc.silverlink.domain.policy.entity.Policy;
import com.aicc.silverlink.domain.policy.entity.PolicyType;
import com.aicc.silverlink.domain.policy.repository.PolicyRepository;
import com.aicc.silverlink.domain.user.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
// 💡 불필요한 stubbing 에러를 방지하기 위해 LENIENT 설정을 추가합니다.
// 실무에서는 더 정확한 stubbing을 권장하지만, 테스트 픽스처가 공통으로 쓰일 때 유용합니다.
@MockitoSettings(strictness = Strictness.LENIENT)
class PolicyServiceTest {

    @InjectMocks
    private PolicyService policyService;

    @Mock
    private PolicyRepository policyRepository;

    @Mock
    private AdminRepository adminRepository;

    @Mock
    private AuditLogService auditLogService;

    // --- 테스트용 픽스처 생성 헬퍼 ---
    private User createMockUser() {
        User user = mock(User.class);
        // 💡 주의: getName() stubbing은 실제 사용하는 테스트에서만 하거나
        // LENIENT 설정을 통해 예외를 방지합니다.
        return user;
    }

    private Admin createMockAdmin(User user) {
        Admin admin = mock(Admin.class);
        given(admin.getUser()).willReturn(user);
        return admin;
    }

    @Nested
    @DisplayName("약관 생성(create) 테스트")
    class CreateTests {

        @Test
        @DisplayName("성공: 중복되지 않은 버전과 유효한 관리자 ID면 약관이 생성된다")
        void create_Success() {
            // given
            Long adminId = 1L;
            User mockUser = createMockUser();
            Admin mockAdmin = createMockAdmin(mockUser);

            PolicyRequest request = new PolicyRequest();
            ReflectionTestUtils.setField(request, "policyType", PolicyType.TERMS_OF_SERVICE);
            ReflectionTestUtils.setField(request, "version", "v1.0");
            ReflectionTestUtils.setField(request, "content", "약관 내용");
            // 💡 [핵심수정] NPE 방지를 위해 필수 필드 주입
            ReflectionTestUtils.setField(request, "isMandatory", true);

            given(policyRepository.existsByPolicyTypeAndVersion(any(), any())).willReturn(false);
            given(adminRepository.findByIdWithUser(adminId)).willReturn(Optional.of(mockAdmin));

            // Policy.create() 내부에서 날짜 등이 초기화되므로 Mock보다는 실제 객체 생성을 활용
            Policy savedPolicy = Policy.create(PolicyType.TERMS_OF_SERVICE, "v1.0", "약관 내용", true, "설명", mockUser);
            ReflectionTestUtils.setField(savedPolicy, "id", 100L);

            given(policyRepository.save(any(Policy.class))).willReturn(savedPolicy);

            // when
            PolicyResponse response = policyService.create(request, adminId);

            // then
            assertThat(response.getId()).isEqualTo(100L);
            assertThat(response.getVersion()).isEqualTo("v1.0");
            verify(policyRepository, times(1)).save(any(Policy.class));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 버전이면 예외가 발생한다")
        void create_Fail_DuplicateVersion() {
            // given
            PolicyRequest request = new PolicyRequest();
            ReflectionTestUtils.setField(request, "policyType", PolicyType.TERMS_OF_SERVICE);
            ReflectionTestUtils.setField(request, "version", "v1.0");

            given(policyRepository.existsByPolicyTypeAndVersion(any(), any())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> policyService.create(request, 1L))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("이미 존재하는 정책 버전");
        }
    }

    @Nested
    @DisplayName("최신 약관 조회(getLatest) 테스트")
    class GetLatestTests {

        @Test
        @DisplayName("성공: 해당 타입의 약관이 존재하면 최신본을 반환한다")
        void getLatest_Success() {
            // given
            User mockUser = createMockUser();
            Policy policy = Policy.create(PolicyType.PRIVACY_POLICY, "v1.5", "내용", true, "설명", mockUser);

            given(policyRepository.findFirstByPolicyTypeOrderByCreatedAtDesc(PolicyType.PRIVACY_POLICY))
                    .willReturn(Optional.of(policy));

            // when
            PolicyResponse response = policyService.getLatest(PolicyType.PRIVACY_POLICY);

            // then
            assertThat(response.getVersion()).isEqualTo("v1.5");
            assertThat(response.getPolicyName()).isEqualTo(PolicyType.PRIVACY_POLICY.getDescription());
        }

        @Test
        @DisplayName("실패: 해당 타입의 약관이 하나도 없으면 예외가 발생한다")
        void getLatest_Fail_NotFound() {
            // given
            given(policyRepository.findFirstByPolicyTypeOrderByCreatedAtDesc(any()))
                    .willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> policyService.getLatest(PolicyType.LOCATION_BASED_SERVICE))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("해당 정책을 찾을 수 없습니다");
        }
    }
}