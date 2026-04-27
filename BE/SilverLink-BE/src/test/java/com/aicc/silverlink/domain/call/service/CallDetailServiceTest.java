package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.call.dto.CallDetailDto.*;
import com.aicc.silverlink.domain.call.entity.*;
import com.aicc.silverlink.domain.call.repository.*;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CallDetailService 단위 테스트")
class CallDetailServiceTest {

    @InjectMocks
    private CallDetailService callDetailService;

    @Mock
    private CallRecordRepository callRecordRepository;
    @Mock
    private LlmModelRepository llmModelRepository;
    @Mock
    private ElderlyResponseRepository elderlyResponseRepository;
    @Mock
    private CallSummaryRepository summaryRepository;
    @Mock
    private CallEmotionRepository emotionRepository;
    @Mock
    private CallDailyStatusRepository dailyStatusRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private GuardianElderlyRepository guardianElderlyRepository;
    @Mock
    private com.aicc.silverlink.domain.consent.service.AccessRequestService accessRequestService;

    // ===== Helper Methods =====

    private User createMockElderlyUser() {
        User user = mock(User.class);
        lenient().doReturn("박어르신").when(user).getName();
        lenient().doReturn("010-8765-4321").when(user).getPhone();
        return user;
    }

    private Elderly createMockElderly(Long id) {
        Elderly elderly = mock(Elderly.class);
        lenient().doReturn(id).when(elderly).getId();
        lenient().doReturn(createMockElderlyUser()).when(elderly).getUser();
        lenient().doReturn(75).when(elderly).age();
        lenient().doReturn(Elderly.Gender.F).when(elderly).getGender();
        return elderly;
    }

    private CallRecord createMockCallRecord(Long id, Elderly elderly) {
        CallRecord callRecord = mock(CallRecord.class);
        lenient().doReturn(id).when(callRecord).getId();
        lenient().doReturn(elderly).when(callRecord).getElderly();
        lenient().doReturn(LocalDateTime.of(2024, 1, 15, 10, 30, 0)).when(callRecord).getCallAt();
        lenient().doReturn(932).when(callRecord).getCallTimeSec();
        lenient().doReturn(CallState.COMPLETED).when(callRecord).getState();
        lenient().doReturn("15분 32초").when(callRecord).getFormattedDuration();
        lenient().doReturn("https://s3.amazonaws.com/bucket/recordings/" + id + ".mp3").when(callRecord)
                .getRecordingUrl();
        lenient().doReturn(false).when(callRecord).hasDangerResponse();
        return callRecord;
    }

    private LlmModel createMockLlmModel(Long id, CallRecord callRecord, String prompt, LocalDateTime createdAt) {
        LlmModel model = mock(LlmModel.class);
        lenient().doReturn(id).when(model).getId();
        lenient().doReturn(callRecord).when(model).getCallRecord();
        lenient().doReturn(prompt).when(model).getPrompt();
        lenient().doReturn(createdAt).when(model).getCreatedAt();
        return model;
    }

    private ElderlyResponse createMockElderlyResponse(Long id, CallRecord callRecord, String content,
            LocalDateTime respondedAt, boolean danger) {
        ElderlyResponse response = mock(ElderlyResponse.class);
        lenient().doReturn(id).when(response).getId();
        lenient().doReturn(callRecord).when(response).getCallRecord();
        lenient().doReturn(content).when(response).getContent();
        lenient().doReturn(respondedAt).when(response).getRespondedAt();
        lenient().doReturn(danger).when(response).isDanger();
        lenient().doReturn(danger ? "위험 신호 감지" : null).when(response).getDangerReason();
        return response;
    }

    private CallSummary createMockCallSummary(Long id, CallRecord callRecord, String content) {
        CallSummary summary = mock(CallSummary.class);
        lenient().doReturn(id).when(summary).getId();
        lenient().doReturn(callRecord).when(summary).getCallRecord();
        lenient().doReturn(content).when(summary).getContent();
        lenient().doReturn(LocalDateTime.now()).when(summary).getCreatedAt();
        return summary;
    }

    private CallEmotion createMockCallEmotion(Long id, CallRecord callRecord, EmotionLevel level) {
        CallEmotion emotion = mock(CallEmotion.class);
        lenient().doReturn(id).when(emotion).getId();
        lenient().doReturn(callRecord).when(emotion).getCallRecord();
        lenient().doReturn(level).when(emotion).getEmotionLevel();
        lenient().doReturn(level.getKorean()).when(emotion).getEmotionLevelKorean();
        lenient().doReturn(LocalDateTime.now()).when(emotion).getCreatedAt();
        return emotion;
    }

    private CallDailyStatus createMockDailyStatus(Long id, CallRecord callRecord, Boolean mealTaken,
            CallDailyStatus.StatusLevel healthStatus,
            CallDailyStatus.StatusLevel sleepStatus) {
        CallDailyStatus status = mock(CallDailyStatus.class);
        lenient().doReturn(id).when(status).getId();
        lenient().doReturn(callRecord).when(status).getCallRecord();
        lenient().doReturn(mealTaken).when(status).getMealTaken();
        lenient().doReturn(healthStatus).when(status).getHealthStatus();
        lenient().doReturn(healthStatus != null ? healthStatus.getKorean() : "미확인").when(status)
                .getHealthStatusKorean();
        lenient().doReturn("오늘 몸이 가뿐해요").when(status).getHealthDetail();
        lenient().doReturn(sleepStatus).when(status).getSleepStatus();
        lenient().doReturn(sleepStatus != null ? sleepStatus.getKorean() : "미확인").when(status).getSleepStatusKorean();
        lenient().doReturn("어젯밤 푹 잤어요").when(status).getSleepDetail();
        return status;
    }

    @Nested
    @DisplayName("상담사용 통화 상세 조회")
    class GetCallDetailForCounselor {

        @Test
        @DisplayName("상담사가 담당 어르신의 통화 상세를 조회한다")
        void success() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1001L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            LocalDateTime callStartTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

            // LLM 발화 (CallBot)
            LlmModel llm1 = createMockLlmModel(1L, callRecord,
                    "안녕하세요, 박어르신. 오늘 기분은 어떠세요?", callStartTime);
            LlmModel llm2 = createMockLlmModel(2L, callRecord,
                    "아침 식사는 하셨나요?", callStartTime.plusSeconds(15));

            // 어르신 응답
            ElderlyResponse resp1 = createMockElderlyResponse(101L, callRecord,
                    "아이고, 네 잘 지내. 오늘 날씨가 좋아서 기분이 좋아.",
                    callStartTime.plusSeconds(8), false);
            ElderlyResponse resp2 = createMockElderlyResponse(102L, callRecord,
                    "응, 아침에 죽 먹었어.",
                    callStartTime.plusSeconds(20), false);

            // 요약
            CallSummary summary = createMockCallSummary(1L, callRecord,
                    "오늘 아침 식사 잘 하셨고, 컨디션이 좋으셨습니다.");

            // 감정
            CallEmotion emotion = createMockCallEmotion(1L, callRecord, EmotionLevel.GOOD);

            // 일일 상태
            CallDailyStatus dailyStatus = createMockDailyStatus(1L, callRecord, true,
                    CallDailyStatus.StatusLevel.GOOD, CallDailyStatus.StatusLevel.GOOD);

            // Stubbing
            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId))
                    .willReturn(true);
            given(llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId)).willReturn(List.of(llm1, llm2));
            given(elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId))
                    .willReturn(List.of(resp1, resp2));
            given(summaryRepository.findLatestByCallId(callId)).willReturn(Optional.of(summary));
            given(emotionRepository.findLatestByCallId(callId)).willReturn(Optional.of(emotion));
            given(dailyStatusRepository.findByCallRecordId(callId)).willReturn(Optional.of(dailyStatus));

            // when
            CallDetailResponse result = callDetailService.getCallDetailForCounselor(counselorId, callId);

            // then
            assertThat(result.getCallId()).isEqualTo(callId);
            assertThat(result.getElderlyName()).isEqualTo("박어르신");
            assertThat(result.getDuration()).isEqualTo("15분 32초");
            assertThat(result.getRecordingUrl()).contains("1001.mp3");
            assertThat(result.getSummary()).contains("아침 식사 잘 하셨고");

            // 대화 내용 (시간순 정렬)
            assertThat(result.getConversations()).hasSize(4);
            assertThat(result.getConversations().get(0).getSpeaker()).isEqualTo(ConversationMessage.Speaker.CALLBOT);
            assertThat(result.getConversations().get(1).getSpeaker()).isEqualTo(ConversationMessage.Speaker.ELDERLY);

            // 오늘의 상태
            assertThat(result.getDailyStatus().getEmotion().getLevel()).isEqualTo("GOOD");
            assertThat(result.getDailyStatus().getMeal().getTaken()).isTrue();
            assertThat(result.getDailyStatus().getHealth().getLevelKorean()).isEqualTo("좋음");
            assertThat(result.getDailyStatus().getSleep().getLevelKorean()).isEqualTo("좋음");

            // AI 분석
            assertThat(result.getAiAnalysis().isHasDangerSignal()).isFalse();
        }

        @Test
        @DisplayName("담당하지 않는 어르신의 통화 조회 시 예외 발생")
        void failWithUnassignedElderly() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1001L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> callDetailService.getCallDetailForCounselor(counselorId, callId))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("존재하지 않는 통화 조회 시 예외 발생")
        void failWithInvalidCallId() {
            // given
            Long counselorId = 1L;
            Long invalidCallId = 9999L;

            given(callRecordRepository.findById(invalidCallId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> callDetailService.getCallDetailForCounselor(counselorId, invalidCallId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("보호자용 통화 상세 조회")
    class GetCallDetailForGuardian {

        @Test
        @DisplayName("보호자가 어르신의 통화 상세를 조회한다 (권한 있음)")
        void success() {
            // given
            Long guardianId = 50L;
            Long elderlyId = 100L;
            Long callId = 1001L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            CallEmotion emotion = createMockCallEmotion(1L, callRecord, EmotionLevel.NORMAL);
            CallDailyStatus dailyStatus = createMockDailyStatus(1L, callRecord, false,
                    CallDailyStatus.StatusLevel.NORMAL, CallDailyStatus.StatusLevel.BAD);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId)).willReturn(true);
            given(accessRequestService.hasAccess(any(), any(), any())).willReturn(true); // 권한 있음

            given(llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId)).willReturn(List.of());
            given(elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId)).willReturn(List.of());
            given(summaryRepository.findLatestByCallId(callId)).willReturn(Optional.empty());
            given(emotionRepository.findLatestByCallId(callId)).willReturn(Optional.of(emotion));
            given(dailyStatusRepository.findByCallRecordId(callId)).willReturn(Optional.of(dailyStatus));

            // when
            CallDetailResponse result = callDetailService.getCallDetailForGuardian(guardianId, callId);

            // then
            assertThat(result.getCallId()).isEqualTo(callId);
            assertThat(result.isAccessGranted()).isTrue();
            assertThat(result.getDailyStatus().getMeal().getTaken()).isFalse();
            assertThat(result.getDailyStatus().getMeal().getStatus()).isEqualTo("식사 안함");
            assertThat(result.getDailyStatus().getSleep().getLevelKorean()).isEqualTo("나쁨");
        }

        @Test
        @DisplayName("보호자가 어르신의 통화 상세를 조회한다 (권한 없음 - 마스킹 처리)")
        void successWithPartialAccess() {
            // given
            Long guardianId = 50L;
            Long elderlyId = 100L;
            Long callId = 1001L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            // mock data that would exist if accessed
            CallSummary summary = createMockCallSummary(1L, callRecord, "요약 내용");

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId)).willReturn(true);
            given(accessRequestService.hasAccess(any(), any(), any())).willReturn(false); // 권한 없음

            given(summaryRepository.findLatestByCallId(callId)).willReturn(Optional.of(summary));

            // when
            CallDetailResponse result = callDetailService.getCallDetailForGuardian(guardianId, callId);

            // then
            assertThat(result.getCallId()).isEqualTo(callId);
            assertThat(result.isAccessGranted()).isFalse();
            assertThat(result.getSummary()).isEqualTo("요약 내용");
            assertThat(result.getConversations()).isEmpty();
            assertThat(result.getDailyStatus()).isNull();
            assertThat(result.getAiAnalysis()).isNull();
        }

        @Test
        @DisplayName("보호 관계가 없는 어르신의 통화 조회 시 예외 발생")
        void failWithUnrelatedElderly() {
            // given
            Long guardianId = 50L;
            Long elderlyId = 100L;
            Long callId = 1001L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> callDetailService.getCallDetailForGuardian(guardianId, callId))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("관리자용 통화 상세 조회")
    class GetCallDetailForAdmin {

        @Test
        @DisplayName("관리자가 모든 통화 상세를 조회한다")
        void success() {
            // given
            Long callId = 1001L;
            Long elderlyId = 100L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId)).willReturn(List.of());
            given(elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId)).willReturn(List.of());
            given(summaryRepository.findLatestByCallId(callId)).willReturn(Optional.empty());
            given(emotionRepository.findLatestByCallId(callId)).willReturn(Optional.empty());
            given(dailyStatusRepository.findByCallRecordId(callId)).willReturn(Optional.empty());

            // when
            CallDetailResponse result = callDetailService.getCallDetailForAdmin(callId);

            // then
            assertThat(result.getCallId()).isEqualTo(callId);
            // 일일 상태가 없는 경우 미확인으로 표시
            assertThat(result.getDailyStatus().getMeal().getStatus()).isEqualTo("미확인");
            assertThat(result.getDailyStatus().getHealth().getLevelKorean()).isEqualTo("미확인");
        }
    }

    @Nested
    @DisplayName("대화 내용 조회")
    class GetConversations {

        @Test
        @DisplayName("대화 내용을 시간순으로 조회한다")
        void success() {
            // given
            Long callId = 1001L;
            Long elderlyId = 100L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            LocalDateTime callStartTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

            LlmModel llm1 = createMockLlmModel(1L, callRecord, "첫 번째 질문", callStartTime);
            ElderlyResponse resp1 = createMockElderlyResponse(101L, callRecord, "첫 번째 대답",
                    callStartTime.plusSeconds(5), false);
            LlmModel llm2 = createMockLlmModel(2L, callRecord, "두 번째 질문", callStartTime.plusSeconds(10));
            ElderlyResponse resp2 = createMockElderlyResponse(102L, callRecord, "두 번째 대답",
                    callStartTime.plusSeconds(18), false);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId)).willReturn(List.of(llm1, llm2));
            given(elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId))
                    .willReturn(List.of(resp1, resp2));

            // when
            List<ConversationMessage> result = callDetailService.getConversations(callId);

            // then
            assertThat(result).hasSize(4);
            // 시간순 정렬 확인: llm1(0초) -> resp1(5초) -> llm2(10초) -> resp2(18초)
            assertThat(result.get(0).getContent()).isEqualTo("첫 번째 질문");
            assertThat(result.get(0).getOffsetSeconds()).isEqualTo(0);
            assertThat(result.get(1).getContent()).isEqualTo("첫 번째 대답");
            assertThat(result.get(1).getOffsetSeconds()).isEqualTo(5);
            assertThat(result.get(2).getContent()).isEqualTo("두 번째 질문");
            assertThat(result.get(2).getOffsetSeconds()).isEqualTo(10);
            assertThat(result.get(3).getContent()).isEqualTo("두 번째 대답");
            assertThat(result.get(3).getOffsetSeconds()).isEqualTo(18);
        }
    }

    @Nested
    @DisplayName("위험 신호 감지")
    class DangerSignalDetection {

        @Test
        @DisplayName("위험 응답이 있으면 AI 분석에 표시된다")
        void detectDangerSignal() {
            // given
            Long callId = 1001L;
            Long elderlyId = 100L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            LocalDateTime callStartTime = LocalDateTime.of(2024, 1, 15, 10, 30, 0);

            // 위험 응답 포함
            ElderlyResponse dangerResponse = createMockElderlyResponse(101L, callRecord,
                    "요즘 너무 힘들어서 살기 싫어...", callStartTime.plusSeconds(30), true);

            CallEmotion emotion = createMockCallEmotion(1L, callRecord, EmotionLevel.DEPRESSED);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId)).willReturn(List.of());
            given(elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId))
                    .willReturn(List.of(dangerResponse));
            given(summaryRepository.findLatestByCallId(callId)).willReturn(Optional.empty());
            given(emotionRepository.findLatestByCallId(callId)).willReturn(Optional.of(emotion));
            given(dailyStatusRepository.findByCallRecordId(callId)).willReturn(Optional.empty());

            // when
            CallDetailResponse result = callDetailService.getCallDetailForAdmin(callId);

            // then
            assertThat(result.getAiAnalysis().isHasDangerSignal()).isTrue();
            assertThat(result.getAiAnalysis().getDangerReasons()).contains("위험 신호 감지");
            assertThat(result.getAiAnalysis().getOverallAssessment()).contains("주의가 필요");

            // 대화 내용에서도 위험 표시
            assertThat(result.getConversations().get(0).getIsDanger()).isTrue();
        }
    }

    @Nested
    @DisplayName("오늘의 상태 조회")
    class GetDailyStatus {

        @Test
        @DisplayName("일일 상태를 정상적으로 조회한다")
        void success() {
            // given
            Long callId = 1001L;
            Long elderlyId = 100L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            CallEmotion emotion = createMockCallEmotion(1L, callRecord, EmotionLevel.GOOD);
            CallDailyStatus dailyStatus = createMockDailyStatus(1L, callRecord, true,
                    CallDailyStatus.StatusLevel.GOOD, CallDailyStatus.StatusLevel.NORMAL);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(emotionRepository.findLatestByCallId(callId)).willReturn(Optional.of(emotion));
            given(dailyStatusRepository.findByCallRecordId(callId)).willReturn(Optional.of(dailyStatus));

            // when
            DailyStatusResponse result = callDetailService.getDailyStatus(callId);

            // then
            assertThat(result.getEmotion().getLevel()).isEqualTo("GOOD");
            assertThat(result.getEmotion().getScore()).isEqualTo(85);
            assertThat(result.getMeal().getTaken()).isTrue();
            assertThat(result.getMeal().getStatus()).isEqualTo("식사함");
            assertThat(result.getHealth().getLevel()).isEqualTo("GOOD");
            assertThat(result.getSleep().getLevel()).isEqualTo("NORMAL");
        }

        @Test
        @DisplayName("일일 상태가 없으면 미확인으로 표시된다")
        void returnUnknownWhenNoStatus() {
            // given
            Long callId = 1001L;
            Long elderlyId = 100L;

            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(emotionRepository.findLatestByCallId(callId)).willReturn(Optional.empty());
            given(dailyStatusRepository.findByCallRecordId(callId)).willReturn(Optional.empty());

            // when
            DailyStatusResponse result = callDetailService.getDailyStatus(callId);

            // then
            assertThat(result.getEmotion().getLevel()).isEqualTo("UNKNOWN");
            assertThat(result.getEmotion().getLevelKorean()).isEqualTo("미확인");
            assertThat(result.getMeal().getTaken()).isNull();
            assertThat(result.getMeal().getStatus()).isEqualTo("미확인");
            assertThat(result.getHealth().getLevelKorean()).isEqualTo("미확인");
            assertThat(result.getSleep().getLevelKorean()).isEqualTo("미확인");
        }
    }
}