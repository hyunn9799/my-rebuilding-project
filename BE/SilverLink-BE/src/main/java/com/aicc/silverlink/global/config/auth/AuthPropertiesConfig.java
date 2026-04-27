package com.aicc.silverlink.global.config.auth;

import com.aicc.silverlink.global.config.twilio.TwilioProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ AuthPhoneProperties.class, WebAuthnProperties.class, TwilioProperties.class })
public class AuthPropertiesConfig {

}
