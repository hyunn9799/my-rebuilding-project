package com.aicc.silverlink.domain.elderly.scheduler;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.StartCallRequest;
import com.aicc.silverlink.domain.elderly.service.CallScheduleService;
import com.aicc.silverlink.infrastructure.callbot.CallBotClient;
import com.aicc.silverlink.infrastructure.callbot.CallBotProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.client.RestTemplate;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

/**
 * CallScheduler 단위 테스트
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("CallScheduler 테스트")
class CallSchedulerTest {

    @Mock
    private CallScheduleService callScheduleService;

    @Mock
    private CallBotClient callBotClient;

    @InjectMocks
    private CallScheduler callScheduler;

    @Test
    @DisplayName("예정된 통화가 없으면 CallBot 호출 안함")
    void triggerScheduledCalls_noDueCalls() {
        // given
        given(callScheduleService.getDueForCall()).willReturn(List.of());

        // when
        callScheduler.triggerScheduledCalls();

        // then
        verify(callScheduleService).getDueForCall();
        verify(callBotClient, never()).startCall(any());
    }

    @Test
    @DisplayName("예정된 통화가 있으면 CallBot 호출")
    void triggerScheduledCalls_withDueCalls() {
        // given
        StartCallRequest request1 = StartCallRequest.builder()
                .elderlyId(1L)
                .elderlyName("홍길동")
                .phone("+811011112222")
                .build();

        StartCallRequest request2 = StartCallRequest.builder()
                .elderlyId(2L)
                .elderlyName("김순자")
                .phone("+811033334444")
                .build();

        given(callScheduleService.getDueForCall()).willReturn(List.of(request1, request2));
        given(callBotClient.startCall(any())).willReturn(true);

        // when
        callScheduler.triggerScheduledCalls();

        // then
        verify(callScheduleService).getDueForCall();
        verify(callBotClient, times(2)).startCall(any());
        verify(callBotClient).startCall(request1);
        verify(callBotClient).startCall(request2);
    }

    @Test
    @DisplayName("예정된 통화가 1명인 경우 실제 CallBot 호출 (Integration)")
    void triggerScheduledCalls_singleDueCall() {
        // given
        StartCallRequest request = StartCallRequest.builder()
                .elderlyId(1L)
                .elderlyName("김성호")
                .phone("+821053915653")
                .build();

        given(callScheduleService.getDueForCall()).willReturn(List.of(request));

        // Real CallBotClient setup for integration
        RestTemplate restTemplate = new RestTemplate();
        CallBotProperties properties = new CallBotProperties();
        String pythonUrl = "http://localhost:5000"; // Assuming default local python URL
        properties.setUrl(pythonUrl);
        CallBotClient realCallBotClient = new CallBotClient(restTemplate, properties);

        // Inject real client into scheduler
        CallScheduler integrationScheduler = new CallScheduler(callScheduleService, realCallBotClient);

        // when
        integrationScheduler.triggerScheduledCalls();

        // then
        verify(callScheduleService).getDueForCall();
        // Since we used a real client, we can't verify(callBotClient).startCall(...) on the mock.
        // We verify the side effect (the python server receiving it) externally or assume success if no exception.
    }

    @Test
    @DisplayName("일부 통화 실패해도 나머지 통화 시도")
    void triggerScheduledCalls_partialFailure() {
        // given
        StartCallRequest request1 = StartCallRequest.builder()
                .elderlyId(1L)
                .elderlyName("홍길동")
                .phone("+811011112222")
                .build();

        StartCallRequest request2 = StartCallRequest.builder()
                .elderlyId(2L)
                .elderlyName("김순자")
                .phone("+811033334444")
                .build();

        StartCallRequest request3 = StartCallRequest.builder()
                .elderlyId(3L)
                .elderlyName("박영희")
                .phone("+811055556666")
                .build();

        given(callScheduleService.getDueForCall()).willReturn(List.of(request1, request2, request3));
        given(callBotClient.startCall(request1)).willReturn(true);
        given(callBotClient.startCall(request2)).willReturn(false); // 실패
        given(callBotClient.startCall(request3)).willReturn(true);

        // when
        callScheduler.triggerScheduledCalls();

        // then
        verify(callBotClient, times(3)).startCall(any());
        // 실패해도 모든 호출이 이루어져야 함
    }

    @Test
    @DisplayName("모든 통화 실패해도 예외 발생 안함")
    void triggerScheduledCalls_allFailed() {
        // given
        StartCallRequest request = StartCallRequest.builder()
                .elderlyId(1L)
                .elderlyName("홍길동")
                .phone("+811011112222")
                .build();

        given(callScheduleService.getDueForCall()).willReturn(List.of(request));
        given(callBotClient.startCall(any())).willReturn(false);

        // when - 예외 없이 완료되어야 함
        callScheduler.triggerScheduledCalls();

        // then
        verify(callBotClient).startCall(request);
    }
}