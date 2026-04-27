package com.aicc.silverlink.infrastructure.callbot;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.StartCallRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

/**
 * Python CallBot API 클라이언트
 */
@Component
@Slf4j
public class CallBotClient {

    private final RestTemplate restTemplate;
    private final CallBotProperties callBotProperties;

    public CallBotClient(@Qualifier("callBotRestTemplate") RestTemplate restTemplate,
                         CallBotProperties callBotProperties) {
        this.restTemplate = restTemplate;
        this.callBotProperties = callBotProperties;
    }

    /**
     * Python CallBot에 전화 발신 요청
     *
     * @param request 전화 발신 요청 정보 (elderlyId, name, phone, chronicDiseases)
     * @return 요청 성공 여부
     */
    public boolean startCall(StartCallRequest request) {
        String url = callBotProperties.getUrl() + "/api/callbot/call";

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<StartCallRequest> entity = new HttpEntity<>(request, headers);

            restTemplate.postForEntity(url, entity, Void.class);

            log.info("[CallBot] 전화 발신 요청 성공 (POST): elderlyId={}, name={}, phone={}",
                    request.getElderlyId(), request.getElderlyName(), maskPhone(request.getPhone()));

            return true;

        } catch (RestClientException e) {
            log.error("[CallBot] 전화 발신 요청 실패: elderlyId={}, error={}",
                    request.getElderlyId(), e.getMessage());
            return false;
        }
    }

    /**
     * CallBot 서버 상태 확인
     */
    public boolean isHealthy() {
        String url = callBotProperties.getUrl() + "/health";

        try {
            restTemplate.getForEntity(url, String.class);
            return true;
        } catch (RestClientException e) {
            log.warn("[CallBot] 서버 상태 확인 실패: {}", e.getMessage());
            return false;
        }
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return phone.substring(0, phone.length() - 4) + "****";
    }
}
