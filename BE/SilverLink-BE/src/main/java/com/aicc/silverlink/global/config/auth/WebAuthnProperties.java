package com.aicc.silverlink.global.config.auth;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.HashSet;
import java.util.Set;

@Getter
@Setter
@ConfigurationProperties(prefix = "security.auth.webauthn")
public class WebAuthnProperties {

    private Rp rp = new Rp();
    private Set<String> origins = new HashSet<>();

    @Getter
    @Setter
    public static class Rp {
        private String id;
        private String name = "SilverLink";
        private String origin;
    }

    // Convenience methods for backward compatibility
    public String getRpId() {
        return rp.getId();
    }

    public String getRpName() {
        return rp.getName();
    }
}
