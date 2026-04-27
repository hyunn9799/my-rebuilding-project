package com.aicc.silverlink.domain.elderly.service;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.*;
import com.aicc.silverlink.domain.elderly.entity.CallScheduleHistory;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.entity.ElderlyHealthInfo;
import com.aicc.silverlink.domain.elderly.repository.CallScheduleHistoryRepository;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.elderly.repository.HealthInfoRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.exception.BusinessException;
import com.aicc.silverlink.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * 통화 스케줄 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class CallScheduleService {

    private final ElderlyRepository elderlyRepository;
    private final HealthInfoRepository healthInfoRepository;
    private final CallScheduleHistoryRepository historyRepository;
    private final UserRepository userRepository;

    private static final DateTimeFormatter TIME_FORMATTER = DateTimeFormatter.ofPattern("HH:mm");

    // ===== 스케줄 조회/수정 =====

    /**
     * 어르신 통화 스케줄 조회
     */
    public Response getSchedule(Long elderlyId) {
        Elderly elderly = elderlyRepository.findWithUserById(elderlyId)
                .orElseThrow(() -> new IllegalArgumentException("어르신을 찾을 수 없습니다."));
        return Response.from(elderly);
    }

    /**
     * 어르신 통화 스케줄 설정/수정
     */
    @Transactional
    public Response updateSchedule(Long elderlyId, UpdateRequest request) {
        Elderly elderly = elderlyRepository.findWithUserById(elderlyId)
                .orElseThrow(() -> new IllegalArgumentException("어르신을 찾을 수 없습니다."));

        elderly.updateCallSchedule(
                request.getPreferredCallTime(),
                request.getDaysAsString(),
                request.getCallScheduleEnabled());

        elderlyRepository.save(elderly);
        log.info("[CallSchedule] 스케줄 업데이트: elderlyId={}, time={}, days={}, enabled={}",
                elderlyId, request.getPreferredCallTime(), request.getDaysAsString(), request.getCallScheduleEnabled());

        return Response.from(elderly);
    }

    // ===== 상담사/관리자 직접 수정 =====

    /**
     * 상담사/관리자가 직접 스케줄 수정 (구두 요청 등)
     */
    @Transactional
    public Response directUpdateSchedule(Long elderlyId, Long changerUserId, DirectUpdateRequest request) {
        Elderly elderly = elderlyRepository.findWithUserById(elderlyId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "어르신을 찾을 수 없습니다."));

        User changer = userRepository.findById(changerUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        // 이전 값 저장
        String previousTime = elderly.getPreferredCallTime();
        String previousDays = elderly.getPreferredCallDays();
        Boolean previousEnabled = elderly.getCallScheduleEnabled();

        // 스케줄 업데이트
        elderly.updateCallSchedule(
                request.getPreferredCallTime(),
                request.getDaysAsString(),
                request.getCallScheduleEnabled());

        elderlyRepository.save(elderly);

        // 변경 이력 저장
        CallScheduleHistory history = CallScheduleHistory.createDirectUpdate(
                elderly,
                changer,
                previousTime, previousDays, previousEnabled,
                request.getPreferredCallTime(), request.getDaysAsString(), request.getCallScheduleEnabled(),
                request.getChangeReason());

        historyRepository.save(history);

        log.info("[CallSchedule] 직접 수정: elderlyId={}, changedBy={}, reason={}",
                elderlyId, changer.getName(), request.getChangeReason());

        return Response.from(elderly);
    }

    // ===== 변경 이력 조회 =====

    /**
     * 특정 어르신의 변경 이력 조회
     */
    public List<HistoryResponse> getHistoryByElderly(Long elderlyId) {
        return historyRepository.findByElderlyIdOrderByCreatedAtDesc(elderlyId)
                .stream()
                .map(HistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 어르신의 변경 이력 조회 (페이징)
     */
    public Page<HistoryResponse> getHistoryByElderly(Long elderlyId, Pageable pageable) {
        return historyRepository.findByElderlyId(elderlyId, pageable)
                .map(HistoryResponse::from);
    }

    /**
     * 상담사 담당 어르신들의 변경 이력 조회
     */
    public List<HistoryResponse> getHistoryByCounselor(Long counselorId) {
        return historyRepository.findByCounselorAssigned(counselorId)
                .stream()
                .map(HistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 상담사 담당 어르신들의 변경 이력 조회 (페이징)
     */
    public Page<HistoryResponse> getHistoryByCounselor(Long counselorId, Pageable pageable) {
        return historyRepository.findByCounselorAssigned(counselorId, pageable)
                .map(HistoryResponse::from);
    }

    /**
     * 전체 변경 이력 조회 (관리자용)
     */
    public List<HistoryResponse> getAllHistory() {
        return historyRepository.findAllWithDetails()
                .stream()
                .map(HistoryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 전체 변경 이력 조회 (관리자용, 페이징)
     */
    public Page<HistoryResponse> getAllHistory(Pageable pageable) {
        return historyRepository.findAllWithDetails(pageable)
                .map(HistoryResponse::from);
    }

    // ===== CallBot용 =====

    /**
     * 현재 시간에 전화해야 할 어르신 목록
     * 
     * @return 전화 발신 요청 목록
     */
    public List<StartCallRequest> getDueForCall() {
        String currentTime = LocalTime.now().format(TIME_FORMATTER);
        String dayCode = LocalDate.now().getDayOfWeek().name().substring(0, 3); // MON, TUE, ...

        log.debug("[CallSchedule] 스케줄 체크: time={}, day={}", currentTime, dayCode);

        List<Elderly> dueList = elderlyRepository.findDueForCall(currentTime, dayCode);

        return dueList.stream()
                .map(this::toStartCallRequest)
                .collect(Collectors.toList());
    }

    /**
     * 전체 활성화된 스케줄 목록 조회 (관리자용)
     */
    public List<Response> getAllSchedules() {
        return elderlyRepository.findAllWithCallScheduleEnabled().stream()
                .map(Response::from)
                .collect(Collectors.toList());
    }

    // ===== Private Methods =====

    private StartCallRequest toStartCallRequest(Elderly elderly) {
        String originalPhone = elderly.getUser().getPhone();
        String formattedPhone = originalPhone;
        if (originalPhone != null && originalPhone.startsWith("0")) {
            formattedPhone = "+82" + originalPhone.substring(1); // 한국 국가 코드 +82로 수정 (요청하신 형식)
        }

        return StartCallRequest.builder()
                .elderlyId(elderly.getId())
                .elderlyName(elderly.getUser().getName())
                .phone(formattedPhone)
                .build();
    }
}
