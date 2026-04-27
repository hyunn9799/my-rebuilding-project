package com.aicc.silverlink.global.config.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Getter
@Setter
@Configuration
@ConfigurationProperties(prefix = "security.auth")
public class AuthPolicyProperties {

    private Long accessTtlSeconds;
    private Long refreshTtlSeconds;
    private Long idleTtlSeconds;
    private String concurrentPolicy;

    private String refreshCookieName;
    private String refreshCookiePath;
    private String refreshCookieSameSite;
    private Boolean refreshCookieSecure;

}
