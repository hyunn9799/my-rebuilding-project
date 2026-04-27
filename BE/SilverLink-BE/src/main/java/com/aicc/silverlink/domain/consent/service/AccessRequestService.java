package com.aicc.silverlink.domain.consent.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.consent.dto.AccessRequestDto;
import com.aicc.silverlink.domain.consent.dto.AccessRequestDto.*;
import com.aicc.silverlink.domain.consent.entity.AccessRequest;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessRequestStatus;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope;
import com.aicc.silverlink.domain.consent.repository.AccessRequestRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.notification.service.NotificationService;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 접근 권한 요청 서비스
 *
 * 보호자가 어르신의 민감정보(건강정보, 복약정보, 통화기록)를
 * 열람하기 위한 권한 요청/승인/거절/철회 프로세스를 처리합니다.
 *
 * 프로세스:
 * 1. 보호자가 접근 권한 요청 (createAccessRequest)
 * 2. 관리자가 서류(동의서, 가족관계증명서) 확인 (verifyDocuments)
 * 3. 관리자가 승인 또는 거절 (approveRequest / rejectRequest)
 * 4. 승인된 경우 보호자는 해당 범위의 민감정보 열람 가능
 * 5. 필요시 어르신 또는 관리자가 권한 철회 가능 (revokeAccess)
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AccessRequestService {

    private final AccessRequestRepository accessRequestRepository;
    private final UserRepository userRepository;
    private final ElderlyRepository elderlyRepository;
    private final AdminRepository adminRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final com.aicc.silverlink.domain.assignment.repository.AssignmentRepository assignmentRepository;
    private final NotificationService notificationService;

    // ========== 보호자용 메서드 ==========

    /**
     * 접근 권한 요청 생성
     * 보호자가 어르신의 민감정보 열람을 요청합니다.
     *
     * @param guardianUserId 보호자 사용자 ID
     * @param request        요청 DTO
     * @return 생성된 요청 정보
     */
    @Transactional
    public AccessRequestResponse createAccessRequest(Long guardianUserId, CreateRequest request) {
        log.info("접근 권한 요청 생성 - guardianUserId: {}, elderlyUserId: {}, scope: {}",
                guardianUserId, request.elderlyUserId(), request.scope());

        // 1. 요청자(보호자) 확인
        User guardian = userRepository.findById(guardianUserId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (guardian.getRole() != Role.GUARDIAN && guardian.getRole() != Role.COUNSELOR) {
            throw new IllegalArgumentException("ONLY_GUARDIAN_OR_COUNSELOR_CAN_REQUEST");
        }

        // 2. 대상 어르신 확인
        Elderly elderly = elderlyRepository.findById(request.elderlyUserId())
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        // 3. 관계 확인
        if (guardian.getRole() == Role.GUARDIAN) {
            GuardianElderly relation = guardianElderlyRepository.findByGuardianId(guardianUserId)
                    .orElseThrow(() -> new IllegalArgumentException("GUARDIAN_ELDERLY_RELATION_NOT_FOUND"));

            if (!relation.getElderly().getId().equals(request.elderlyUserId())) {
                throw new IllegalArgumentException("NOT_YOUR_ELDERLY");
            }
        } else if (guardian.getRole() == Role.COUNSELOR) {
            boolean isAssigned = assignmentRepository.existsByCounselor_IdAndElderly_IdAndStatus(
                    guardianUserId, request.elderlyUserId(),
                    com.aicc.silverlink.domain.assignment.entity.AssignmentStatus.ACTIVE);

            if (!isAssigned) {
                throw new IllegalArgumentException("NOT_YOUR_ASSIGNED_ELDERLY");
            }
        }

        // 4. 중복 요청 확인 (이미 대기 중이거나 승인된 요청이 있는지)
        accessRequestRepository.findActiveRequest(guardianUserId, request.elderlyUserId(), request.scope())
                .ifPresent(ar -> {
                    if (ar.getStatus() == AccessRequestStatus.PENDING) {
                        throw new IllegalStateException("REQUEST_ALREADY_PENDING");
                    }
                    if (ar.isAccessGranted()) {
                        throw new IllegalStateException("ACCESS_ALREADY_GRANTED");
                    }
                });

        // 5. 요청 생성
        AccessRequest accessRequest = AccessRequest.create(guardian, elderly, request.scope());
        AccessRequest saved = accessRequestRepository.save(accessRequest);

        // 6. 관리자에게 새 요청 알림 발송
        List<Admin> admins = adminRepository
                .findByAdministrativeDivision_AdmCode(elderly.getAdministrativeDivision().getAdmCode());
        for (Admin admin : admins) {
            notificationService.createAccessRequestNotification(
                    admin.getUser().getId(),
                    saved.getId(),
                    guardian.getName(),
                    elderly.getUser().getName());
        }

        log.info("접근 권한 요청 생성 완료 - requestId: {}", saved.getId());
        return AccessRequestResponse.from(saved);
    }

    /**
     * 보호자가 자신의 요청 취소
     */
    @Transactional
    public void cancelRequest(Long guardianUserId, Long requestId) {
        log.info("접근 권한 요청 취소 - guardianUserId: {}, requestId: {}", guardianUserId, requestId);

        AccessRequest request = accessRequestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("REQUEST_NOT_FOUND"));

        // 본인 요청인지 확인
        if (!request.getRequester().getId().equals(guardianUserId)) {
            throw new IllegalArgumentException("NOT_YOUR_REQUEST");
        }

        request.cancel();
        log.info("접근 권한 요청 취소 완료 - requestId: {}", requestId);
    }

    /**
     * 보호자의 요청 목록 조회
     */
    public List<AccessRequestSummary> getMyRequests(Long guardianUserId) {
        return accessRequestRepository.findByRequesterId(guardianUserId).stream()
                .map(AccessRequestSummary::from)
                .collect(Collectors.toList());
    }

    // ========== 관리자용 메서드 ==========

    /**
     * 대기 중인 요청 목록 조회
     */
    public List<AccessRequestSummary> getPendingRequests() {
        return accessRequestRepository.findPendingRequests().stream()
                .map(AccessRequestSummary::from)
                .collect(Collectors.toList());
    }

    /**
     * 서류 확인 완료된 대기 요청 목록 조회
     */
    public List<AccessRequestSummary> getVerifiedPendingRequests() {
        return accessRequestRepository.findVerifiedPendingRequests().stream()
                .map(AccessRequestSummary::from)
                .collect(Collectors.toList());
    }

    /**
     * 서류 확인 완료 처리
     * 관리자가 동의서와 가족관계증명서를 확인했음을 표시합니다.
     *
     * @param adminUserId 관리자 사용자 ID
     * @param request     요청 DTO
     */
    @Transactional
    public AccessRequestResponse verifyDocuments(Long adminUserId, VerifyDocumentsRequest request) {
        log.info("서류 확인 완료 처리 - adminUserId: {}, requestId: {}", adminUserId, request.accessRequestId());

        // 관리자 권한 확인
        validateAdmin(adminUserId);

        AccessRequest accessRequest = accessRequestRepository.findByIdWithDetails(request.accessRequestId())
                .orElseThrow(() -> new IllegalArgumentException("REQUEST_NOT_FOUND"));

        if (accessRequest.getStatus() != AccessRequestStatus.PENDING) {
            throw new IllegalStateException("REQUEST_NOT_PENDING");
        }

        accessRequest.verifyDocuments();

        log.info("서류 확인 완료 - requestId: {}", request.accessRequestId());
        return AccessRequestResponse.from(accessRequest);
    }

    /**
     * 접근 권한 요청 승인
     * 서류 확인이 완료된 요청만 승인 가능합니다.
     *
     * @param adminUserId 관리자 사용자 ID
     * @param request     승인 요청 DTO
     */
    @Transactional
    public AccessRequestResponse approveRequest(Long adminUserId, ApproveRequest request) {
        log.info("접근 권한 요청 승인 - adminUserId: {}, requestId: {}", adminUserId, request.accessRequestId());

        Admin admin = getAdmin(adminUserId);

        AccessRequest accessRequest = accessRequestRepository.findByIdWithDetails(request.accessRequestId())
                .orElseThrow(() -> new IllegalArgumentException("REQUEST_NOT_FOUND"));

        // 만료일 기본값 설정 (null이면 1년 후)
        LocalDateTime expiresAt = request.expiresAt();
        if (expiresAt == null) {
            expiresAt = LocalDateTime.now().plusYears(1);
        }

        accessRequest.approve(admin, expiresAt, request.note());

        // 요청자에게 승인 알림 발송
        notificationService.createAccessApprovedNotification(
                accessRequest.getRequester().getId(),
                request.accessRequestId(),
                accessRequest.getElderly().getUser().getName());

        log.info("접근 권한 요청 승인 완료 - requestId: {}, expiresAt: {}", request.accessRequestId(), expiresAt);
        return AccessRequestResponse.from(accessRequest);
    }

    /**
     * 접근 권한 요청 거절
     *
     * @param adminUserId 관리자 사용자 ID
     * @param request     거절 요청 DTO
     */
    @Transactional
    public AccessRequestResponse rejectRequest(Long adminUserId, RejectRequest request) {
        log.info("접근 권한 요청 거절 - adminUserId: {}, requestId: {}", adminUserId, request.accessRequestId());

        Admin admin = getAdmin(adminUserId);

        AccessRequest accessRequest = accessRequestRepository.findByIdWithDetails(request.accessRequestId())
                .orElseThrow(() -> new IllegalArgumentException("REQUEST_NOT_FOUND"));

        accessRequest.reject(admin, request.reason());

        // 요청자에게 거절 알림 발송
        notificationService.createAccessRejectedNotification(
                accessRequest.getRequester().getId(),
                request.accessRequestId(),
                accessRequest.getElderly().getUser().getName(),
                request.reason());

        log.info("접근 권한 요청 거절 완료 - requestId: {}, reason: {}", request.accessRequestId(), request.reason());
        return AccessRequestResponse.from(accessRequest);
    }

    /**
     * 접근 권한 철회 (관리자용)
     */
    @Transactional
    public AccessRequestResponse revokeAccess(Long adminUserId, RevokeRequest request) {
        log.info("접근 권한 철회 (관리자) - adminUserId: {}, requestId: {}", adminUserId, request.accessRequestId());

        validateAdmin(adminUserId);

        AccessRequest accessRequest = accessRequestRepository.findByIdWithDetails(request.accessRequestId())
                .orElseThrow(() -> new IllegalArgumentException("REQUEST_NOT_FOUND"));

        accessRequest.revoke(request.reason());

        log.info("접근 권한 철회 완료 - requestId: {}", request.accessRequestId());
        return AccessRequestResponse.from(accessRequest);
    }

    /**
     * 요청 상세 조회
     */
    public AccessRequestResponse getRequestDetail(Long requestId) {
        AccessRequest request = accessRequestRepository.findByIdWithDetails(requestId)
                .orElseThrow(() -> new IllegalArgumentException("REQUEST_NOT_FOUND"));
        return AccessRequestResponse.from(request);
    }

    /**
     * 대기 중인 요청 통계
     */
    public PendingRequestStats getPendingStats() {
        List<AccessRequest> pendingRequests = accessRequestRepository.findPendingRequests();

        long total = pendingRequests.size();
        long verified = pendingRequests.stream().filter(AccessRequest::isDocumentVerified).count();
        long notVerified = total - verified;

        return new PendingRequestStats(total, verified, notVerified);
    }

    // ========== 어르신용 메서드 ==========

    /**
     * 어르신이 자신에 대한 접근 요청 목록 조회
     */
    public List<AccessRequestSummary> getRequestsForElderly(Long elderlyUserId) {
        return accessRequestRepository.findByElderlyId(elderlyUserId).stream()
                .map(AccessRequestSummary::from)
                .collect(Collectors.toList());
    }

    /**
     * 어르신이 승인된 권한 철회
     */
    @Transactional
    public AccessRequestResponse revokeAccessByElderly(Long elderlyUserId, RevokeRequest request) {
        log.info("접근 권한 철회 (어르신) - elderlyUserId: {}, requestId: {}", elderlyUserId, request.accessRequestId());

        AccessRequest accessRequest = accessRequestRepository.findByIdWithDetails(request.accessRequestId())
                .orElseThrow(() -> new IllegalArgumentException("REQUEST_NOT_FOUND"));

        // 본인에 대한 요청인지 확인
        if (!accessRequest.getElderly().getId().equals(elderlyUserId)) {
            throw new IllegalArgumentException("NOT_YOUR_ACCESS_REQUEST");
        }

        accessRequest.revoke(request.reason() != null ? request.reason() : "어르신 본인에 의한 철회");

        log.info("접근 권한 철회 완료 (어르신) - requestId: {}", request.accessRequestId());
        return AccessRequestResponse.from(accessRequest);
    }

    // ========== 권한 확인 메서드 (다른 서비스에서 호출) ==========

    /**
     * 특정 사용자가 특정 어르신의 특정 범위 민감정보에 접근 가능한지 확인
     *
     * @param requesterId   접근 시도자 ID
     * @param elderlyUserId 어르신 ID
     * @param scope         접근 범위
     * @return 접근 가능 여부
     */
    public boolean hasAccess(Long requesterId, Long elderlyUserId, AccessScope scope) {
        return accessRequestRepository.hasValidAccess(
                requesterId,
                elderlyUserId,
                scope,
                LocalDateTime.now());
    }

    /**
     * 접근 권한 확인 (상세 결과 반환)
     */
    public AccessCheckResult checkAccess(Long requesterId, Long elderlyUserId, AccessScope scope) {
        return accessRequestRepository.findValidAccess(
                        requesterId,
                        elderlyUserId,
                        scope,
                        LocalDateTime.now())
                .map(AccessCheckResult::granted)
                .orElse(AccessCheckResult.denied("접근 권한이 없습니다. 관리자에게 권한을 요청하세요."));
    }

    // ========== 배치 처리 ==========

    /**
     * 만료된 권한 자동 처리 (매일 자정 실행)
     */
    @Scheduled(cron = "0 0 0 * * *")
    @Transactional
    public void processExpiredAccess() {
        log.info("만료된 접근 권한 처리 시작");
        int count = accessRequestRepository.expireOldApprovals(LocalDateTime.now());
        log.info("만료된 접근 권한 처리 완료 - {}건", count);
    }

    // ========== 헬퍼 메서드 ==========

    private void validateAdmin(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (user.getRole() != Role.ADMIN) {
            throw new IllegalArgumentException("ADMIN_ONLY");
        }
    }

    private Admin getAdmin(Long userId) {
        validateAdmin(userId);
        return adminRepository.findByIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("ADMIN_NOT_FOUND"));
    }

}
