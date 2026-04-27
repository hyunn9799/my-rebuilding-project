package com.aicc.silverlink.global.config.twilio;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Twilio 설정 프로퍼티
 *
 * application.yml 예시:
 * twilio:
 * account-sid: ACxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * auth-token: xxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * verify-service-sid: VAxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * messaging-service-sid: MGxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxxx
 * sms-enabled: true
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "twilio")
public class TwilioProperties {

    /**
     * Twilio Account SID
     */
    private String accountSid;

    /**
     * Twilio Auth Token
     */
    private String authToken;

    /**
     * Twilio Verify Service SID (인증번호 발송용)
     */
    private String verifyServiceSid;

    /**
     * Twilio Messaging Service SID (일반 SMS 발송용)
     */
    private String messagingServiceSid;

    /**
     * SMS 발송 활성화 여부
     * false로 설정 시 실제 SMS 발송 없이 로그만 기록
     */
    private boolean smsEnabled = true;
}
