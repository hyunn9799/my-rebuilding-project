package com.aicc.silverlink.domain.notification.service;

import com.aicc.silverlink.domain.emergency.entity.SmsLog;
import com.aicc.silverlink.domain.emergency.entity.SmsLog.MessageType;
import com.aicc.silverlink.domain.emergency.repository.SmsLogRepository;
import com.aicc.silverlink.global.config.twilio.TwilioProperties;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import com.aicc.silverlink.domain.user.entity.User;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 일반 알림 SMS 서비스
 *
 * 문의 답변, 민원 답변, 접근권한 승인/거절 등의 SMS 발송
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationSmsService {

    private final SmsLogRepository smsLogRepository;
    private final TwilioProperties twilioProperties;

    @PostConstruct
    public void init() {
        if (twilioProperties.getAccountSid() != null && twilioProperties.getAuthToken() != null) {
            Twilio.init(twilioProperties.getAccountSid(), twilioProperties.getAuthToken());
            log.info("[NotificationSmsService] Twilio 초기화 완료");
        }
    }

    // ========== 문의 답변 SMS ==========

    @Async
    @Transactional
    public void sendInquiryReplySmsAsync(User receiver, Long inquiryId) {
        String phone = receiver.getPhone();
        if (phone == null || phone.isBlank()) {
            log.warn("[SMS] 수신자 전화번호 없음. userId={}", receiver.getId());
            return;
        }

        // 중복 발송 방지
        if (smsLogRepository.existsRecentSms(phone, MessageType.INQUIRY_REPLY, inquiryId,
                java.time.LocalDateTime.now().minusMinutes(5))) {
            log.info("[SMS] 최근 동일 SMS 발송 이력 있음. 스킵. inquiryId={}", inquiryId);
            return;
        }

        String shortUrl = buildShortUrl("guardian", "inquiry");
        String message = String.format(
                "[실버링크]\n등록하신 문의에 답변이 등록되었습니다.\n확인: %s",
                shortUrl);

        SmsLog smsLog = SmsLog.createForInquiryReply(receiver, phone, inquiryId, message, shortUrl);
        smsLogRepository.save(smsLog);

        sendSms(smsLog, phone, message);
    }

    // ========== 민원 답변 SMS ==========

    @Async
    @Transactional
    public void sendComplaintReplySmsAsync(User receiver, Long complaintId) {
        String phone = receiver.getPhone();
        if (phone == null || phone.isBlank()) {
            log.warn("[SMS] 수신자 전화번호 없음. userId={}", receiver.getId());
            return;
        }

        if (smsLogRepository.existsRecentSms(phone, MessageType.COMPLAINT_REPLY, complaintId,
                java.time.LocalDateTime.now().minusMinutes(5))) {
            log.info("[SMS] 최근 동일 SMS 발송 이력 있음. 스킵. complaintId={}", complaintId);
            return;
        }

        String shortUrl = buildShortUrl("guardian", "complaint");
        String message = String.format(
                "[실버링크]\n등록하신 민원에 답변이 등록되었습니다.\n확인: %s",
                shortUrl);

        SmsLog smsLog = SmsLog.createForComplaintReply(receiver, phone, complaintId, message, shortUrl);
        smsLogRepository.save(smsLog);

        sendSms(smsLog, phone, message);
    }

    // ========== 접근권한 승인 SMS ==========

    @Async
    @Transactional
    public void sendAccessApprovedSmsAsync(User receiver, Long requestId, String elderlyName) {
        String phone = receiver.getPhone();
        if (phone == null || phone.isBlank()) {
            log.warn("[SMS] 수신자 전화번호 없음. userId={}", receiver.getId());
            return;
        }

        if (smsLogRepository.existsRecentSms(phone, MessageType.ACCESS_APPROVED, requestId,
                java.time.LocalDateTime.now().minusMinutes(5))) {
            log.info("[SMS] 최근 동일 SMS 발송 이력 있음. 스킵. requestId={}", requestId);
            return;
        }

        String shortUrl = buildShortUrl("guardian", "sensitive-info");
        String message = String.format(
                "[실버링크]\n%s 어르신의 민감정보 열람 권한이 승인되었습니다.\n확인: %s",
                elderlyName, shortUrl);

        SmsLog smsLog = SmsLog.createForAccessRequest(
                receiver, phone, true, requestId, message, shortUrl);
        smsLogRepository.save(smsLog);

        sendSms(smsLog, phone, message);
    }

    // ========== 접근권한 거절 SMS ==========

    @Async
    @Transactional
    public void sendAccessRejectedSmsAsync(User receiver, Long requestId, String elderlyName, String reason) {
        String phone = receiver.getPhone();
        if (phone == null || phone.isBlank()) {
            log.warn("[SMS] 수신자 전화번호 없음. userId={}", receiver.getId());
            return;
        }

        if (smsLogRepository.existsRecentSms(phone, MessageType.ACCESS_REJECTED, requestId,
                java.time.LocalDateTime.now().minusMinutes(5))) {
            log.info("[SMS] 최근 동일 SMS 발송 이력 있음. 스킵. requestId={}", requestId);
            return;
        }

        String shortUrl = buildShortUrl("guardian", "sensitive-info");
        String reasonSummary = reason != null && reason.length() > 20
                ? reason.substring(0, 20) + "..."
                : reason;

        String message = String.format(
                "[실버링크]\n%s 어르신의 민감정보 열람 권한 요청이 거절되었습니다.\n사유: %s\n확인: %s",
                elderlyName, reasonSummary != null ? reasonSummary : "사유 미기재", shortUrl);

        SmsLog smsLog = SmsLog.createForAccessRequest(
                receiver, phone, false, requestId, message, shortUrl);
        smsLogRepository.save(smsLog);

        sendSms(smsLog, phone, message);
    }

    // ========== 공통 메서드 ==========

    /**
     * SMS 발송 (Twilio)
     */
    private void sendSms(SmsLog smsLog, String phone, String messageContent) {
        // SMS 비활성화 상태면 로그만 기록
        if (!isSmsEnabled()) {
            log.info("[SMS] SMS 비활성화 상태. 로그만 기록. phone={}", maskPhone(phone));
            smsLog.markSent("DISABLED_MODE");
            smsLogRepository.save(smsLog);
            return;
        }

        try {
            String formattedPhone = formatPhoneNumber(phone);

            // Twilio Messaging Service를 사용하여 SMS 발송
            Message message = Message.creator(
                    new PhoneNumber(formattedPhone),
                    getMessagingServiceSid(),
                    messageContent).create();

            smsLog.markSent(message.getSid());
            smsLogRepository.save(smsLog);

            log.info("[SMS] 발송 성공. sid={}, phone={}", message.getSid(), maskPhone(phone));

        } catch (Exception e) {
            log.error("[SMS] 발송 실패. phone={}, error={}", maskPhone(phone), e.getMessage(), e);
            smsLog.markFailed(e.getMessage());
            smsLogRepository.save(smsLog);
        }
    }

    /**
     * 전화번호 E.164 형식 변환
     */
    private String formatPhoneNumber(String phone) {
        if (phone == null)
            return null;

        String cleaned = phone.replaceAll("[^0-9]", "");

        if (cleaned.startsWith("010")) {
            return "+82" + cleaned.substring(1);
        } else if (cleaned.startsWith("82")) {
            return "+" + cleaned;
        } else if (cleaned.startsWith("+82")) {
            return cleaned;
        }

        return "+82" + cleaned;
    }

    /**
     * 단축 URL 생성
     */
    private String buildShortUrl(String role, String page) {
        String baseUrl = "https://d1y2piyw58z1m3.cloudfront.net";
        if (page != null) {
            return String.format("%s/%s/%s", baseUrl, role, page);
        }
        return String.format("%s/%s", baseUrl, role);
    }

    /**
     * 전화번호 마스킹 (로깅용)
     */
    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4)
            return "****";
        return phone.substring(0, phone.length() - 4) + "****";
    }

    /**
     * SMS 활성화 여부
     */
    private boolean isSmsEnabled() {
        // TwilioProperties에 smsEnabled 필드 추가 필요
        // 기본값은 false (개발 환경에서는 실제 발송하지 않음)
        return twilioProperties.getAccountSid() != null
                && !twilioProperties.getAccountSid().isBlank();
    }

    /**
     * Messaging Service SID
     */
    private String getMessagingServiceSid() {
        return twilioProperties.getMessagingServiceSid();
    }
}
