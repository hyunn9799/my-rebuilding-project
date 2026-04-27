package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.call.dto.CallReviewDto.*;
import com.aicc.silverlink.domain.call.entity.CallDailyStatus;
import com.aicc.silverlink.domain.call.entity.CallRecord;
import com.aicc.silverlink.domain.call.entity.CounselorCallReview;
import com.aicc.silverlink.domain.call.repository.*;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.global.exception.BusinessException;
import com.aicc.silverlink.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CallReviewService {

    private final CallRecordRepository callRecordRepository;
    private final CounselorCallReviewRepository reviewRepository;
    private final ElderlyResponseRepository elderlyResponseRepository;
    private final LlmModelRepository llmModelRepository;
    private final CallSummaryRepository summaryRepository;
    private final CallEmotionRepository emotionRepository;
    private final CallDailyStatusRepository dailyStatusRepository;
    private final CounselorRepository counselorRepository;
    private final AssignmentRepository assignmentRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final com.aicc.silverlink.domain.file.service.FileService fileService;
    private final com.aicc.silverlink.domain.notification.service.NotificationService notificationService;

    // ===== 상담사용 메서드 =====

    /**
     * 상담사가 담당하는 어르신들의 통화 기록 목록 조회
     */
    public Page<CallRecordSummaryResponse> getCallRecordsForCounselor(Long counselorId, Pageable pageable) {
        validateCounselor(counselorId);

        Page<CallRecord> callRecords = callRecordRepository.findCallRecordsForCounselor(counselorId, pageable);

        List<CallRecordSummaryResponse> responses = callRecords.getContent().stream()
                .map(cr -> {
                    boolean reviewed = reviewRepository.existsByCallRecordIdAndCounselorId(cr.getId(), counselorId);
                    return CallRecordSummaryResponse.from(cr, reviewed);
                })
                .toList();

        return new PageImpl<>(responses, pageable, callRecords.getTotalElements());
    }

    /**
     * 통화 기록 상세 조회
     */
    public CallRecordDetailResponse getCallRecordDetail(Long callId, Long counselorId) {
        validateCounselor(counselorId);

        CallRecord callRecord = callRecordRepository.findByIdWithDetails(callId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_RECORD_NOT_FOUND));

        // 상담사가 해당 어르신을 담당하는지 확인
        validateCounselorAssignment(counselorId, callRecord.getElderly().getId());

        // 아래 컬렉션들은 별도 쿼리로 조회 (MultipleBagFetchException 방지)

        // 1. 응답 목록 조회
        if (callRecord.getElderlyResponses().isEmpty()) {
            callRecord.getElderlyResponses().addAll(
                    elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId));
        }

        // 2. LLM 모델(AI 발화) 조회
        if (callRecord.getLlmModels().isEmpty()) {
            callRecord.getLlmModels().addAll(
                    llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId));
        }

        // 3. 요약 조회
        if (callRecord.getSummaries().isEmpty()) {
            callRecord.getSummaries().addAll(
                    summaryRepository.findByCallRecordId(callId));
        }

        // 4. 감정 분석 조회
        if (callRecord.getEmotions().isEmpty()) {
            callRecord.getEmotions().addAll(
                    emotionRepository.findByCallRecordIdOrderByCreatedAtDesc(callId));
        }

        CounselorCallReview review = reviewRepository.findByCallRecordIdAndCounselorId(callId, counselorId)
                .orElse(null);

        // 녹음 URL을 Pre-signed URL로 변환
        String presignedRecordingUrl = null;
        if (callRecord.getRecordingUrl() != null && !callRecord.getRecordingUrl().isBlank()) {
            presignedRecordingUrl = fileService.generatePresignedUrl(callRecord.getRecordingUrl());
        }

        // 오늘의 상태 (식사, 건강, 수면) 조회
        CallDailyStatus dailyStatus = dailyStatusRepository.findByCallRecordId(callId).orElse(null);

        return CallRecordDetailResponse.from(callRecord, review, presignedRecordingUrl, dailyStatus);
    }

    /**
     * 상담사가 통화 리뷰 생성 (통화 확인 체크 + 코멘트)
     */
    @Transactional
    public ReviewResponse createReview(Long counselorId, ReviewRequest request) {
        Counselor counselor = validateCounselor(counselorId);

        CallRecord callRecord = callRecordRepository.findById(request.getCallId())
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_RECORD_NOT_FOUND));

        // 상담사가 해당 어르신을 담당하는지 확인
        validateCounselorAssignment(counselorId, callRecord.getElderly().getId());

        // 이미 리뷰가 존재하는지 확인
        if (reviewRepository.existsByCallRecordIdAndCounselorId(request.getCallId(), counselorId)) {
            throw new BusinessException(ErrorCode.REVIEW_ALREADY_EXISTS);
        }

        CounselorCallReview review = CounselorCallReview.create(
                callRecord, counselor, request.getComment(), request.isUrgent());

        CounselorCallReview savedReview = reviewRepository.save(review);
        log.info("상담사 통화 리뷰 생성: counselorId={}, callId={}, urgent={}",
                counselorId, request.getCallId(), request.isUrgent());

        // [Notification] 보호자에게 알림 전송
        try {
            Long elderlyId = callRecord.getElderly().getId();
            String elderlyName = callRecord.getElderly().getUser().getName();

            // 어르신과 연결된 보호자 찾기
            guardianElderlyRepository.findByElderlyId(elderlyId).ifPresent(ge -> {
                Long guardianUserId = ge.getGuardian().getUser().getId();
                notificationService.createCounselorCommentNotification(guardianUserId, request.getCallId(),
                        elderlyName);
            });

        } catch (Exception e) {
            log.error("상담사 리뷰 알림 전송 실패: ", e);
            // 알림 실패가 리뷰 생성을 막지 않도록 예외 처리
        }

        return ReviewResponse.from(savedReview);
    }

    /**
     * 상담사가 통화 리뷰 수정
     */
    @Transactional
    public ReviewResponse updateReview(Long counselorId, Long reviewId, ReviewRequest request) {
        validateCounselor(counselorId);

        CounselorCallReview review = reviewRepository.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_NOT_FOUND));

        // 본인의 리뷰인지 확인
        if (!review.getCounselor().getId().equals(counselorId)) {
            throw new BusinessException(ErrorCode.ACCESS_DENIED);
        }

        review.updateComment(request.getComment(), request.isUrgent());
        log.info("상담사 통화 리뷰 수정: reviewId={}, counselorId={}", reviewId, counselorId);

        return ReviewResponse.from(review);
    }

    /**
     * 미확인 통화 건수 조회
     */
    public UnreviewedCountResponse getUnreviewedCount(Long counselorId) {
        validateCounselor(counselorId);

        long unreviewedCount = callRecordRepository.countUnreviewedCallsForCounselor(counselorId);
        long totalCount = callRecordRepository.findCallRecordsForCounselor(counselorId, Pageable.unpaged())
                .getTotalElements();

        return UnreviewedCountResponse.builder()
                .unreviewedCount(unreviewedCount)
                .totalCount(totalCount)
                .build();
    }

    /**
     * 오늘의 통화 건수 조회
     */
    public long getTodayCallCount(Long counselorId) {
        validateCounselor(counselorId);

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime startOfDay = now.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = now.toLocalDate().atTime(23, 59, 59);

        return callRecordRepository.countCallsForCounselorByDateRange(counselorId, startOfDay, endOfDay);
    }

    // ===== 보호자용 메서드 =====

    /**
     * 보호자가 어르신의 통화 기록 목록 조회
     * 상담사 리뷰 여부와 관계없이 완료된 모든 통화를 반환
     */
    public Page<GuardianCallReviewResponse> getCallReviewsForGuardian(Long guardianId, Long elderlyId,
            Pageable pageable) {
        // 보호자-어르신 관계 확인
        validateGuardianElderlyRelation(guardianId, elderlyId);

        // CallRecord를 직접 조회 (리뷰 여부와 무관하게 모든 통화 포함 - 진행중인 통화 포함)
        Page<CallRecord> callRecords = callRecordRepository.findAllByElderlyId(elderlyId, pageable);

        List<GuardianCallReviewResponse> responses = callRecords.getContent().stream()
                .map(callRecord -> {
                    // 리뷰가 있으면 최신 리뷰를 가져옴 (없으면 null)
                    List<CounselorCallReview> reviews = reviewRepository
                            .findByCallRecordIdOrderByReviewedAtDesc(callRecord.getId());
                    CounselorCallReview latestReview = reviews.isEmpty() ? null : reviews.get(0);

                    CallDailyStatus dailyStatus = dailyStatusRepository
                            .findByCallRecordId(callRecord.getId()).orElse(null);
                    return GuardianCallReviewResponse.from(callRecord, latestReview, dailyStatus);
                })
                .toList();

        return new PageImpl<>(responses, pageable, callRecords.getTotalElements());
    }

    /**
     * 보호자가 통화 상세 조회 (상담사 코멘트 포함)
     */
    public GuardianCallReviewResponse getCallDetailForGuardian(Long guardianId, Long callId) {
        CallRecord callRecord = callRecordRepository.findByIdWithDetails(callId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_RECORD_NOT_FOUND));

        // 보호자-어르신 관계 확인
        validateGuardianElderlyRelation(guardianId, callRecord.getElderly().getId());

        // 대화 데이터 로드 (prompts, responses)
        if (callRecord.getElderlyResponses().isEmpty()) {
            callRecord.getElderlyResponses().addAll(
                    elderlyResponseRepository.findByCallRecordIdOrderByRespondedAtAsc(callId));
        }
        if (callRecord.getLlmModels().isEmpty()) {
            callRecord.getLlmModels().addAll(
                    llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId));
        }

        // 리뷰가 있으면 함께 반환
        List<CounselorCallReview> reviews = reviewRepository.findByCallRecordIdOrderByReviewedAtDesc(callId);
        CounselorCallReview latestReview = reviews.isEmpty() ? null : reviews.get(0);

        // 오늘의 상태 (식사, 건강, 수면) 조회
        CallDailyStatus dailyStatus = dailyStatusRepository.findByCallRecordId(callId).orElse(null);

        return GuardianCallReviewResponse.from(callRecord, latestReview, dailyStatus);
    }

    // ===== Private Helper Methods =====

    private Counselor validateCounselor(Long counselorId) {
        return counselorRepository.findById(counselorId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
    }

    private void validateCounselorAssignment(Long counselorId, Long elderlyId) {
        boolean isAssigned = assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(
                counselorId, elderlyId);
        if (!isAssigned) {
            throw new BusinessException(ErrorCode.NOT_ASSIGNED_ELDERLY);
        }
    }

    private void validateGuardianElderlyRelation(Long guardianId, Long elderlyId) {
        boolean hasRelation = guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId);
        if (!hasRelation) {
            throw new BusinessException(ErrorCode.NOT_RELATED_ELDERLY);
        }
    }
}