package com.aicc.silverlink.domain.admin.controller;

import com.aicc.silverlink.domain.admin.dto.request.AdminCreateRequest;
import com.aicc.silverlink.domain.admin.dto.request.AdminUpdateRequest;
import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision.DivisionLevel;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc(addFilters = false)
@Transactional
@ActiveProfiles("ci")
class AdminControllerTest {

        @Autowired
        private MockMvc mockMvc;

        @Autowired
        private ObjectMapper objectMapper;

        @Autowired
        private UserRepository userRepository;

        @Autowired
        private AdminRepository adminRepository;

        @Autowired
        private AdministrativeDivisionRepository divisionRepository; // [추가] 행정구역 저장용

        private User testUser;
        private User anotherUser;
        private Admin testAdmin;

        // 행정구역 픽스처
        private AdministrativeDivision seoul;
        private AdministrativeDivision gangnam;
        private AdministrativeDivision yeoksam;
        private AdministrativeDivision jongno;

        @BeforeEach
        void setUp() {
                // 1. 행정구역 데이터 생성 (FK 제약조건 충족을 위해 필수)
                seoul = divisionRepository.save(AdministrativeDivision.builder()
                                .admCode(1100000000L).sidoCode("11").sigunguCode("000").dongCode("000")
                                .sidoName("서울특별시").level(DivisionLevel.SIDO).build());

                gangnam = divisionRepository.save(AdministrativeDivision.builder()
                                .admCode(1168000000L).sidoCode("11").sigunguCode("680").dongCode("000")
                                .sidoName("서울특별시").sigunguName("강남구").level(DivisionLevel.SIGUNGU).build());

                yeoksam = divisionRepository.save(AdministrativeDivision.builder()
                                .admCode(1168010100L).sidoCode("11").sigunguCode("680").dongCode("101")
                                .sidoName("서울특별시").sigunguName("강남구").dongName("역삼1동").level(DivisionLevel.DONG)
                                .build());

                jongno = divisionRepository.save(AdministrativeDivision.builder()
                                .admCode(1111000000L).sidoCode("11").sigunguCode("110").dongCode("000")
                                .sidoName("서울특별시").sigunguName("종로구").level(DivisionLevel.SIGUNGU).build());

                // 2. 테스트용 User 생성
                testUser = User.createLocal(
                                "admin_test_" + System.currentTimeMillis(),
                                "encodedPassword123",
                                "테스트관리자",
                                "01012345678",
                                "admin@test.com",
                                Role.ADMIN,
                                null);
                userRepository.save(testUser);

                anotherUser = User.createLocal(
                                "admin_test2_" + System.currentTimeMillis(),
                                "encodedPassword456",
                                "테스트관리자2",
                                "01087654321",
                                "admin2@test.com",
                                Role.ADMIN,
                                null);
                userRepository.save(anotherUser);
        }

        @Nested
        @DisplayName("관리자 생성 API")
        class CreateAdmin {

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 관리자 생성")
                void createAdmin_Success() throws Exception {
                        // given
                        AdminCreateRequest request = new AdminCreateRequest(
                                        testUser.getId(),
                                        gangnam.getAdmCode(), // 1168000000L (DB에 존재하는 코드 사용)
                                        AdminLevel.CITY);

                        // when
                        ResultActions result = mockMvc.perform(post("/api/admins")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.userId").value(testUser.getId()))
                                        .andExpect(jsonPath("$.name").value("테스트관리자"))
                                        .andExpect(jsonPath("$.admCode").value(gangnam.getAdmCode())) // [수정]
                                                                                                      // admDongCode ->
                                                                                                      // admCode
                                        .andExpect(jsonPath("$.adminLevel").value("CITY"));
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: adminLevel 미지정 시 자동 결정")
                void createAdmin_AutoDetermineLevel() throws Exception {
                        // given - 서울시 코드 (시/도 레벨)
                        AdminCreateRequest request = new AdminCreateRequest(
                                        testUser.getId(),
                                        seoul.getAdmCode(),
                                        null);

                        // when
                        ResultActions result = mockMvc.perform(post("/api/admins")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isCreated())
                                        .andExpect(jsonPath("$.adminLevel").value("PROVINCIAL"));
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("실패: 존재하지 않는 사용자")
                void createAdmin_UserNotFound() throws Exception {
                        // given
                        AdminCreateRequest request = new AdminCreateRequest(
                                        999999L,
                                        gangnam.getAdmCode(),
                                        AdminLevel.CITY);

                        // when
                        ResultActions result = mockMvc.perform(post("/api/admins")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("실패: 이미 관리자로 등록된 사용자")
                void createAdmin_AlreadyRegistered() throws Exception {
                        // given - 먼저 관리자로 등록
                        Admin admin = Admin.builder()
                                        .user(testUser)
                                        .administrativeDivision(gangnam) // [수정] Entity 주입
                                        .adminLevel(AdminLevel.CITY)
                                        .build();
                        adminRepository.save(admin);

                        AdminCreateRequest request = new AdminCreateRequest(
                                        testUser.getId(),
                                        gangnam.getAdmCode(),
                                        AdminLevel.CITY);

                        // when
                        ResultActions result = mockMvc.perform(post("/api/admins")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("관리자 조회 API")
        class GetAdmin {

                @BeforeEach
                void setUpAdmin() {
                        testAdmin = Admin.builder()
                                        .user(testUser)
                                        .administrativeDivision(gangnam) // [수정]
                                        .adminLevel(AdminLevel.CITY)
                                        .build();
                        adminRepository.save(testAdmin);
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 관리자 단건 조회")
                void getAdmin_Success() throws Exception {
                        // when
                        ResultActions result = mockMvc.perform(get("/api/admins/{userId}", testUser.getId()));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.userId").value(testUser.getId()))
                                        .andExpect(jsonPath("$.name").value("테스트관리자"))
                                        .andExpect(jsonPath("$.admCode").value(gangnam.getAdmCode()))
                                        .andExpect(jsonPath("$.adminLevel").value("CITY"));
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("실패: 존재하지 않는 관리자 조회")
                void getAdmin_NotFound() throws Exception {
                        // when
                        ResultActions result = mockMvc.perform(get("/api/admins/{userId}", 999999L));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("관리자 목록 조회 API")
        class GetAdmins {

                @BeforeEach
                void setUpAdmins() {
                        // 첫 번째 관리자 (시/군/구 레벨 - 강남구)
                        Admin admin1 = Admin.builder()
                                        .user(testUser)
                                        .administrativeDivision(gangnam)
                                        .adminLevel(AdminLevel.CITY)
                                        .build();
                        adminRepository.save(admin1);

                        // 두 번째 관리자 (시/도 레벨 - 서울시)
                        Admin admin2 = Admin.builder()
                                        .user(anotherUser)
                                        .administrativeDivision(seoul)
                                        .adminLevel(AdminLevel.PROVINCIAL)
                                        .build();
                        adminRepository.save(admin2);
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 전체 관리자 목록 조회")
                void getAllAdmins_Success() throws Exception {
                        // when
                        ResultActions result = mockMvc.perform(get("/api/admins"));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$", hasSize(greaterThanOrEqualTo(2))));
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 행정동 코드로 필터링")
                void getAdminsByAdmCode_Success() throws Exception {
                        // when
                        ResultActions result = mockMvc.perform(get("/api/admins")
                                        .param("admCode", String.valueOf(gangnam.getAdmCode())));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$[0].admCode").value(gangnam.getAdmCode()));
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 관리자 레벨로 필터링")
                void getAdminsByLevel_Success() throws Exception {
                        // when
                        ResultActions result = mockMvc.perform(get("/api/admins")
                                        .param("level", "PROVINCIAL"));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$[0].adminLevel").value("PROVINCIAL"));
                }
        }

        @Nested
        @DisplayName("하위 관리자 조회 API")
        class GetSubordinates {

                @BeforeEach
                void setUpHierarchy() {
                        // 시/군/구 레벨 관리자
                        testAdmin = Admin.builder()
                                        .user(testUser)
                                        .administrativeDivision(gangnam)
                                        .adminLevel(AdminLevel.CITY)
                                        .build();
                        adminRepository.save(testAdmin);
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 하위 관리자 목록 조회")
                void getSubordinates_Success() throws Exception {
                        // when
                        ResultActions result = mockMvc.perform(
                                        get("/api/admins/{userId}/subordinates", testUser.getId()));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isOk());
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("실패: 존재하지 않는 관리자의 하위 조회")
                void getSubordinates_AdminNotFound() throws Exception {
                        // when
                        ResultActions result = mockMvc.perform(
                                        get("/api/admins/{userId}/subordinates", 999999L));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("권한 확인 API")
        class CheckJurisdiction {

                @BeforeEach
                void setUpAdmin() {
                        // 강남구 관리자
                        testAdmin = Admin.builder()
                                        .user(testUser)
                                        .administrativeDivision(gangnam)
                                        .adminLevel(AdminLevel.CITY)
                                        .build();
                        adminRepository.save(testAdmin);
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 관할 구역 내 - true 반환")
                void checkJurisdiction_HasJurisdiction() throws Exception {
                        // when - 강남구 관리자가 역삼동(강남구 하위)에 대한 권한 확인
                        ResultActions result = mockMvc.perform(
                                        get("/api/admins/{userId}/jurisdiction", testUser.getId())
                                                        .param("targetCode", String.valueOf(yeoksam.getAdmCode())));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("true"));
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 관할 구역 외 - false 반환")
                void checkJurisdiction_NoJurisdiction() throws Exception {
                        // when - 강남구 관리자가 종로구에 대한 권한 확인
                        ResultActions result = mockMvc.perform(
                                        get("/api/admins/{userId}/jurisdiction", testUser.getId())
                                                        .param("targetCode", String.valueOf(jongno.getAdmCode())));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(content().string("false"));
                }
        }

        @Nested
        @DisplayName("관리자 수정 API")
        class UpdateAdmin {

                @BeforeEach
                void setUpAdmin() {
                        testAdmin = Admin.builder()
                                        .user(testUser)
                                        .administrativeDivision(gangnam)
                                        .adminLevel(AdminLevel.CITY)
                                        .build();
                        adminRepository.save(testAdmin);
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 담당 구역 변경")
                void updateAdmin_Success() throws Exception {
                        // given - 종로구로 변경
                        AdminUpdateRequest request = new AdminUpdateRequest(jongno.getAdmCode());

                        // when
                        ResultActions result = mockMvc.perform(put("/api/admins/{userId}", testUser.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.admCode").value(jongno.getAdmCode()))
                                        .andExpect(jsonPath("$.adminLevel").value("CITY"));
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 구역 변경 시 레벨 자동 재계산")
                void updateAdmin_LevelRecalculated() throws Exception {
                        // given - 서울시(시/도 레벨)로 변경
                        AdminUpdateRequest request = new AdminUpdateRequest(seoul.getAdmCode());

                        // when
                        ResultActions result = mockMvc.perform(put("/api/admins/{userId}", testUser.getId())
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isOk())
                                        .andExpect(jsonPath("$.admCode").value(seoul.getAdmCode()))
                                        .andExpect(jsonPath("$.adminLevel").value("PROVINCIAL"));
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("실패: 존재하지 않는 관리자 수정")
                void updateAdmin_NotFound() throws Exception {
                        // given
                        AdminUpdateRequest request = new AdminUpdateRequest(jongno.getAdmCode());

                        // when
                        ResultActions result = mockMvc.perform(put("/api/admins/{userId}", 999999L)
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(objectMapper.writeValueAsString(request)));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("관리자 삭제 API")
        class DeleteAdmin {

                @BeforeEach
                void setUpAdmin() {
                        testAdmin = Admin.builder()
                                        .user(testUser)
                                        .administrativeDivision(gangnam)
                                        .adminLevel(AdminLevel.CITY)
                                        .build();
                        adminRepository.save(testAdmin);
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("성공: 관리자 삭제")
                void deleteAdmin_Success() throws Exception {
                        // when
                        ResultActions result = mockMvc.perform(
                                        delete("/api/admins/{userId}", testUser.getId()));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isNoContent());

                        // 삭제 확인
                        mockMvc.perform(get("/api/admins/{userId}", testUser.getId()))
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("실패: 존재하지 않는 관리자 삭제")
                void deleteAdmin_NotFound() throws Exception {
                        // when
                        ResultActions result = mockMvc.perform(delete("/api/admins/{userId}", 999999L));

                        // then
                        result.andDo(print())
                                        .andExpect(status().isBadRequest());
                }
        }

        @Nested
        @DisplayName("입력값 검증")
        class ValidationTests {

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("실패: userId가 null인 경우")
                void createAdmin_NullUserId() throws Exception {
                        // given
                        String requestJson = """
                                        {
                                            "userId": null,
                                            "admCode": 1168000000,
                                            "adminLevel": "CITY"
                                        }
                                        """;

                        // when & then
                        mockMvc.perform(post("/api/admins")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestJson))
                                        .andDo(print())
                                        .andExpect(status().isBadRequest());
                }

                @Test
                @WithMockUser(roles = "ADMIN")
                @DisplayName("실패: admCode가 null인 경우")
                void createAdmin_NullAdmCode() throws Exception {
                        // given
                        String requestJson = """
                                        {
                                            "userId": %d,
                                            "admCode": null,
                                            "adminLevel": "CITY"
                                        }
                                        """.formatted(testUser.getId());

                        // when & then
                        mockMvc.perform(post("/api/admins")
                                        .contentType(MediaType.APPLICATION_JSON)
                                        .content(requestJson))
                                        .andDo(print())
                                        .andExpect(status().isBadRequest());
                }
        }
}