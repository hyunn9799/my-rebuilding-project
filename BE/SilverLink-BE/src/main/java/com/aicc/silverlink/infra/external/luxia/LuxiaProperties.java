package com.aicc.silverlink.infra.external.luxia;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "luxia")
public record LuxiaProperties(
        String baseUrl,
        String apiKey,
        DocumentAi documentAi,
        Timeout timeout
) {
    public record DocumentAi(String path, String model) {}
    public record Timeout(int connectMs, int responseMs) {}
}
