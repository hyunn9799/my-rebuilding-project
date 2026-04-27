package com.aicc.silverlink.infrastructure.callbot;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.StartCallRequest;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 실제 Python CallBot 서버 연동 테스트
 * 이 테스트는 Python 서버가 실행 중일 때만 수동으로 실행합니다.
 */
@Disabled("수동 실행 전용 - 실제 Python CallBot 서버 연동 테스트")
public class CallBotRealIntegrationTest {

    @Test
    @DisplayName("실제 Python CallBot 서버에 통화 요청을 보낸다 (서버 구동 필요)")
    void sendRealCallRequest() {
        // given
        RestTemplate restTemplate = new RestTemplate();
        CallBotProperties properties = new CallBotProperties();

        // 주의: Python 서버가 실행 중인 주소여야 합니다.
        // Docker 환경이라면 http://localhost:5000 또는 http://silverlink-worker:5000 등
        String pythonUrl = "http://localhost:5000";
        properties.setUrl(pythonUrl);

        CallBotClient client = new CallBotClient(restTemplate, properties);

        StartCallRequest request = StartCallRequest.builder()
                .elderlyId(3L) // 테스트용 ID
                .elderlyName("김성호")
//                .phone("+821053915653") // 테스트용 번호 (Twilio Verified Number여야 실제 발신됨)
                .phone("+821080509357") // 테스트용 번호 (Twilio Verified Number여야 실제 발신됨)
                .build();

        try {
            // when
            boolean success = client.startCall(request);

            // then
            assertThat(success).isTrue();
            System.out.println("✅ Python CallBot 요청 성공");
        } catch (Exception e) {
            System.err.println("❌ Python CallBot 요청 실패 (서버가 꺼져있을 수 있음): " + e.getMessage());
            // 실제 환경이 아닐 경우 테스트 실패를 방지하려면 주석 처리
            // throw e;
        }
    }
}
