package com.aicc.silverlink.domain.admin.service;

import com.aicc.silverlink.domain.admin.dto.AdminMemberDtos;
import com.aicc.silverlink.domain.admin.entity.OfflineRegistrationLog;
import com.aicc.silverlink.domain.admin.repository.OfflineRegistrationLogRepository;
import com.aicc.silverlink.domain.auth.entity.PhoneVerification;
import com.aicc.silverlink.domain.auth.repository.PhoneVerificationRepository;
import com.aicc.silverlink.domain.consent.entity.ConsentHistory;
import com.aicc.silverlink.domain.consent.repository.ConsentHistoryRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.util.SecurityUtils;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.LocalDate;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OfflineRegistrationServiceTest {

    @InjectMocks
    private OfflineRegistrationService service;

    @Mock
    private UserRepository userRepository;
    @Mock
    private ElderlyRepository elderlyRepository;
    @Mock
    private GuardianRepository guardianRepository;
    @Mock
    private GuardianElderlyRepository guardianElderlyRepository;
    @Mock
    private AdministrativeDivisionRepository divRepository;
    @Mock
    private OfflineRegistrationLogRepository logRepository;
    @Mock
    private ConsentHistoryRepository consentRepository;
    @Mock
    private PhoneVerificationRepository phoneVerificationRepository;
    @Mock
    private PasswordEncoder passwordEncoder;

    @Test
    @DisplayName("어르신 등록 실패 - 중복된 ID")
    void registerElderly_fail_duplicateId() {
        // given
        AdminMemberDtos.RegisterElderlyRequest req = createElderlyRequest();

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(1L);
            given(userRepository.findByLoginId(req.loginId())).willReturn(Optional.of(mock(User.class)));

            // when & then
            assertThatThrownBy(() -> service.registerElderly(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("LOGIN_ID_DUPLICATE");
        }
    }

    @Test
    @DisplayName("어르신 등록 실패 - 중복된 전화번호")
    void registerElderly_fail_duplicatePhone() {
        // given
        AdminMemberDtos.RegisterElderlyRequest req = createElderlyRequest();

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(1L);
            given(userRepository.findByLoginId(req.loginId())).willReturn(Optional.empty());
            given(userRepository.existsByPhone(req.phone())).willReturn(true);

            // when & then
            assertThatThrownBy(() -> service.registerElderly(req))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessage("PHONE_DUPLICATE");
        }
    }

    @Test
    @DisplayName("어르신 등록 성공")
    void registerElderly_success() {
        // given
        AdminMemberDtos.RegisterElderlyRequest req = createElderlyRequest();
        AdministrativeDivision div = mock(AdministrativeDivision.class);

        try (MockedStatic<SecurityUtils> securityUtils = mockStatic(SecurityUtils.class)) {
            securityUtils.when(SecurityUtils::currentUserId).thenReturn(1L);

            given(userRepository.findByLoginId(req.loginId())).willReturn(Optional.empty());
            given(userRepository.existsByPhone(req.phone())).willReturn(false);
            given(passwordEncoder.encode(req.password())).willReturn("encodedPass");
            given(divRepository.findById(req.admCode())).willReturn(Optional.of(div));

            // when
            service.registerElderly(req);

            // then
            verify(userRepository).save(any(User.class));
            verify(elderlyRepository).save(any(Elderly.class));
            verify(logRepository).save(any(OfflineRegistrationLog.class));
            verify(phoneVerificationRepository).save(any(PhoneVerification.class));
            verify(consentRepository, times(3)).save(any(ConsentHistory.class));
        }
    }

    // Helper
    private AdminMemberDtos.RegisterElderlyRequest createElderlyRequest() {
        return new AdminMemberDtos.RegisterElderlyRequest(
                "elder1", "pass", "Kim", "010-1111-2222", null,
                1L, LocalDate.of(1950, 1, 1), Elderly.Gender.M,
                "Addr1", "Addr2", "12345", "Memo",
                "09:00", java.util.List.of("MON", "WED", "FRI"), true);
    }
}
