package com.aicc.silverlink.infrastructure.callbot;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * CallBot 설정
 */
@Component
@ConfigurationProperties(prefix = "callbot.api")
@Getter
@Setter
public class CallBotProperties {

    /**
     * Python CallBot API URL
     * 예: http://localhost:5000
     */
    private String url;

    /**
     * 연결 타임아웃 (ms)
     */
    private int connectTimeout = 5000;

    /**
     * 읽기 타임아웃 (ms)
     */
    private int readTimeout = 10000;
}
