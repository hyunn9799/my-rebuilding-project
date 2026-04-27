package com.aicc.silverlink.global.config.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.auth.phone")
public class AuthPhoneProperties {

    private int codeLength = 6;
    private int ttlSeconds = 300;
    private int maxAttemps = 5;
    private int cooldownSeconds = 60;
    private int dailyLimit = 10;
    private boolean debugReturnCode = false;
}
