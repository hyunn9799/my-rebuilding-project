package com.aicc.silverlink.infra.external.welfare;

import com.aicc.silverlink.infra.external.welfare.dto.MyXmlDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WelfareClient {

    private final WebClient webClient;

    public WelfareClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public MyXmlDto callWelfareApi(String url) {
        return webClient.get()
                .uri(url)
                .retrieve()
                .bodyToMono(MyXmlDto.class)
                .block(); // 필요하면 reactive 유지
    }
}