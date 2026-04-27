package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.call.dto.CallReviewDto.*;
import com.aicc.silverlink.domain.call.entity.*;
import com.aicc.silverlink.domain.call.repository.*;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
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
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("CallReviewService 단위 테스트")
class CallReviewServiceTest {

    @InjectMocks
    private CallReviewService callReviewService;

    @Mock
    private CallRecordRepository callRecordRepository;
    @Mock
    private CounselorCallReviewRepository reviewRepository;
    @Mock
    private ElderlyResponseRepository elderlyResponseRepository;
    @Mock
    private CallSummaryRepository summaryRepository;
    @Mock
    private CallEmotionRepository emotionRepository;
    @Mock
    private CounselorRepository counselorRepository;
    @Mock
    private AssignmentRepository assignmentRepository;
    @Mock
    private GuardianElderlyRepository guardianElderlyRepository;
    @Mock
    private LlmModelRepository llmModelRepository;
    @Mock
    private CallDailyStatusRepository dailyStatusRepository;
    @Mock
    private com.aicc.silverlink.domain.file.service.FileService fileService;
    @Mock
    private com.aicc.silverlink.domain.notification.service.NotificationService notificationService;

    // ===== Helper Methods =====

    // ===== Helper Methods =====

    private User createMockCounselorUser() {
        User user = mock(User.class);
        lenient().doReturn("김상담").when(user).getName();
        lenient().doReturn("010-1234-5678").when(user).getPhone();
        return user;
    }

    private User createMockElderlyUser() {
        User user = mock(User.class);
        lenient().doReturn("박어르신").when(user).getName();
        lenient().doReturn("010-8765-4321").when(user).getPhone();
        return user;
    }

    private Counselor createMockCounselor(Long id) {
        Counselor counselor = mock(Counselor.class);
        lenient().doReturn(id).when(counselor).getId();
        lenient().doReturn(createMockCounselorUser()).when(counselor).getUser();
        return counselor;
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
        lenient().doReturn(LocalDateTime.now().minusHours(1)).when(callRecord).getCallAt();
        lenient().doReturn(180).when(callRecord).getCallTimeSec();
        lenient().doReturn(CallState.COMPLETED).when(callRecord).getState();
        lenient().doReturn("3분 0초").when(callRecord).getFormattedDuration();
        lenient().doReturn(new ArrayList<LlmModel>()).when(callRecord).getLlmModels();
        lenient().doReturn(new ArrayList<ElderlyResponse>()).when(callRecord).getElderlyResponses();
        lenient().doReturn(new ArrayList<CallSummary>()).when(callRecord).getSummaries();
        lenient().doReturn(new ArrayList<CallEmotion>()).when(callRecord).getEmotions();
        lenient().doReturn(false).when(callRecord).hasDangerResponse();
        lenient().doReturn("https://s3.amazonaws.com/bucket/recordings/1001.mp3").when(callRecord).getRecordingUrl();
        lenient().doReturn(null).when(callRecord).getDailyStatus();
        return callRecord;
    }

    private CounselorCallReview createMockReview(Long id, CallRecord callRecord, Counselor counselor) {
        CounselorCallReview review = mock(CounselorCallReview.class);
        lenient().doReturn(id).when(review).getId();
        lenient().doReturn(callRecord).when(review).getCallRecord();
        lenient().doReturn(counselor).when(review).getCounselor();
        lenient().doReturn(LocalDateTime.now()).when(review).getReviewedAt();
        lenient().doReturn("어르신 상태 양호합니다.").when(review).getComment();
        lenient().doReturn(false).when(review).isUrgent();
        lenient().doReturn(LocalDateTime.now()).when(review).getCreatedAt();
        lenient().doReturn(LocalDateTime.now()).when(review).getUpdatedAt();
        return review;
    }

    @Nested
    @DisplayName("통화 목록 조회")
    class GetCallRecordsForCounselor {

        @Test
        @DisplayName("상담사가 담당 어르신의 통화 목록을 조회한다")
        void success() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;
            Pageable pageable = PageRequest.of(0, 20);

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            Page<CallRecord> callRecordPage = new PageImpl<>(List.of(callRecord));

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(callRecordRepository.findCallRecordsForCounselor(counselorId, pageable)).willReturn(callRecordPage);
            given(reviewRepository.existsByCallRecordIdAndCounselorId(callId, counselorId)).willReturn(true);

            // when
            Page<CallRecordSummaryResponse> result = callReviewService.getCallRecordsForCounselor(counselorId,
                    pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCallId()).isEqualTo(callId);
            assertThat(result.getContent().get(0).isReviewed()).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 상담사로 조회 시 예외 발생")
        void failWithInvalidCounselor() {
            // given
            Long invalidCounselorId = 999L;
            Pageable pageable = PageRequest.of(0, 20);
            given(counselorRepository.findById(invalidCounselorId)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> callReviewService.getCallRecordsForCounselor(invalidCounselorId, pageable))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("통화 리뷰 생성")
    class CreateReview {

        @Test
        @DisplayName("상담사가 통화 리뷰를 생성한다")
        void success() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            CounselorCallReview savedReview = createMockReview(1L, callRecord, counselor);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(callId)
                    .comment("어르신께서 오늘 기분이 좋아보이셨습니다.")
                    .urgent(false)
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId))
                    .willReturn(true);
            given(reviewRepository.existsByCallRecordIdAndCounselorId(callId, counselorId)).willReturn(false);
            given(reviewRepository.save(any(CounselorCallReview.class))).willReturn(savedReview);

            // when
            ReviewResponse result = callReviewService.createReview(counselorId, request);

            // then
            assertThat(result.getReviewId()).isEqualTo(1L);
            assertThat(result.getCallId()).isEqualTo(callId);
            verify(reviewRepository).save(any(CounselorCallReview.class));

            // Verify notification trigger (indirectly via log or mock interaction if set
            // up)
            // Note: Since we didn't mock guardianElderlyRepository.findByElderlyId in this
            // specific test yet,
            // the notification part silently passes (due to optional).
            // We should add a specific test for notification below.
        }

        @Test
        @DisplayName("리뷰 생성 시 보호자에게 알림이 전송된다")
        void successWithNotification() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;
            Long guardianId = 50L;
            Long guardianUserId = 500L;

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            CounselorCallReview savedReview = createMockReview(1L, callRecord, counselor);

            // Mock Guardian & User
            com.aicc.silverlink.domain.guardian.entity.Guardian guardian = mock(
                    com.aicc.silverlink.domain.guardian.entity.Guardian.class);
            User guardianUser = mock(User.class);
            when(guardianUser.getId()).thenReturn(guardianUserId);
            when(guardian.getUser()).thenReturn(guardianUser);

            com.aicc.silverlink.domain.guardian.entity.GuardianElderly guardianElderly = mock(
                    com.aicc.silverlink.domain.guardian.entity.GuardianElderly.class);
            when(guardianElderly.getGuardian()).thenReturn(guardian);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(callId)
                    .comment("알림 테스트")
                    .urgent(false)
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId))
                    .willReturn(true);
            given(reviewRepository.existsByCallRecordIdAndCounselorId(callId, counselorId)).willReturn(false);
            given(reviewRepository.save(any(CounselorCallReview.class))).willReturn(savedReview);

            // Mock finding guardian
            given(guardianElderlyRepository.findByElderlyId(elderlyId)).willReturn(Optional.of(guardianElderly));

            // when
            callReviewService.createReview(counselorId, request);

            // then
            verify(notificationService).createCounselorCommentNotification(eq(guardianUserId), eq(callId), any());
        }

        @Test
        @DisplayName("이미 리뷰가 존재하면 예외 발생")
        void failWithDuplicateReview() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(callId)
                    .comment("중복 리뷰")
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId))
                    .willReturn(true);
            given(reviewRepository.existsByCallRecordIdAndCounselorId(callId, counselorId)).willReturn(true);

            // when & then
            assertThatThrownBy(() -> callReviewService.createReview(counselorId, request))
                    .isInstanceOf(BusinessException.class);
        }

        @Test
        @DisplayName("담당하지 않는 어르신의 통화에 리뷰 작성 시 예외 발생")
        void failWithUnassignedElderly() {
            // given
            Long counselorId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(callId)
                    .comment("권한 없음")
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(callRecordRepository.findById(callId)).willReturn(Optional.of(callRecord));
            given(assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId))
                    .willReturn(false);

            // when & then
            assertThatThrownBy(() -> callReviewService.createReview(counselorId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("통화 리뷰 수정")
    class UpdateReview {

        @Test
        @DisplayName("상담사가 본인의 리뷰를 수정한다")
        void success() {
            // given
            Long counselorId = 1L;
            Long reviewId = 1L;
            Long elderlyId = 100L;
            Long callId = 1000L;

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            CounselorCallReview review = createMockReview(reviewId, callRecord, counselor);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(callId)
                    .comment("수정된 코멘트입니다.")
                    .urgent(true)
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(review));

            // when
            ReviewResponse result = callReviewService.updateReview(counselorId, reviewId, request);

            // then
            verify(review).updateComment("수정된 코멘트입니다.", true);
        }

        @Test
        @DisplayName("다른 상담사의 리뷰 수정 시 예외 발생")
        void failWithOtherCounselorReview() {
            // given
            Long counselorId = 1L;
            Long otherCounselorId = 2L;
            Long reviewId = 2L;

            Counselor counselor = createMockCounselor(counselorId);
            Counselor otherCounselor = createMockCounselor(otherCounselorId);

            Elderly elderly = createMockElderly(100L);
            CallRecord callRecord = createMockCallRecord(1000L, elderly);
            CounselorCallReview otherReview = createMockReview(reviewId, callRecord, otherCounselor);

            ReviewRequest request = ReviewRequest.builder()
                    .callId(1000L)
                    .comment("수정 시도")
                    .build();

            given(counselorRepository.findById(counselorId)).willReturn(Optional.of(counselor));
            given(reviewRepository.findById(reviewId)).willReturn(Optional.of(otherReview));

            // when & then
            assertThatThrownBy(() -> callReviewService.updateReview(counselorId, reviewId, request))
                    .isInstanceOf(BusinessException.class);
        }
    }

    @Nested
    @DisplayName("보호자용 통화 리뷰 조회")
    class GuardianCallReview {

        @Test
        @DisplayName("보호자가 어르신의 통화 리뷰를 조회한다")
        void success() {
            // given
            Long guardianId = 50L;
            Long elderlyId = 100L;
            Long counselorId = 1L;
            Long callId = 1000L;
            Pageable pageable = PageRequest.of(0, 20);

            Counselor counselor = createMockCounselor(counselorId);
            Elderly elderly = createMockElderly(elderlyId);
            CallRecord callRecord = createMockCallRecord(callId, elderly);
            CounselorCallReview review = createMockReview(1L, callRecord, counselor);

            Page<CallRecord> callRecordPage = new PageImpl<>(List.of(callRecord));

            given(guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId)).willReturn(true);
            given(callRecordRepository.findAllByElderlyId(elderlyId, pageable)).willReturn(callRecordPage);
            given(reviewRepository.findByCallRecordIdOrderByReviewedAtDesc(callId)).willReturn(List.of(review));

            // when
            Page<GuardianCallReviewResponse> result = callReviewService.getCallReviewsForGuardian(guardianId, elderlyId,
                    pageable);

            // then
            assertThat(result.getContent()).hasSize(1);
            assertThat(result.getContent().get(0).getCounselorName()).isEqualTo("김상담");
        }

        @Test
        @DisplayName("보호 관계가 없는 어르신 조회 시 예외 발생")
        void failWithUnrelatedElderly() {
            // given
            Long guardianId = 50L;
            Long elderlyId = 999L;
            Pageable pageable = PageRequest.of(0, 20);

            given(guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId)).willReturn(false);

            // when & then
            assertThatThrownBy(() -> callReviewService.getCallReviewsForGuardian(guardianId, elderlyId, pageable))
                    .isInstanceOf(BusinessException.class);
        }
    }
}