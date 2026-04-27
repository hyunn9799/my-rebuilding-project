package com.aicc.silverlink.infrastructure.callbot;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * CallBot 관련 Bean 설정
 */
@Configuration
public class CallBotConfig {

    /**
     * CallBot 전용 RestTemplate Bean
     */
    @Bean(name = "callBotRestTemplate")
    public RestTemplate callBotRestTemplate(CallBotProperties properties) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.getConnectTimeout());
        factory.setReadTimeout(properties.getReadTimeout());
        return new RestTemplate(factory);
    }
}
