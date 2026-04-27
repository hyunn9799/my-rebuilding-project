package com.aicc.silverlink.domain.emergency.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.assignment.entity.Assignment;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.call.entity.CallRecord;
import com.aicc.silverlink.domain.call.repository.CallRecordRepository;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.emergency.dto.EmergencyAlertDto.*;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.AlertStatus;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.Severity;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlertRecipient;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlertRecipient.ReceiverRole;
import com.aicc.silverlink.domain.emergency.repository.EmergencyAlertRecipientRepository;
import com.aicc.silverlink.domain.emergency.repository.EmergencyAlertRepository;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.emergency.event.EmergencyAlertCreatedEvent;
import com.aicc.silverlink.domain.notification.service.UnifiedSseService;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 긴급 알림 서비스 (SSE 통합 버전)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class EmergencyAlertService {

    private final EmergencyAlertRepository alertRepository;
    private final EmergencyAlertRecipientRepository recipientRepository;
    private final ElderlyRepository elderlyRepository;
    private final CallRecordRepository callRecordRepository;
    private final AssignmentRepository assignmentRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final SmsService smsService;
    private final UnifiedSseService unifiedSseService; // 통합 SSE 서비스로 변경
    private final ObjectMapper objectMapper;
    private final ApplicationEventPublisher eventPublisher;

    // ========== 긴급 알림 생성 (CallBot → Backend) ==========

    /**
     * 긴급 알림 생성 및 발송
     * CallBot에서 위험 감지 시 호출
     */
    @Transactional
    public EmergencyAlert createAlert(CreateRequest request) {
        log.info("[EmergencyAlertService] 긴급 알림 생성 시작. elderlyId={}, severity={}, type={}",
                request.getElderlyUserId(), request.getSeverity(), request.getAlertType());

        // 1. 어르신 조회
        Elderly elderly = elderlyRepository.findById(request.getElderlyUserId())
                .orElseThrow(() -> new IllegalArgumentException("어르신을 찾을 수 없습니다."));

        // 2. 통화 기록 조회 (있는 경우)
        CallRecord callRecord = null;
        if (request.getCallId() != null) {
            callRecord = callRecordRepository.findById(request.getCallId()).orElse(null);
        }

        // 3. 담당 상담사 조회
        Counselor assignedCounselor = null;
        Optional<Assignment> assignment = assignmentRepository.findActiveByElderlyId(elderly.getId());
        if (assignment.isPresent()) {
            assignedCounselor = assignment.get().getCounselor();
        }

        // 4. 위험 키워드 JSON 변환
        String dangerKeywordsJson = null;
        if (request.getDangerKeywords() != null && !request.getDangerKeywords().isEmpty()) {
            try {
                dangerKeywordsJson = objectMapper.writeValueAsString(request.getDangerKeywords());
            } catch (JsonProcessingException e) {
                log.warn("위험 키워드 JSON 변환 실패", e);
            }
        }

        // 5. 긴급 알림 생성
        EmergencyAlert alert = EmergencyAlert.builder()
                .elderly(elderly)
                .callRecord(callRecord)
                .severity(request.getSeverity())
                .alertType(request.getAlertType())
                .title(request.getTitle())
                .description(request.getDescription())
                .dangerKeywords(dangerKeywordsJson)
                .relatedSttContent(request.getRelatedSttContent())
                .assignedCounselor(assignedCounselor)
                .build();

        EmergencyAlert savedAlert = alertRepository.save(alert);
        log.info("[EmergencyAlertService] 긴급 알림 저장 완료. alertId={}", savedAlert.getId());

        // 6. 수신자 등록 및 SMS 알림 발송
        List<EmergencyAlertRecipient> recipients = createAndNotifyRecipients(savedAlert, elderly, assignedCounselor);

        // 7. SSE 실시간 알림 발송 (트랜잭션 커밋 후 실행되도록 이벤트 발행)
        List<Long> recipientUserIds = recipients.stream()
                .map(r -> r.getReceiver().getId())
                .collect(Collectors.toList());
        eventPublisher.publishEvent(new EmergencyAlertCreatedEvent(this, savedAlert, recipientUserIds));

        return savedAlert;
    }

    /**
     * 수신자 등록 및 SMS 알림 발송
     */
    private List<EmergencyAlertRecipient> createAndNotifyRecipients(
            EmergencyAlert alert,
            Elderly elderly,
            Counselor assignedCounselor) {

        List<EmergencyAlertRecipient> recipients = new ArrayList<>();

        // SMS 발송 대상 여부 (CRITICAL은 무조건 발송, WARNING은 설정에 따라)
        boolean sendSms = alert.isCritical();

        // 1. 담당 상담사 추가
        if (assignedCounselor != null) {
            EmergencyAlertRecipient counselorRecipient = EmergencyAlertRecipient.create(
                    alert,
                    assignedCounselor.getUser(),
                    ReceiverRole.COUNSELOR,
                    sendSms);
            recipients.add(counselorRecipient);
        }

        // 2. 보호자 추가
        Optional<GuardianElderly> guardianElderly = guardianElderlyRepository.findByElderlyId(elderly.getId());
        if (guardianElderly.isPresent()) {
            Guardian guardian = guardianElderly.get().getGuardian();
            EmergencyAlertRecipient guardianRecipient = EmergencyAlertRecipient.create(
                    alert,
                    guardian.getUser(),
                    ReceiverRole.GUARDIAN,
                    sendSms);
            recipients.add(guardianRecipient);
        }

        // 3. 관리자 추가 (어르신 관할 구역 담당 관리자)
        List<Admin> admins = findAdminsForElderly(elderly);
        for (Admin admin : admins) {
            EmergencyAlertRecipient adminRecipient = EmergencyAlertRecipient.create(
                    alert,
                    admin.getUser(),
                    ReceiverRole.ADMIN,
                    sendSms);
            recipients.add(adminRecipient);
        }

        // 4. 수신자 저장
        recipientRepository.saveAll(recipients);
        log.info("[EmergencyAlertService] 수신자 {} 명 등록 완료. alertId={}", recipients.size(), alert.getId());

        // 5. SMS 발송 (비동기)
        if (sendSms) {
            for (EmergencyAlertRecipient recipient : recipients) {
                if (recipient.isSmsRequired()) {
                    smsService.sendEmergencyAlertSmsAsync(alert, recipient);
                }
            }
        }

        return recipients;
    }

    /**
     * 어르신 관할 구역 담당 관리자 조회
     */
    private List<Admin> findAdminsForElderly(Elderly elderly) {
        Long admCode = elderly.getAdministrativeDivision().getAdmCode();
        return adminRepository.findByAdministrativeDivision_AdmCode(admCode);
    }

    // ========== 긴급 알림 조회 ==========

    /**
     * 긴급 알림 상세 조회
     */
    public DetailResponse getAlertDetail(Long alertId, Long userId) {
        EmergencyAlert alert = alertRepository.findByIdWithDetails(alertId)
                .orElseThrow(() -> new IllegalArgumentException("긴급 알림을 찾을 수 없습니다."));

        // 읽음 처리
        markAsRead(alertId, userId);

        return buildDetailResponse(alert);
    }

    /**
     * 상담사용 긴급 알림 목록 조회
     */
    public Page<SummaryResponse> getAlertsForCounselor(Long counselorId, Pageable pageable) {
        return alertRepository.findByCounselorId(counselorId, pageable)
                .map(this::buildSummaryResponseWithGuardian);
    }

    /**
     * 상담사용 미처리 긴급 알림 목록 (실시간 표시용)
     */
    public List<SummaryResponse> getPendingAlertsForCounselor(Long counselorId) {
        return alertRepository.findPendingByCounselorId(counselorId)
                .stream()
                .map(this::buildSummaryResponseWithGuardian)
                .collect(Collectors.toList());
    }

    /**
     * 관리자용 긴급 알림 목록 조회
     */
    public Page<SummaryResponse> getAlertsForAdmin(Pageable pageable) {
        return alertRepository.findAllWithDetails(pageable)
                .map(this::buildSummaryResponseWithGuardian);
    }

    /**
     * 보호자용 긴급 알림 목록 조회
     */
    public Page<SummaryResponse> getAlertsForGuardian(Long guardianId, Pageable pageable) {
        return alertRepository.findByGuardianId(guardianId, pageable)
                .map(SummaryResponse::from);
    }

    /**
     * 사용자별 미확인 알림 목록 (실시간 표시용)
     */
    public List<RecipientAlertResponse> getUnreadAlertsForUser(Long userId) {
        return recipientRepository.findUnreadByReceiverId(userId)
                .stream()
                .map(RecipientAlertResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 미확인 알림 수
     */
    public long getUnreadCount(Long userId) {
        return recipientRepository.countByReceiverIdAndIsReadFalse(userId);
    }

    // ========== 긴급 알림 처리 ==========

    /**
     * 알림 읽음 처리
     */
    @Transactional
    public void markAsRead(Long alertId, Long userId) {
        recipientRepository.findByEmergencyAlertIdAndReceiverId(alertId, userId)
                .ifPresent(recipient -> {
                    recipient.markAsRead();
                    recipientRepository.save(recipient);

                    // SSE로 미확인 알림 수 업데이트 전송
                    long unreadCount = recipientRepository.countByReceiverIdAndIsReadFalse(userId);
                    unifiedSseService.sendUnreadCountUpdate(userId, unreadCount, 0);
                });
    }

    /**
     * 모든 알림 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        recipientRepository.markAllAsReadByReceiverId(userId);

        // SSE로 미확인 알림 수 업데이트 전송
        unifiedSseService.sendUnreadCountUpdate(userId, 0, 0);
    }

    /**
     * 알림 처리 완료
     */
    @Transactional
    public DetailResponse processAlert(Long alertId, Long userId, ProcessRequest request) {
        EmergencyAlert alert = alertRepository.findById(alertId)
                .orElseThrow(() -> new IllegalArgumentException("긴급 알림을 찾을 수 없습니다."));

        User processor = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다."));

        switch (request.getStatus()) {
            case RESOLVED:
                alert.resolve(processor, request.getResolutionNote());
                break;
            case ESCALATED:
                alert.escalate(processor, request.getResolutionNote());
                break;
            case IN_PROGRESS:
                alert.startProcessing(processor);
                break;
            default:
                throw new IllegalArgumentException("잘못된 처리 상태입니다.");
        }

        alertRepository.save(alert);
        log.info("[EmergencyAlertService] 긴급 알림 처리 완료. alertId={}, status={}", alertId, request.getStatus());

        // SSE로 상태 변경 알림 전송 (모든 수신자에게)
        List<EmergencyAlertRecipient> recipients = recipientRepository.findByEmergencyAlertId(alertId);
        for (EmergencyAlertRecipient recipient : recipients) {
            unifiedSseService.sendEmergencyAlertStatusUpdate(
                    recipient.getReceiver().getId(),
                    alertId,
                    request.getStatus().name());
        }

        return buildDetailResponse(alert);
    }

    // ========== 통계 ==========

    /**
     * 긴급 알림 통계 조회
     */
    public StatsResponse getStats() {
        return StatsResponse.builder()
                .totalCount(alertRepository.count())
                .criticalCount(alertRepository.countBySeverity(Severity.CRITICAL))
                .warningCount(alertRepository.countBySeverity(Severity.WARNING))
                .pendingCount(alertRepository.countByStatus(AlertStatus.PENDING))
                .inProgressCount(alertRepository.countByStatus(AlertStatus.IN_PROGRESS))
                .resolvedCount(alertRepository.countByStatus(AlertStatus.RESOLVED))
                .build();
    }

    /**
     * 상담사별 통계 조회
     */
    public StatsResponse getStatsForCounselor(Long counselorId) {
        long pending = alertRepository.countPendingByCounselorId(counselorId);
        List<Object[]> statusCounts = alertRepository.countByStatusForCounselor(counselorId);
        List<Object[]> severityCounts = alertRepository.countBySeverityForCounselor(counselorId);

        long total = 0, inProgress = 0, resolved = 0;

        for (Object[] row : statusCounts) {
            AlertStatus status = (AlertStatus) row[0];
            long count = (Long) row[1];
            total += count;

            switch (status) {
                case IN_PROGRESS:
                    inProgress = count;
                    break;
                case RESOLVED:
                    resolved = count;
                    break;
                default:
                    break;
            }
        }

        long critical = 0, warning = 0;
        for (Object[] row : severityCounts) {
            Severity severity = (Severity) row[0];
            long count = (Long) row[1];

            switch (severity) {
                case CRITICAL:
                    critical = count;
                    break;
                case WARNING:
                    warning = count;
                    break;
                default:
                    break;
            }
        }

        return StatsResponse.builder()
                .totalCount(total)
                .pendingCount(pending)
                .inProgressCount(inProgress)
                .resolvedCount(resolved)
                .criticalCount(critical)
                .warningCount(warning)
                .build();
    }

    // ========== 내부 헬퍼 메서드 ==========

    private SummaryResponse buildSummaryResponseWithGuardian(EmergencyAlert alert) {
        Optional<GuardianElderly> guardianElderly = guardianElderlyRepository
                .findByElderlyId(alert.getElderly().getId());

        if (guardianElderly.isPresent()) {
            Guardian guardian = guardianElderly.get().getGuardian();
            return SummaryResponse.fromWithGuardian(
                    alert,
                    guardian.getUser().getName(),
                    guardian.getUser().getPhone());
        }

        return SummaryResponse.from(alert);
    }

    private DetailResponse buildDetailResponse(EmergencyAlert alert) {
        Elderly elderly = alert.getElderly();
        User elderlyUser = elderly.getUser();

        // 보호자 정보
        DetailResponse.GuardianInfo guardianInfo = null;
        Optional<GuardianElderly> guardianElderly = guardianElderlyRepository.findByElderlyId(elderly.getId());
        if (guardianElderly.isPresent()) {
            Guardian guardian = guardianElderly.get().getGuardian();
            guardianInfo = DetailResponse.GuardianInfo.builder()
                    .id(guardian.getId())
                    .name(guardian.getUser().getName())
                    .phone(guardian.getUser().getPhone())
                    .relation(guardianElderly.get().getRelationType().name())
                    .build();
        }

        // 상담사 정보
        DetailResponse.CounselorInfo counselorInfo = null;
        if (alert.getAssignedCounselor() != null) {
            Counselor counselor = alert.getAssignedCounselor();
            counselorInfo = DetailResponse.CounselorInfo.builder()
                    .id(counselor.getId())
                    .name(counselor.getUser().getName())
                    .phone(counselor.getUser().getPhone())
                    .department(counselor.getDepartment())
                    .build();
        }

        // 통화 정보
        DetailResponse.CallInfo callInfo = null;
        if (alert.getCallRecord() != null) {
            CallRecord call = alert.getCallRecord();
            callInfo = DetailResponse.CallInfo.builder()
                    .callId(call.getId())
                    .callAt(call.getCallAt())
                    .duration(call.getFormattedDuration())
                    .state(call.getState().getKorean())
                    .emotionLevel(
                            call.getLatestEmotion() != null ? call.getLatestEmotion().getEmotionLevelKorean() : null)
                    .recordingUrl(call.getRecordingUrl())
                    .build();
        }

        // 처리 정보
        DetailResponse.ProcessInfo processInfo = null;
        if (alert.getProcessedBy() != null) {
            processInfo = DetailResponse.ProcessInfo.builder()
                    .processedByName(alert.getProcessedBy().getName())
                    .processedAt(alert.getProcessedAt())
                    .resolutionNote(alert.getResolutionNote())
                    .build();
        }

        // 위험 키워드 파싱
        List<String> dangerKeywords = null;
        if (alert.getDangerKeywords() != null) {
            try {
                dangerKeywords = objectMapper.readValue(
                        alert.getDangerKeywords(),
                        objectMapper.getTypeFactory().constructCollectionType(List.class, String.class));
            } catch (JsonProcessingException e) {
                log.warn("위험 키워드 파싱 실패", e);
            }
        }

        return DetailResponse.builder()
                .alertId(alert.getId())
                .severity(alert.getSeverity())
                .severityText(alert.getSeverity().getDescription())
                .alertType(alert.getAlertType())
                .alertTypeText(alert.getAlertType().getDescription())
                .title(alert.getTitle())
                .description(alert.getDescription())
                .dangerKeywords(dangerKeywords)
                .relatedSttContent(alert.getRelatedSttContent())
                .status(alert.getStatus())
                .statusText(alert.getStatus().getDescription())
                .elderly(DetailResponse.ElderlyInfo.builder()
                        .id(elderly.getId())
                        .name(elderlyUser.getName())
                        .age(elderly.age())
                        .gender(elderly.getGender().name())
                        .phone(elderlyUser.getPhone())
                        .address(elderly.getAddressLine1() != null ? elderly.getAddressLine1() : "")
                        .build())
                .guardian(guardianInfo)
                .counselor(counselorInfo)
                .call(callInfo)
                .process(processInfo)
                .createdAt(alert.getCreatedAt())
                .build();
    }

}
