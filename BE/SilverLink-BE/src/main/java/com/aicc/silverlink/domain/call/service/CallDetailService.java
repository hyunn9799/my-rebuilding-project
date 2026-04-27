package com.aicc.silverlink.domain.call.service;

import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.call.dto.CallDetailDto.*;
import com.aicc.silverlink.domain.call.entity.*;
import com.aicc.silverlink.domain.call.repository.*;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.global.exception.BusinessException;
import com.aicc.silverlink.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static com.aicc.silverlink.domain.call.dto.CallDetailDto.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class CallDetailService {

    private final CallRecordRepository callRecordRepository;
    private final LlmModelRepository llmModelRepository;
    private final ElderlyResponseRepository elderlyResponseRepository;
    private final CallSummaryRepository summaryRepository;
    private final CallEmotionRepository emotionRepository;
    private final CallDailyStatusRepository dailyStatusRepository;
    private final AssignmentRepository assignmentRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final com.aicc.silverlink.domain.consent.service.AccessRequestService accessRequestService;

    /**
     * 상담사용 - 통화 상세 조회
     */
    public CallDetailResponse getCallDetailForCounselor(Long counselorId, Long callId) {
        log.info("상담사 통화 상세 조회: counselorId={}, callId={}", counselorId, callId);

        CallRecord callRecord = findCallRecordById(callId);

        // 담당 어르신인지 확인
        Long elderlyId = callRecord.getElderly().getId();
        if (!assignmentRepository.existsByCounselorIdAndElderlyIdAndStatusActive(counselorId, elderlyId)) {
            throw new BusinessException(ErrorCode.NOT_ASSIGNED_ELDERLY);
        }

        return buildCallDetailResponse(callRecord, true);
    }

    /**
     * 보호자용 - 통화 상세 조회
     */
    public CallDetailResponse getCallDetailForGuardian(Long guardianId, Long callId) {
        log.info("보호자 통화 상세 조회: guardianId={}, callId={}", guardianId, callId);

        CallRecord callRecord = findCallRecordById(callId);

        // 보호 관계인지 확인
        Long elderlyId = callRecord.getElderly().getId();
        if (!guardianElderlyRepository.existsByGuardianIdAndElderlyId(guardianId, elderlyId)) {
            throw new BusinessException(ErrorCode.NOT_RELATED_ELDERLY);
        }

        // 민감 정보 조회 권한 확인
        boolean hasAccess = accessRequestService.hasAccess(
                guardianId,
                elderlyId,
                com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope.CALL_RECORDS);

        return buildCallDetailResponse(callRecord, hasAccess);
    }

    /**
     * 관리자용 - 통화 상세 조회 (권한 검증 없음)
     */
    public CallDetailResponse getCallDetailForAdmin(Long callId) {
        log.info("관리자 통화 상세 조회: callId={}", callId);

        CallRecord callRecord = findCallRecordById(callId);
        return buildCallDetailResponse(callRecord, true);
    }

    /**
     * 대화 내용만 조회
     */
    public List<ConversationMessage> getConversations(Long callId) {
        CallRecord callRecord = findCallRecordById(callId);

        List<LlmModel> llmModels = llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId);
        List<ElderlyResponse> elderlyResponses = elderlyResponseRepository
                .findByCallRecordIdOrderByRespondedAtAsc(callId);

        return mergeConversations(llmModels, elderlyResponses, callRecord.getCallAt());
    }

    /**
     * 오늘의 상태만 조회
     */
    public DailyStatusResponse getDailyStatus(Long callId) {
        findCallRecordById(callId); // 존재 확인
        return buildDailyStatus(callId);
    }

    // ===== Private Helper Methods =====

    private CallRecord findCallRecordById(Long callId) {
        return callRecordRepository.findById(callId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CALL_RECORD_NOT_FOUND));
    }

    private CallDetailResponse buildCallDetailResponse(CallRecord callRecord, boolean hasAccess) {
        Long callId = callRecord.getId();

        // 1. 통화 요약 (항상 제공)
        String summary = summaryRepository.findLatestByCallId(callId)
                .map(CallSummary::getContent)
                .orElse(null);

        if (!hasAccess) {
            return CallDetailResponse.from(
                    callRecord,
                    summary,
                    List.of(), // 대화 내용 숨김
                    null, // 상태 정보 숨김
                    null, // AI 분석 숨김
                    false);
        }

        // 2. 대화 내용
        List<LlmModel> llmModels = llmModelRepository.findByCallIdOrderByCreatedAtAsc(callId);
        List<ElderlyResponse> elderlyResponses = elderlyResponseRepository
                .findByCallRecordIdOrderByRespondedAtAsc(callId);
        List<ConversationMessage> conversations = mergeConversations(
                llmModels, elderlyResponses, callRecord.getCallAt());

        // 3. 오늘의 상태
        DailyStatusResponse dailyStatus = buildDailyStatus(callId);

        // 4. AI 분석 결과
        AiAnalysisResponse aiAnalysis = buildAiAnalysis(callId, elderlyResponses);

        return CallDetailResponse.from(callRecord, summary, conversations, dailyStatus, aiAnalysis, true);
    }

    private DailyStatusResponse buildDailyStatus(Long callId) {
        // 감정 상태
        CallEmotion emotion = emotionRepository.findLatestByCallId(callId).orElse(null);

        // 일일 상태 (식사, 건강, 수면)
        CallDailyStatus dailyStatus = dailyStatusRepository.findByCallRecordId(callId).orElse(null);

        return DailyStatusResponse.from(emotion, dailyStatus);
    }

    private AiAnalysisResponse buildAiAnalysis(Long callId, List<ElderlyResponse> elderlyResponses) {
        CallEmotion emotion = emotionRepository.findLatestByCallId(callId).orElse(null);
        return AiAnalysisResponse.from(emotion, elderlyResponses);
    }
}