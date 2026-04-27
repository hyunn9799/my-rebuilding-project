package com.aicc.silverlink.domain.elderly.service;

import com.aicc.silverlink.domain.elderly.dto.ScheduleChangeRequestDto.*;
import com.aicc.silverlink.domain.elderly.entity.CallScheduleHistory;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest;
import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest.RequestStatus;
import com.aicc.silverlink.domain.elderly.repository.CallScheduleHistoryRepository;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.elderly.repository.ScheduleChangeRequestRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.exception.BusinessException;
import com.aicc.silverlink.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class ScheduleChangeRequestService {

    private final ScheduleChangeRequestRepository changeRequestRepository;
    private final ElderlyRepository elderlyRepository;
    private final UserRepository userRepository;
    private final CallScheduleHistoryRepository historyRepository;

    /**
     * 변경 요청 생성 (어르신용)
     */
    @Transactional
    public Response createRequest(Long elderlyUserId, CreateRequest request) {
        // 이미 대기 중인 요청이 있는지 확인
        if (changeRequestRepository.existsByElderlyIdAndStatus(elderlyUserId, RequestStatus.PENDING)) {
            throw new BusinessException(ErrorCode.DUPLICATE_RESOURCE, "이미 대기 중인 변경 요청이 있습니다.");
        }

        Elderly elderly = elderlyRepository.findById(elderlyUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "어르신 정보를 찾을 수 없습니다."));

        ScheduleChangeRequest changeRequest = ScheduleChangeRequest.create(
                elderly,
                request.getPreferredCallTime(),
                request.getDaysAsString());

        changeRequestRepository.save(changeRequest);
        return Response.from(changeRequest);
    }

    /**
     * 본인의 변경 요청 목록 조회 (어르신용)
     */
    public List<Response> getMyRequests(Long elderlyUserId) {
        return changeRequestRepository.findByElderlyIdOrderByCreatedAtDesc(elderlyUserId)
                .stream()
                .map(Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 대기 중인 변경 요청 목록 (상담사용)
     */
    public List<Response> getPendingRequests() {
        return changeRequestRepository.findPendingRequestsWithElderly(RequestStatus.PENDING)
                .stream()
                .map(Response::from)
                .collect(Collectors.toList());
    }

    /**
     * 변경 요청 승인 (상담사용)
     */
    @Transactional
    public Response approveRequest(Long requestId, Long counselorUserId) {
        ScheduleChangeRequest request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "변경 요청을 찾을 수 없습니다."));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 처리된 요청입니다.");
        }

        User counselor = userRepository.findById(counselorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "상담사 정보를 찾을 수 없습니다."));

        // 어르신 이전 스케줄 정보 저장
        Elderly elderly = request.getElderly();
        String previousTime = elderly.getPreferredCallTime();
        String previousDays = elderly.getPreferredCallDays();
        Boolean previousEnabled = elderly.getCallScheduleEnabled();

        // 요청 승인
        request.approve(counselor);

        // 어르신 스케줄 업데이트
        elderly.updateCallSchedule(
                request.getRequestedCallTime(),
                request.getRequestedCallDays(),
                true);

        // 변경 이력 저장
        CallScheduleHistory history = CallScheduleHistory.createRequestApproved(
                elderly,
                counselor,
                requestId,
                previousTime, previousDays, previousEnabled,
                request.getRequestedCallTime(), request.getRequestedCallDays());

        historyRepository.save(history);

        log.info("[ScheduleChangeRequest] 요청 승인: requestId={}, elderlyId={}, counselor={}",
                requestId, elderly.getId(), counselor.getName());

        return Response.from(request);
    }

    /**
     * 변경 요청 거절 (상담사용)
     */
    @Transactional
    public Response rejectRequest(Long requestId, Long counselorUserId, RejectRequest rejectRequest) {
        ScheduleChangeRequest request = changeRequestRepository.findById(requestId)
                .orElseThrow(() -> new BusinessException(ErrorCode.ENTITY_NOT_FOUND, "변경 요청을 찾을 수 없습니다."));

        if (request.getStatus() != RequestStatus.PENDING) {
            throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "이미 처리된 요청입니다.");
        }

        User counselor = userRepository.findById(counselorUserId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "상담사 정보를 찾을 수 없습니다."));

        Elderly elderly = request.getElderly();

        // 요청 거절
        request.reject(counselor, rejectRequest.getReason());

        // 변경 이력 저장 (거절 기록)
        CallScheduleHistory history = CallScheduleHistory.createRequestRejected(
                elderly,
                counselor,
                requestId,
                rejectRequest.getReason());

        historyRepository.save(history);

        log.info("[ScheduleChangeRequest] 요청 거절: requestId={}, elderlyId={}, counselor={}, reason={}",
                requestId, elderly.getId(), counselor.getName(), rejectRequest.getReason());

        return Response.from(request);
    }
}
