package com.aicc.silverlink.infra.external.welfare;

import com.aicc.silverlink.domain.welfare.dto.WelfareApiDto;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

@Component
public class WelfareApiClient {

    private final WebClient webClient;

    public WelfareApiClient(WebClient webClient) {
        this.webClient = webClient;
    }

    public WelfareApiDto.CentralResponse getCentralList(String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(WelfareApiDto.CentralResponse.class)
                .block();
    }

    public WelfareApiDto.DetailItem getCentralDetail(String uri) {
        return webClient.get()
                .uri(uri)
                .retrieve()
                .bodyToMono(WelfareApiDto.DetailItem.class)
                .block();
    }
}