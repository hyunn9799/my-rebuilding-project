package com.aicc.silverlink.infrastructure.callbot;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.StartCallRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class CallBotClientTest {

    @Mock
    private RestTemplate restTemplate;

    private CallBotProperties callBotProperties;

    private CallBotClient callBotClient;

    @BeforeEach
    void setUp() {
        callBotProperties = new CallBotProperties();
        String pythonUrl = "http://localhost:5000";
        callBotProperties.setUrl(pythonUrl);
        
        callBotClient = new CallBotClient(restTemplate, callBotProperties);
    }

    @Test
    @DisplayName("startCall - POST 요청으로 JSON 바디가 올바르게 전달되어야 한다")
    @SuppressWarnings("unchecked")
    void startCall_shouldSendPostRequestWithJsonBody() {
        // given
        StartCallRequest request = StartCallRequest.builder()
                .elderlyId(3L)
                .elderlyName("HongGilDong")
                .phone("+811053915653")
                .build();

        String expectedUrl = "http://localhost:5000/api/callbot/call";

        // when
        callBotClient.startCall(request);

        // then
        ArgumentCaptor<HttpEntity<StartCallRequest>> entityCaptor = ArgumentCaptor.forClass(HttpEntity.class);
        verify(restTemplate).postForEntity(eq(expectedUrl), entityCaptor.capture(), eq(Void.class));

        HttpEntity<StartCallRequest> capturedEntity = entityCaptor.getValue();
        
        // 검증: 바디 내용
        assertThat(capturedEntity.getBody()).isNotNull();
        assertThat(capturedEntity.getBody().getElderlyId()).isEqualTo(3L);
        assertThat(capturedEntity.getBody().getElderlyName()).isEqualTo("HongGilDong");
        assertThat(capturedEntity.getBody().getPhone()).isEqualTo("+811053915653");
        
        // 검증: 헤더 (Content-Type)
        assertThat(capturedEntity.getHeaders().getContentType()).isEqualTo(org.springframework.http.MediaType.APPLICATION_JSON);
    }
}
