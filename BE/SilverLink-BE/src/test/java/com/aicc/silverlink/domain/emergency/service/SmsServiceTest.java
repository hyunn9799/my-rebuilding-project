package com.aicc.silverlink.domain.emergency.service;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlertRecipient;
import com.aicc.silverlink.domain.emergency.entity.SmsLog;
import com.aicc.silverlink.domain.emergency.entity.SmsLog.MessageType;
import com.aicc.silverlink.domain.emergency.repository.EmergencyAlertRecipientRepository;
import com.aicc.silverlink.domain.emergency.repository.SmsLogRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.global.config.twilio.TwilioProperties;
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
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * SmsService 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class SmsServiceTest {

    @InjectMocks
    private SmsService smsService;

    @Mock
    private TwilioProperties twilioProperties;

    @Mock
    private SmsLogRepository smsLogRepository;

    @Mock
    private EmergencyAlertRecipientRepository recipientRepository;

    private User elderlyUser;
    private User guardianUser;
    private User counselorUser;
    private Elderly elderly;

    @BeforeEach
    void setUp() {
        // 어르신 유저
        elderlyUser = User.createLocal("elderly01", "pw", "김순자", "01011112222", null, Role.ELDERLY, null);
        ReflectionTestUtils.setField(elderlyUser, "id", 1L);

        // 보호자 유저
        guardianUser = User.createLocal("guardian01", "pw", "김보호", "01033334444", null, Role.GUARDIAN, null);
        ReflectionTestUtils.setField(guardianUser, "id", 2L);

        // 상담사 유저
        counselorUser = User.createLocal("counselor01", "pw", "박상담", "01055556666", null, Role.COUNSELOR, null);
        ReflectionTestUtils.setField(counselorUser, "id", 3L);

        // Messaging Service SID 설정
        ReflectionTestUtils.setField(smsService, "messagingServiceSid", "MG1234567890abcdef");
    }

    // ========== 전화번호 형식 변환 테스트 ==========

    @Nested
    @DisplayName("전화번호 E.164 형식 변환 테스트")
    class FormatPhoneNumberTest {

        @Test
        @DisplayName("010-1234-5678 형식 → +821012345678")
        void formatWithDashes() {
            // given
            String phone = "010-1234-5678";

            // when
            String result = invokeFormatPhoneNumber(phone);

            // then
            assertThat(result).isEqualTo("+821012345678");
        }

        @Test
        @DisplayName("01012345678 형식 → +821012345678")
        void formatWithoutDashes() {
            // given
            String phone = "01012345678";

            // when
            String result = invokeFormatPhoneNumber(phone);

            // then
            assertThat(result).isEqualTo("+821012345678");
        }

        @Test
        @DisplayName("+821012345678 형식 → 그대로 유지")
        void formatAlreadyE164() {
            // given
            String phone = "+821012345678";

            // when
            String result = invokeFormatPhoneNumber(phone);

            // then
            assertThat(result).isEqualTo("+821012345678");
        }

        @Test
        @DisplayName("null 입력 → null 반환")
        void formatNull() {
            // when
            String result = invokeFormatPhoneNumber(null);

            // then
            assertThat(result).isNull();
        }

        private String invokeFormatPhoneNumber(String phone) {
            return ReflectionTestUtils.invokeMethod(smsService, "formatPhoneNumber", phone);
        }
    }

    // ========== 전화번호 마스킹 테스트 ==========

    @Nested
    @DisplayName("전화번호 마스킹 테스트")
    class MaskPhoneTest {

        @Test
        @DisplayName("+821012345678 → +8210****5678")
        void maskE164Phone() {
            // given
            String phone = "+821012345678";

            // when
            String result = invokeMaskPhone(phone);

            // then
            assertThat(result).contains("****");
            assertThat(result).doesNotContain("1234");
        }

        @Test
        @DisplayName("null 입력 → null 반환")
        void maskNull() {
            // when
            String result = invokeMaskPhone(null);

            // then
            assertThat(result).isNull();
        }

        private String invokeMaskPhone(String phone) {
            return ReflectionTestUtils.invokeMethod(smsService, "maskPhone", phone);
        }
    }

    // ========== 문자열 자르기 테스트 ==========

    @Nested
    @DisplayName("문자열 자르기 테스트")
    class TruncateTest {

        @Test
        @DisplayName("긴 문자열 → 지정 길이로 자르고 ... 추가")
        void truncateLongString() {
            // given
            String longStr = "이것은 매우 긴 문자열입니다. 테스트를 위해 작성되었습니다.";

            // when
            String result = invokeTruncate(longStr, 10);

            // then
            assertThat(result).hasSize(10); // maxLength가 10이나까 10글자
            assertThat(result).endsWith("...");
        }

        @Test
        @DisplayName("짧은 문자열 → 그대로 유지")
        void truncateShortString() {
            // given
            String shortStr = "짧은 문자열";

            // when
            String result = invokeTruncate(shortStr, 100);

            // then
            assertThat(result).isEqualTo(shortStr);
        }

        @Test
        @DisplayName("null 입력 → 빈 문자열 반환")
        void truncateNull() {
            // when
            String result = invokeTruncate(null, 10);

            // then
            assertThat(result).isEmpty();
        }

        private String invokeTruncate(String str, int maxLength) {
            return ReflectionTestUtils.invokeMethod(smsService, "truncate", str, maxLength);
        }
    }

    // ========== 단축 URL 생성 테스트 ==========

    @Nested
    @DisplayName("단축 URL 생성 테스트")
    class BuildShortUrlTest {

        @Test
        @DisplayName("상담사 알림 URL 생성")
        void buildCounselorAlertUrl() {
            // when
            String result = invokeBuildShortUrl("counselor", "alerts");

            // then
            assertThat(result).isEqualTo("https://d1y2piyw58z1m3.cloudfront.net/counselor/alerts");
        }

        @Test
        @DisplayName("보호자 URL 생성 (page 없음)")
        void buildGuardianUrl() {
            // when
            String result = invokeBuildShortUrl("guardian", null);

            // then
            assertThat(result).isEqualTo("https://d1y2piyw58z1m3.cloudfront.net/guardian");
        }

        @Test
        @DisplayName("보호자 문의 URL 생성")
        void buildGuardianInquiryUrl() {
            // when
            String result = invokeBuildShortUrl("guardian", "inquiry");

            // then
            assertThat(result).isEqualTo("https://d1y2piyw58z1m3.cloudfront.net/guardian/inquiry");
        }

        private String invokeBuildShortUrl(String role, String page) {
            return ReflectionTestUtils.invokeMethod(smsService, "buildShortUrl", role, page);
        }
    }

    // ========== 중복 발송 방지 테스트 ==========

    @Nested
    @DisplayName("중복 발송 방지 테스트")
    class RecentlySentTest {

        @Test
        @DisplayName("최근 5분 내 동일 SMS 발송 여부 확인 - 있음")
        void isRecentlySent_True() {
            // given
            String phone = "+821012345678";
            MessageType type = MessageType.EMERGENCY_CRITICAL;
            Long referenceId = 1L;

            given(smsLogRepository.existsRecentSms(eq(phone), eq(type), eq(referenceId), any(LocalDateTime.class)))
                    .willReturn(true);

            // when
            boolean result = smsService.isRecentlySent(phone, type, referenceId);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("최근 5분 내 동일 SMS 발송 여부 확인 - 없음")
        void isRecentlySent_False() {
            // given
            String phone = "+821012345678";
            MessageType type = MessageType.INQUIRY_REPLY;
            Long referenceId = 2L;

            given(smsLogRepository.existsRecentSms(eq(phone), eq(type), eq(referenceId), any(LocalDateTime.class)))
                    .willReturn(false);

            // when
            boolean result = smsService.isRecentlySent(phone, type, referenceId);

            // then
            assertThat(result).isFalse();
        }
    }

    // ========== 긴급 알림 메시지 생성 테스트 ==========

    @Nested
    @DisplayName("긴급 알림 메시지 생성 테스트")
    class BuildEmergencyAlertMessageTest {

        @Test
        @DisplayName("보호자용 메시지 생성")
        void buildMessageForGuardian() {
            // given
            EmergencyAlert alert = createMockAlert(EmergencyAlert.Severity.CRITICAL);
            EmergencyAlertRecipient recipient = createMockRecipient(guardianUser,
                    EmergencyAlertRecipient.ReceiverRole.GUARDIAN);

            // when
            String result = invokeBuildEmergencyAlertMessage(alert, recipient);

            // then
            assertThat(result).contains("[긴급]");
            assertThat(result).contains("김순자");
            assertThat(result).contains("위험 감지");
        }

        @Test
        @DisplayName("상담사용 메시지 생성 - WARNING 수준")
        void buildMessageForCounselor_Warning() {
            // given
            EmergencyAlert alert = createMockAlert(EmergencyAlert.Severity.WARNING);
            EmergencyAlertRecipient recipient = createMockRecipient(counselorUser,
                    EmergencyAlertRecipient.ReceiverRole.COUNSELOR);

            // when
            String result = invokeBuildEmergencyAlertMessage(alert, recipient);

            // then
            assertThat(result).contains("[알림]");
            assertThat(result).contains("담당 어르신");
            assertThat(result).contains("위험 감지");
        }

        private EmergencyAlert createMockAlert(EmergencyAlert.Severity severity) {
            EmergencyAlert alert = mock(EmergencyAlert.class);
            Elderly mockElderly = mock(Elderly.class);

            given(alert.getId()).willReturn(1L);
            given(alert.getSeverity()).willReturn(severity);
            given(alert.getAlertType()).willReturn(EmergencyAlert.AlertType.HEALTH);
            given(alert.getTitle()).willReturn("건강 위험 감지");
            given(alert.isCritical()).willReturn(severity == EmergencyAlert.Severity.CRITICAL);
            given(alert.getElderly()).willReturn(mockElderly);
            given(mockElderly.getUser()).willReturn(elderlyUser);
            given(mockElderly.age()).willReturn(78);

            return alert;
        }

        private EmergencyAlertRecipient createMockRecipient(User receiver, EmergencyAlertRecipient.ReceiverRole role) {
            EmergencyAlertRecipient recipient = mock(EmergencyAlertRecipient.class);
            given(recipient.getReceiver()).willReturn(receiver);
            given(recipient.getReceiverRole()).willReturn(role);
            return recipient;
        }

        private String invokeBuildEmergencyAlertMessage(EmergencyAlert alert, EmergencyAlertRecipient recipient) {
            return ReflectionTestUtils.invokeMethod(smsService, "buildEmergencyAlertMessage", alert, recipient);
        }
    }
}