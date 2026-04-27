package com.aicc.silverlink.domain.policy.controller;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.policy.dto.PolicyRequest;
import com.aicc.silverlink.domain.policy.entity.Policy;
import com.aicc.silverlink.domain.policy.entity.PolicyType;
import com.aicc.silverlink.domain.policy.repository.PolicyRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.domain.audit.service.AuditLogService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("ci") //
class PolicyControllerTest {

    @Autowired
    private MockMvc mockMvc;
    @Autowired
    private ObjectMapper objectMapper;
    @Autowired
    private PolicyRepository policyRepository;
    @Autowired
    private AdminRepository adminRepository;
    @Autowired
    private UserRepository userRepository;
    @Autowired
    private AdministrativeDivisionRepository divisionRepository;

    @MockitoBean
    private AuditLogService auditLogService;

    private User adminUser;

    @BeforeEach
    void setUp() {
        // 1. 행정구역 생성 (H2 MySQL 모드 호환 및 날짜 강제 주입)
        AdministrativeDivision division = AdministrativeDivision.builder()
                .admCode(1100000000L)
                .sidoCode("11")
                .sidoName("서울특별시")
                .level(AdministrativeDivision.DivisionLevel.SIDO)
                .build();

        // 💡 JPA Auditing 에러 방지를 위한 날짜 수동 주입
        ReflectionTestUtils.setField(division, "createdAt", LocalDateTime.now());
        ReflectionTestUtils.setField(division, "updatedAt", LocalDateTime.now());
        divisionRepository.save(division);

        // 2. 관리자용 User 생성
        adminUser = User.createLocal(
                "policy_admin_" + System.currentTimeMillis(),
                "password123",
                "정책관리자",
                "010-1111-2222",
                "policy@test.com",
                Role.ADMIN,
                null);
        userRepository.save(adminUser);

        // 3. Admin 엔티티 생성 (PolicyService.create에서 검증하므로 필수!)
        Admin testAdmin = Admin.builder()
                .user(adminUser)
                .administrativeDivision(division)
                .adminLevel(AdminLevel.NATIONAL)
                .build();
        adminRepository.save(testAdmin);
    }

    /**
     * 💡 핵심: @AuthenticationPrincipal Long 에 Long 타입 ID를 정확히 전달하기 위한 헬퍼
     */
    private UsernamePasswordAuthenticationToken getAdminAuth() {
        return new UsernamePasswordAuthenticationToken(
                adminUser.getId(),
                null,
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")));
    }

    @Nested
    @DisplayName("약관 조회 테스트")
    class GetPolicyTests {
        @Test
        @DisplayName("성공: 특정 타입의 최신 약관을 조회한다 (로그인 불필요)")
        void getLatestPolicy_Success() throws Exception {
            // given
            policyRepository.save(Policy.create(PolicyType.TERMS_OF_SERVICE, "v1.0", "내용", true, "설명", adminUser));

            // when & then
            mockMvc.perform(get("/api/policies/latest/{type}", PolicyType.TERMS_OF_SERVICE))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.version").value("v1.0"));
        }

        @Test
        @DisplayName("실패: 등록된 약관이 없는 타입을 조회하면 에러를 반환한다")
        void getLatestPolicy_NotFound() throws Exception {
            mockMvc.perform(get("/api/policies/latest/{type}", PolicyType.PRIVACY_POLICY))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("약관 생성 테스트")
    class CreatePolicyTests {
        @Test
        @DisplayName("성공: 관리자가 새로운 약관을 등록한다")
        void createPolicy_Success() throws Exception {
            // given
            PolicyRequest request = new PolicyRequest();
            ReflectionTestUtils.setField(request, "policyType", PolicyType.PRIVACY_POLICY);
            ReflectionTestUtils.setField(request, "version", "v2.0");
            ReflectionTestUtils.setField(request, "content", "새로운 개인정보 처리방침");
            ReflectionTestUtils.setField(request, "isMandatory", true);

            // when
            ResultActions result = mockMvc.perform(post("/api/policies")
                    .with(authentication(getAdminAuth())) // 💡 인증 정보(Long ID) 주입
                    .contentType(MediaType.APPLICATION_JSON)
                    .content(objectMapper.writeValueAsString(request)));

            // then
            result.andDo(print())
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.version").value("v2.0"));
        }

        @Test
        @DisplayName("실패: 이미 존재하는 버전으로 등록 시도 시 400 에러를 반환한다")
        void createPolicy_Duplicate() throws Exception {
            // given
            policyRepository.save(Policy.create(PolicyType.TERMS_OF_SERVICE, "v1.0", "기존 내용", true, "설명", adminUser));

            PolicyRequest request = new PolicyRequest();
            ReflectionTestUtils.setField(request, "policyType", PolicyType.TERMS_OF_SERVICE);
            ReflectionTestUtils.setField(request, "version", "v1.0"); // 중복 버전
            ReflectionTestUtils.setField(request, "content", "중복 내용");
            ReflectionTestUtils.setField(request, "isMandatory", true);

            // when & then
            mockMvc.perform(post("/api/policies")
                            .with(authentication(getAdminAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(objectMapper.writeValueAsString(request)))
                    .andDo(print())
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("입력값 검증 테스트")
    class ValidationTests {
        @Test
        @DisplayName("실패: 필수 파라미터(버전 등)가 누락되면 400 에러를 반환한다")
        void createPolicy_InvalidRequest() throws Exception {
            // given (version 누락)
            String json = """
                    {
                        "policyType": "TERMS_OF_SERVICE",
                        "content": "내용만 있음",
                        "isMandatory": true
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/policies")
                            .with(authentication(getAdminAuth()))
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(json))
                    .andExpect(status().isBadRequest());
        }
    }
}