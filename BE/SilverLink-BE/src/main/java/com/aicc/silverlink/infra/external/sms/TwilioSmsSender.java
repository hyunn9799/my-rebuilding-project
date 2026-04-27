package com.aicc.silverlink.infra.external.sms;

import com.aicc.silverlink.global.config.twilio.TwilioProperties;
import com.twilio.Twilio;
import com.twilio.exception.ApiException;
import com.twilio.rest.verify.v2.service.Verification;
import com.twilio.rest.verify.v2.service.VerificationCheck;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * Twilio Verify API를 사용한 SMS 인증 발송/검증 구현체
 */
@Slf4j
@Component
public class TwilioSmsSender {

    private final TwilioProperties props;
    private String verifyServiceSid;

    public TwilioSmsSender(TwilioProperties props) {
        this.props = props;
    }

    @PostConstruct
    public void init() {
        Twilio.init(props.getAccountSid(), props.getAuthToken());
        this.verifyServiceSid = props.getVerifyServiceSid();
        log.info("[Twilio] Initialized with Verify Service SID: {}****",
                verifyServiceSid != null && verifyServiceSid.length() > 6
                        ? verifyServiceSid.substring(0, 6)
                        : "UNKNOWN");
    }

    /**
     * 인증번호 발송 (Twilio Verify가 자동 생성)
     * 
     * @param toNumber 수신번호 (E.164 형식: +821012345678)
     * @return 발송 성공 여부
     */
    public boolean sendVerificationCode(String toNumber) {
        try {
            Verification verification = Verification.creator(
                    verifyServiceSid,
                    toNumber,
                    "sms").create();

            log.info("[Twilio] 인증번호 발송 완료. to={}, status={}",
                    maskPhone(toNumber), verification.getStatus());
            return true;

        } catch (ApiException e) {
            log.error("[Twilio] 인증번호 발송 실패 (API 오류). to={}, code={}, msg={}",
                    maskPhone(toNumber), e.getCode(), e.getMessage());
            throw new IllegalStateException("SMS 발송 실패: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("[Twilio] 인증번호 발송 실패. to={}, msg={}",
                    maskPhone(toNumber), e.getMessage(), e);
            throw new IllegalStateException("SMS 발송 중 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 인증번호 확인
     * 
     * @param toNumber 수신번호 (E.164 형식: +821012345678)
     * @param code     사용자가 입력한 인증번호
     * @return 검증 결과
     */
    public VerificationResult checkVerificationCode(String toNumber, String code) {
        try {
            VerificationCheck verificationCheck = VerificationCheck.creator(verifyServiceSid)
                    .setCode(code)
                    .setTo(toNumber)
                    .create();

            String status = verificationCheck.getStatus();

            if ("approved".equals(status)) {
                log.info("[Twilio] 인증 성공. to={}", maskPhone(toNumber));
                return VerificationResult.SUCCESS;
            } else {
                // status가 'pending'인 경우 = 코드 불일치
                log.warn("[Twilio] 인증 실패 (코드 불일치). to={}, status={}",
                        maskPhone(toNumber), status);
                return VerificationResult.CODE_MISMATCH;
            }

        } catch (ApiException e) {
            // 20404: 인증 세션 만료 또는 존재하지 않음
            if (e.getCode() == 20404) {
                log.warn("[Twilio] 인증 실패 (만료/없음). to={}", maskPhone(toNumber));
                return VerificationResult.EXPIRED;
            }
            log.error("[Twilio] 인증 확인 실패 (API 오류). to={}, code={}, msg={}",
                    maskPhone(toNumber), e.getCode(), e.getMessage());
            throw new IllegalStateException("인증 확인 중 오류: " + e.getMessage(), e);

        } catch (Exception e) {
            log.error("[Twilio] 인증 확인 실패. to={}, msg={}",
                    maskPhone(toNumber), e.getMessage(), e);
            throw new IllegalStateException("인증 확인 중 시스템 오류: " + e.getMessage(), e);
        }
    }

    /**
     * 인증 결과 Enum
     */
    public enum VerificationResult {
        SUCCESS, // 인증 성공
        CODE_MISMATCH, // 코드 불일치
        EXPIRED // 만료 또는 존재하지 않음
    }

    /**
     * 로그에서 전화번호 마스킹
     */
    private String maskPhone(String phone) {
        if (phone == null)
            return null;
        int len = phone.length();
        if (len <= 4)
            return "****";
        return phone.substring(0, Math.min(3, len)) + "****" + phone.substring(Math.max(len - 4, 0));
    }
}
