package com.aicc.silverlink.domain.chatbot.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * 챗봇 관련 설정
 */
@Configuration
public class ChatbotConfig {

    /**
     * Python 챗봇 서비스 호출용 RestTemplate
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
