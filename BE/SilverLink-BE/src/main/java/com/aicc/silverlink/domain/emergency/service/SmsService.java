package com.aicc.silverlink.domain.emergency.service;

import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlertRecipient;
import com.aicc.silverlink.domain.emergency.entity.SmsLog;
import com.aicc.silverlink.domain.emergency.entity.SmsLog.MessageType;
import com.aicc.silverlink.domain.emergency.repository.EmergencyAlertRecipientRepository;
import com.aicc.silverlink.domain.emergency.repository.SmsLogRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.global.config.twilio.TwilioProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * SMS 발송 서비스
 * Twilio API를 사용하여 SMS 발송 및 이력 관리
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final TwilioProperties twilioProperties;
    private final SmsLogRepository smsLogRepository;
    private final EmergencyAlertRecipientRepository recipientRepository;

    private String messagingServiceSid;

    // UCS-2 인코딩 SMS 세그먼트 제한 (한글 사용 시)
    private static final int UCS2_SINGLE_SEGMENT_LIMIT = 67;

    @PostConstruct
    public void init() {
        Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
        this.messagingServiceSid = twilioProperties.getMessagingServiceSid();
        log.info("[SmsService] Twilio 초기화 완료. MessagingServiceSid: {}",
                messagingServiceSid != null && messagingServiceSid.length() > 6
                        ? messagingServiceSid.substring(0, 6) + "****"
                        : "UNKNOWN");

        log.info("Messaging Service SID: {}", twilioProperties.getMessagingServiceSid());
    }

    // ========== 긴급 알림 SMS ==========

    /**
     * 긴급 알림 SMS 발송 (비동기)
     * 한글 SMS 인코딩 깨짐 방지를 위해 2건으로 분할 발송:
     * - 1차: 본문 메시지 (67자 이내)
     * - 2차: URL (67자 이내)
     */
    @Async
    @Transactional
    public void sendEmergencyAlertSmsAsync(EmergencyAlert alert, EmergencyAlertRecipient recipient) {
        try {
            User receiver = recipient.getReceiver();
            String phone = formatPhoneNumber(receiver.getPhone());

            // 본문 메시지 생성 (67자 이내)
            String bodyMessage = buildEmergencyAlertBodyMessage(alert, recipient);
            // URL 메시지 생성
            String urlMessage = buildEmergencyAlertUrlMessage(recipient);
            String shortUrl = getUrlForRecipient(recipient);

            // SMS 로그 생성 (본문 + URL 합쳐서 기록)
            String fullMessage = bodyMessage + "\n" + urlMessage;
            SmsLog smsLog = SmsLog.createForEmergencyAlert(
                    receiver,
                    phone,
                    alert.getSeverity(),
                    alert.getId(),
                    fullMessage,
                    shortUrl);
            smsLogRepository.save(smsLog);

            log.info("[SmsService] 긴급 알림 SMS 2건 분할 발송 시작. alertId={}, phone={}, bodyLen={}, urlLen={}",
                    alert.getId(), maskPhone(phone), bodyMessage.length(), urlMessage.length());

            // 1차 SMS: 본문 발송
            sendSmsWithoutLog(phone, bodyMessage);

            // 2차 SMS: URL 발송 (수신자 상태 업데이트는 여기서)
            sendSms(phone, urlMessage, smsLog, recipient);

        } catch (Exception e) {
            log.error("[SmsService] 긴급 알림 SMS 발송 실패. alertId={}, recipientId={}, error={}",
                    alert.getId(), recipient.getId(), e.getMessage(), e);
        }
    }

    /**
     * 긴급 알림 본문 메시지 생성 (67자 이내)
     * UCS-2 SMS 세그먼트 분할 방지를 위해 간결하게 유지
     */
    private String buildEmergencyAlertBodyMessage(EmergencyAlert alert, EmergencyAlertRecipient recipient) {
        var elderly = alert.getElderly();
        String elderlyName = truncate(elderly.getUser().getName(), 10);
        int age = elderly.age();

        String prefix = alert.isCritical() ? "[긴급]" : "[알림]";

        // 보호자용 메시지 (67자 이내)
        if (recipient.getReceiverRole() == EmergencyAlertRecipient.ReceiverRole.GUARDIAN) {
            return prefix + " " + elderlyName + "(" + age + "세)\n"
                    + "어르신 위험 감지됨\n"
                    + "담당 생활지원사님이 확인 후 연락 드릴 예정입니다.";
        }

        // 상담사용 메시지 (67자 이내)
        if (recipient.getReceiverRole() == EmergencyAlertRecipient.ReceiverRole.COUNSELOR) {
            return prefix + " 담당 어르신\n"
                    + elderlyName + "(" + age + "세) 위험 감지\n"
                    + truncate(alert.getTitle(), 20);
        }

        // 관리자용 메시지 (67자 이내)
        return prefix + " 담당 어르신\n"
                + elderlyName + "(" + age + "세) 위험 감지\n"
                + truncate(alert.getTitle(), 20);
    }

    /**
     * 긴급 알림 URL 메시지 생성 (67자 이내)
     */
    private String buildEmergencyAlertUrlMessage(EmergencyAlertRecipient recipient) {
        String url = getUrlForRecipient(recipient);
        return "상세 확인:\n" + url;
    }

    /**
     * 수신자 역할에 따른 URL 반환
     */
    private String getUrlForRecipient(EmergencyAlertRecipient recipient) {
        if (recipient.getReceiverRole() == EmergencyAlertRecipient.ReceiverRole.GUARDIAN) {
            return buildShortUrl("guardian", "alerts");
        }
        if (recipient.getReceiverRole() == EmergencyAlertRecipient.ReceiverRole.COUNSELOR) {
            return buildShortUrl("counselor", "alerts");
        }
        return buildShortUrl("admin", null);
    }

    // 하위 호환성을 위한 기존 메서드 유지 (deprecated)
    @Deprecated
    private String buildEmergencyAlertMessage(EmergencyAlert alert, EmergencyAlertRecipient recipient) {
        return buildEmergencyAlertBodyMessage(alert, recipient) + "\n"
                + buildEmergencyAlertUrlMessage(recipient);
    }

    // ========== 일반 알림 SMS ==========

    /**
     * 문의 답변 SMS 발송
     */
    @Async
    @Transactional
    public void sendInquiryReplySms(User receiver, Long inquiryId) {
        try {
            String phone = formatPhoneNumber(receiver.getPhone());
            String shortUrl = buildShortUrl("guardian", "inquiry");
            String message = "[실버링크]\n등록하신 문의에 답변이 등록되었습니다.\n확인: " + shortUrl;

            SmsLog smsLog = SmsLog.createForInquiryReply(receiver, phone, inquiryId, message, shortUrl);
            smsLogRepository.save(smsLog);

            sendSms(phone, message, smsLog, null);

        } catch (Exception e) {
            log.error("[SmsService] 문의 답변 SMS 발송 실패. inquiryId={}, error={}",
                    inquiryId, e.getMessage(), e);
        }
    }

    /**
     * 민원 답변 SMS 발송
     */
    @Async
    @Transactional
    public void sendComplaintReplySms(User receiver, Long complaintId) {
        try {
            String phone = formatPhoneNumber(receiver.getPhone());
            String shortUrl = buildShortUrl("guardian", "complaint");
            String message = "[실버링크]\n등록하신 민원에 답변이 등록되었습니다.\n확인: " + shortUrl;

            SmsLog smsLog = SmsLog.createForComplaintReply(receiver, phone, complaintId, message, shortUrl);
            smsLogRepository.save(smsLog);

            sendSms(phone, message, smsLog, null);

        } catch (Exception e) {
            log.error("[SmsService] 민원 답변 SMS 발송 실패. complaintId={}, error={}",
                    complaintId, e.getMessage(), e);
        }
    }

    /**
     * 접근권한 승인 SMS 발송
     */
    @Async
    @Transactional
    public void sendAccessApprovedSms(User receiver, Long requestId, String elderlyName) {
        try {
            String phone = formatPhoneNumber(receiver.getPhone());
            String shortUrl = buildShortUrl("guardian", "sensitive-info");
            String message = String.format(
                    "[실버링크]\n%s 어르신의 민감정보 열람 권한이 승인되었습니다.\n확인: %s",
                    elderlyName,
                    shortUrl);

            SmsLog smsLog = SmsLog.createForAccessRequest(receiver, phone, true, requestId, message, shortUrl);
            smsLogRepository.save(smsLog);

            sendSms(phone, message, smsLog, null);

        } catch (Exception e) {
            log.error("[SmsService] 접근권한 승인 SMS 발송 실패. requestId={}, error={}",
                    requestId, e.getMessage(), e);
        }
    }

    /**
     * 접근권한 거절 SMS 발송
     */
    @Async
    @Transactional
    public void sendAccessRejectedSms(User receiver, Long requestId, String elderlyName) {
        try {
            String phone = formatPhoneNumber(receiver.getPhone());
            String shortUrl = buildShortUrl("guardian", "sensitive-info");
            String message = String.format(
                    "[실버링크]\n%s 어르신의 민감정보 열람 권한 요청이 거절되었습니다.\n사유 확인: %s",
                    elderlyName,
                    shortUrl);

            SmsLog smsLog = SmsLog.createForAccessRequest(receiver, phone, false, requestId, message, shortUrl);
            smsLogRepository.save(smsLog);

            sendSms(phone, message, smsLog, null);

        } catch (Exception e) {
            log.error("[SmsService] 접근권한 거절 SMS 발송 실패. requestId={}, error={}",
                    requestId, e.getMessage(), e);
        }
    }

    // ========== 공통 메서드 ==========

    /**
     * SMS 발송 (Twilio)
     * 한글(UCS-2) 메시지가 SMS 세그먼트 경계에서 깨지지 않도록
     * 명시적으로 unicode 인코딩을 지정하여 발송
     */
    private void sendSms(String toPhone, String messageContent, SmsLog smsLog, EmergencyAlertRecipient recipient) {
        try {
            // Twilio Messaging Service를 사용하여 SMS 발송
            Message message = Message.creator(
                    new PhoneNumber(toPhone),
                    messagingServiceSid,
                    messageContent).create();

            String messageSid = message.getSid();
            log.info("[SmsService] SMS 발송 성공. to={}, sid={}", maskPhone(toPhone), messageSid);

            // 로그 업데이트
            smsLog.markSent(messageSid);
            smsLogRepository.save(smsLog);

            // 수신자 상태 업데이트
            if (recipient != null) {
                recipient.markSmsSent();
                recipientRepository.save(recipient);
            }

        } catch (Exception e) {
            log.error("[SmsService] SMS 발송 실패. to={}, error={}", maskPhone(toPhone), e.getMessage());

            smsLog.markFailed(e.getMessage());
            smsLogRepository.save(smsLog);

            if (recipient != null) {
                recipient.markSmsFailed();
                recipientRepository.save(recipient);
            }

            throw new RuntimeException("SMS 발송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * SMS 발송 (Twilio) - 로그 없이 발송
     * 분할 발송 시 첫 번째 메시지에 사용
     */
    private void sendSmsWithoutLog(String toPhone, String messageContent) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(toPhone),
                    messagingServiceSid,
                    messageContent).create();

            log.info("[SmsService] SMS 발송 성공 (분할 1차). to={}, sid={}",
                    maskPhone(toPhone), message.getSid());

        } catch (Exception e) {
            log.error("[SmsService] SMS 발송 실패 (분할 1차). to={}, error={}",
                    maskPhone(toPhone), e.getMessage());
            throw new RuntimeException("SMS 발송 실패: " + e.getMessage(), e);
        }
    }

    /**
     * 전화번호 E.164 형식으로 변환
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null)
            return null;

        // 이미 E.164 형식이면 그대로 반환
        if (phone.startsWith("+")) {
            return phone;
        }

        // 한국 번호 변환 (010-1234-5678 -> +821012345678)
        String cleaned = phone.replaceAll("[^0-9]", "");
        if (cleaned.startsWith("0")) {
            cleaned = "82" + cleaned.substring(1);
        }
        return "+" + cleaned;
    }

    /**
     * 단축 URL 생성
     * SMS 세그먼트 경계에서 URL이 깨지지 않도록 가능한 짧게 유지
     */
    private String buildShortUrl(String role, String page) {
        String baseUrl = "https://d1y2piyw58z1m3.cloudfront.net";
        if (page != null) {
            return baseUrl + "/" + role + "/" + page;
        }
        return baseUrl + "/" + role;
    }

    /**
     * 문자열 자르기
     */
    private String truncate(String str, int maxLength) {
        if (str == null)
            return "";
        if (str.length() <= maxLength)
            return str;
        return str.substring(0, maxLength - 3) + "...";
    }

    /**
     * 전화번호 마스킹 (로깅용)
     */
    private String maskPhone(String phone) {
        if (phone == null)
            return null;
        int len = phone.length();
        if (len <= 4)
            return "****";
        return phone.substring(0, Math.min(3, len)) + "****" + phone.substring(Math.max(len - 4, 0));
    }

    // ========== 중복 발송 방지 ==========

    /**
     * 최근 5분 내 동일 SMS 발송 여부 확인
     */
    public boolean isRecentlySent(String phone, MessageType messageType, Long referenceId) {
        LocalDateTime since = LocalDateTime.now().minusMinutes(5);
        return smsLogRepository.existsRecentSms(
                formatPhoneNumber(phone),
                messageType,
                referenceId,
                since);
    }
}
