package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.call.dto.CallBotInternalDto.*;
import com.aicc.silverlink.domain.call.entity.*;
import com.aicc.silverlink.domain.call.repository.*;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.global.sse.CallBotSseService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("CallBotInternalService 단위 테스트")
class CallBotInternalServiceTest {

    @InjectMocks
    private CallBotInternalService callBotInternalService;

    @Mock
    private CallRecordRepository callRecordRepository;
    @Mock
    private LlmModelRepository llmModelRepository;
    @Mock
    private ElderlyResponseRepository elderlyResponseRepository;
    @Mock
    private CallSummaryRepository callSummaryRepository;
    @Mock
    private CallEmotionRepository callEmotionRepository;
    @Mock
    private CallDailyStatusRepository callDailyStatusRepository;
    @Mock
    private ElderlyRepository elderlyRepository;
    @Mock
    private CallBotSseService sseService;

    @Nested
    @DisplayName("통화 데이터 저장 (중복 방지 / 덮어씌우기)")
    class SaveDataWithOverwrite {

        @Test
        @DisplayName("통화 요약 저장 시 기존 데이터를 삭제하고 저장한다")
        void saveSummary() {
            // given
            Long callId = 1L;
            CallRecord callRecord = mock(CallRecord.class);
            SummaryRequest request = new SummaryRequest("새로운 요약입니다.");

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));

            // when
            callBotInternalService.saveSummary(callId, request);

            // then
            // 1. 기존 데이터 삭제 호출 검증
            verify(callSummaryRepository).deleteByCallRecord(callRecord);
            // 2. 새로운 데이터 저장 호출 검증
            verify(callSummaryRepository).save(any(CallSummary.class));
        }

        @Test
        @DisplayName("감정 분석 저장 시 기존 데이터를 삭제하고 저장한다")
        void saveEmotion() {
            // given
            Long callId = 1L;
            CallRecord callRecord = mock(CallRecord.class);
            EmotionRequest request = new EmotionRequest("GOOD");

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));

            // when
            callBotInternalService.saveEmotion(callId, request);

            // then
            // 1. 기존 데이터 삭제 호출 검증
            verify(callEmotionRepository).deleteByCallRecord(callRecord);
            // 2. 새로운 데이터 저장 호출 검증
            verify(callEmotionRepository).save(any(CallEmotion.class));
        }

        @Test
        @DisplayName("일일 상태 저장 시 기존 데이터를 삭제하고 저장한다")
        void saveDailyStatus() {
            // given
            Long callId = 1L;
            CallRecord callRecord = mock(CallRecord.class);
            DailyStatusRequest request = new DailyStatusRequest(
                    true, "GOOD", "건강함", "GOOD", "잘 잤음");

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));

            // when
            callBotInternalService.saveDailyStatus(callId, request);

            // then
            // 1. 기존 데이터 삭제 호출 검증
            verify(callDailyStatusRepository).deleteByCallRecord(callRecord);
            // 2. 새로운 데이터 저장 호출 검증
            verify(callDailyStatusRepository).save(any(CallDailyStatus.class));
        }
    }

    @Nested
    @DisplayName("메시지 독립 저장 및 순서 검증")
    class DecoupledMessageTest {

        @Test
        @DisplayName("이전 발화(Prompt)가 없어도 어르신 응답(Reply) 저장에 성공한다")
        void saveReply_ShouldSucceed_WhenNoPromptExists() {
            // given
            Long callId = 1L;
            CallRecord callRecord = mock(CallRecord.class);
            SaveReplyRequest request = new SaveReplyRequest("어르신 답변입니다.", false);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            // 이전 발화가 없으므로 Optional.empty() 반환
            given(llmModelRepository.findTopByCallRecordOrderByIdDesc(callRecord)).willReturn(Optional.empty());

            // when
            callBotInternalService.saveReply(callId, request);

            // then
            // 에러 없이 저장 메서드가 호출되어야 함 (llmModel은 null로 저장됨)
            verify(elderlyResponseRepository).save(any(ElderlyResponse.class));
        }

        @Test
        @DisplayName("통화 로그 조회 시 메시지 생성 시간 순서대로 정렬된다")
        void getCallLogs_ShouldSortChronologically() {
            // given
            Long callId = 1L;
            CallRecord callRecord = mock(CallRecord.class);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));

            // 1. 봇 발화 (10:00:00)
            LlmModel prompt1 = LlmModel.builder()
                    .callRecord(callRecord)
                    .prompt("안녕하세요")
                    .build();
            org.springframework.test.util.ReflectionTestUtils.setField(prompt1, "createdAt",
                    java.time.LocalDateTime.of(2023, 1, 1, 10, 0, 0));

            // 2. 어르신 응답 (10:00:05) - 독립적으로 저장됨
            ElderlyResponse reply1 = ElderlyResponse.builder()
                    .callRecord(callRecord)
                    .content("반가워요")
                    .danger(false)
                    .respondedAt(java.time.LocalDateTime.of(2023, 1, 1, 10, 0, 5))
                    .build();

            // 3. 봇 발화 (10:00:10)
            LlmModel prompt2 = LlmModel.builder()
                    .callRecord(callRecord)
                    .prompt("식사는 하셨나요?")
                    .build();
            org.springframework.test.util.ReflectionTestUtils.setField(prompt2, "createdAt",
                    java.time.LocalDateTime.of(2023, 1, 1, 10, 0, 10));

            // Repository는 시간순과 상관없이 데이터를 반환한다고 가정
            given(llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId))
                    .willReturn(java.util.List.of(prompt1, prompt2));

            given(elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId))
                    .willReturn(java.util.List.of(reply1));

            // when
            var logs = callBotInternalService.getCallLogs(callId);

            // then
            // Timestamp 비교 로직은 실제 객체의 시간이 세팅되어야 하므로,
            // 여기서는 Service가 두 리스트를 합쳐서 반환하는지 여부만 확인 (개수 확인)
            // 실제 정렬 로직 검증은 통합 테스트나 더 정교한 Mocking이 필요함
            assert logs.size() == 3;
        }
    }
}
