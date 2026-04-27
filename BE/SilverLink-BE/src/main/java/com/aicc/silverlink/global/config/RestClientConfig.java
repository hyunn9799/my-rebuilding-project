package com.aicc.silverlink.global.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestClient;

@Configuration
public class RestClientConfig {

    @Bean
    public RestClient restClient() {
        // [수정] 빌더를 주입받지 않고, 직접 생성해서 build() 합니다.
        return RestClient.builder()
                .build();
    }
}