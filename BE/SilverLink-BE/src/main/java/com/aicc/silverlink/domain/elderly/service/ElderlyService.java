package com.aicc.silverlink.domain.elderly.service;

import com.aicc.silverlink.domain.assignment.entity.Assignment;
import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope;
import com.aicc.silverlink.domain.consent.repository.AccessRequestRepository;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.service.CounselorService;
import com.aicc.silverlink.domain.elderly.dto.request.ElderlyCreateRequest;
import com.aicc.silverlink.domain.elderly.dto.request.ElderlyUpdateRequest;
import com.aicc.silverlink.domain.elderly.dto.request.HealthInfoUpdateRequest;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlyAdminDetailResponse;
import com.aicc.silverlink.domain.elderly.dto.response.ElderlySummaryResponse;
import com.aicc.silverlink.domain.elderly.dto.response.HealthInfoResponse;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.entity.ElderlyHealthInfo;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.elderly.repository.HealthInfoRepository;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.entity.UserStatus;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 어르신 서비스
 *
 * 민감정보(건강정보) 접근 권한 체크 로직이 포함되어 있습니다.
 *
 * 접근 권한 규칙:
 * - 어르신 본인: 항상 접근 가능
 * - 관리자(ADMIN): 항상 접근 가능
 * - 상담사(COUNSELOR): 담당 어르신에 대해 접근 가능
 * - 보호자(GUARDIAN): 동의서 + 가족관계증명서 제출 후 관리자 승인을 받아야 접근 가능
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class ElderlyService {

    private final ElderlyRepository elderlyRepo;
    private final HealthInfoRepository healthRepo;
    private final UserRepository userRepo;
    private final AdministrativeDivisionRepository divisionRepository;
    private final AccessRequestRepository accessRequestRepo;
    private final GuardianElderlyRepository guardianElderlyRepo;
    private final AssignmentRepository assignmentRepo;
    private final CounselorService counselorService;

    @Transactional
    public ElderlySummaryResponse createElderly(ElderlyCreateRequest req) {
        User user = userRepo.findById(req.userId())
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (user.getStatus() == UserStatus.DELETED)
            throw new IllegalStateException("USER_DELETED");
        if (user.getRole() != Role.ELDERLY)
            throw new IllegalStateException("ROLE_NOT_ELDERLY");

        if (elderlyRepo.existsById(user.getId())) {
            throw new IllegalStateException("ELDERLY_ALREADY_EXISTS");
        }

        AdministrativeDivision division = divisionRepository.findById(req.admCode())
                .orElseThrow(() -> new IllegalArgumentException(
                        "존재하지 않는 행정구역 코드입니다: " + req.admCode()));

        Elderly elderly = Elderly.create(user, division, req.birthDate(), req.gender());
        elderly.updateAddress(req.addressLine1(), req.addressLine2(), req.zipcode());

        // 통화 스케줄 설정
        elderly.updateCallSchedule(
                req.preferredCallTime(),
                req.getPreferredCallDaysAsString(),
                req.callScheduleEnabled() != null ? req.callScheduleEnabled() : false);

        Elderly saved = elderlyRepo.save(elderly);

        log.info("어르신 등록 완료 - userId: {}, admCode: {}", user.getId(), req.admCode());

        return ElderlySummaryResponse.from(saved);
    }

    @Transactional(readOnly = true)
    public List<ElderlySummaryResponse> getAllElderlyForAdmin() {
        // 1. 전체 어르신 조회
        List<Elderly> elderlyList = elderlyRepo.findAllWithUserAndDivision();

        // 2. 보호자 매핑 (어르신 ID -> 보호자 이름)
        Map<Long, String> guardianMap = guardianElderlyRepo.findAllWithDetails().stream()
                .collect(Collectors.toMap(
                        ge -> ge.getElderly().getId(),
                        ge -> ge.getGuardian().getUser().getName(),
                        (existing, replacement) -> existing));

        // 3. 상담사 매핑 (어르신 ID -> 상담사 이름)
        Map<Long, String> counselorMap = assignmentRepo.findAllActiveWithDetails().stream()
                .collect(Collectors.toMap(
                        a -> a.getElderly().getId(),
                        a -> a.getCounselor().getUser().getName(),
                        (existing, replacement) -> existing));

        // 4. 조합
        return elderlyList.stream()
                .map(e -> ElderlySummaryResponse.from(
                        e,
                        guardianMap.get(e.getId()),
                        counselorMap.get(e.getId())))
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ElderlyAdminDetailResponse getElderlyDetailForAdmin(Long elderlyUserId) {
        Elderly elderly = elderlyRepo.findWithUserById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        // 1. 연결된 보호자 정보
        GuardianResponse guardian = guardianElderlyRepo.findByElderlyId(elderlyUserId)
                .map(ge -> GuardianResponse.from(ge.getGuardian()))
                .orElse(null);

        // 2. 배정된 상담사 정보
        CounselorResponse counselor = assignmentRepo.findActiveByElderlyId(elderlyUserId)
                .map(assignment -> counselorService.getCounselor(assignment.getCounselor().getId()))
                .orElse(null);

        return ElderlyAdminDetailResponse.builder()
                .elderly(ElderlySummaryResponse.from(elderly))
                .guardian(guardian)
                .counselor(counselor)
                .build();
    }

    @Transactional(readOnly = true)
    public List<ElderlySummaryResponse> searchElderlyByName(String name) {
        List<Elderly> elderlyList = elderlyRepo.findAllByUser_NameContaining(name);
        return elderlyList.stream()
                .map(ElderlySummaryResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public ElderlySummaryResponse getSummary(Long elderlyUserId) {
        Elderly elderly = elderlyRepo.findWithUserById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        String guardianName = guardianElderlyRepo.findByElderlyId(elderlyUserId)
                .map(ge -> ge.getGuardian().getUser().getName())
                .orElse(null);

        return ElderlySummaryResponse.from(elderly, guardianName);
    }

    @Transactional
    public void updateAddress(Long elderlyUserId, String line1, String line2, String zipcode) {
        Elderly elderly = elderlyRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));
        elderly.updateAddress(line1, line2, zipcode);
    }

    /**
     * 어르신 행정구역 변경
     */
    @Transactional
    public void changeAdministrativeDivision(Long elderlyUserId, Long admCode) {
        Elderly elderly = elderlyRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        AdministrativeDivision division = divisionRepository.findById(admCode)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정구역 코드입니다: " + admCode));

        elderly.changeAdministrativeDivision(division);
        log.info("어르신 행정구역 변경 완료 - userId: {}, newAdmCode: {}", elderlyUserId, admCode);
    }

    /**
     * 건강정보 조회 (민감정보 - 권한 체크 필수)
     */
    @Transactional(readOnly = true)
    public HealthInfoResponse getHealthInfo(Long requesterUserId, Long elderlyUserId) {
        // 민감정보 접근 권한 확인
        assertCanReadHealthInfo(requesterUserId, elderlyUserId);

        ElderlyHealthInfo hi = healthRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("HEALTH_INFO_NOT_FOUND"));

        return HealthInfoResponse.from(hi);
    }

    /**
     * 건강정보 등록/수정 (민감정보 - 권한 체크 필수)
     */
    @Transactional
    public HealthInfoResponse upsertHealthInfo(Long requesterUserId, Long elderlyUserId,
                                               HealthInfoUpdateRequest req) {
        // 민감정보 쓰기 권한 확인
        assertCanWriteHealthInfo(requesterUserId, elderlyUserId);

        Elderly elderly = elderlyRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        ElderlyHealthInfo hi = healthRepo.findById(elderlyUserId)
                .orElseGet(() -> ElderlyHealthInfo.create(elderly));

        hi.update(req.chronicDiseases(), req.mentalHealthNotes(), req.specialNotes());
        return HealthInfoResponse.from(healthRepo.save(hi));
    }

    @Transactional
    public ElderlySummaryResponse updateElderlyProfile(Long elderlyUserId, ElderlyUpdateRequest req) {
        Elderly elderly = elderlyRepo.findWithUserById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        elderly.getUser().updateProfile(req.name(), req.phone(), null);
        elderly.updateAddress(req.addressLine1(), req.addressLine2(), req.zipcode());

        // 행정구역 변경
        if (req.admCode() != null && !req.admCode().equals(elderly.getAdministrativeDivision().getAdmCode())) {
            AdministrativeDivision newDivision = divisionRepository.findById(req.admCode())
                    .orElseThrow(() -> new IllegalArgumentException(
                            "존재하지 않는 행정구역 코드입니다: " + req.admCode()));
            elderly.changeAdministrativeDivision(newDivision);
        }

        // 통화 스케줄 수정
        if (req.preferredCallTime() != null || req.preferredCallDays() != null
                || req.callScheduleEnabled() != null) {
            elderly.updateCallSchedule(
                    req.preferredCallTime() != null ? req.preferredCallTime()
                            : elderly.getPreferredCallTime(),
                    req.getPreferredCallDaysAsString() != null ? req.getPreferredCallDaysAsString()
                            : elderly.getPreferredCallDays(),
                    req.callScheduleEnabled() != null ? req.callScheduleEnabled()
                            : elderly.getCallScheduleEnabled());
        }

        return ElderlySummaryResponse.from(elderly);
    }

    @Transactional
    public void withdrawElderly(Long elderlyUserId) {
        Elderly elderly = elderlyRepo.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        // 1. 활성화된 상담사 배정 종료
        assignmentRepo.findActiveByElderlyId(elderlyUserId)
                .ifPresent(Assignment::endAssignment);

        // 2. 보호자 연결 해제 (Mapping 테이블 즉시 삭제)
        guardianElderlyRepo.deleteByElderlyId(elderlyUserId);

        // 3. 유저 소프트 딜리트 (법적 5년 보관)
        elderly.getUser().softDelete();

        log.info("어르신 탈퇴 처리 및 배정 종료 완료 - elderlyId: {}", elderlyUserId);
    }

    // ========== 권한 검증 로직 ==========

    private void assertCanReadHealthInfo(Long requesterUserId, Long elderlyUserId) {
        if (requesterUserId.equals(elderlyUserId))
            return; // 본인 허용

        User requester = userRepo.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (requester.getRole() == Role.ADMIN)
            return; // 관리자 허용

        if (requester.getRole() == Role.COUNSELOR) {
            validateCounselorAccess(requesterUserId, elderlyUserId);
            return;
        }

        if (requester.getRole() == Role.GUARDIAN) {
            validateGuardianAccess(requesterUserId, elderlyUserId, AccessScope.HEALTH_INFO);
            return;
        }

        throw new AccessDeniedException("권한이 없습니다.");
    }

    /**
     * 민감정보 쓰기 권한 확인
     */
    private void assertCanWriteHealthInfo(Long requesterUserId, Long elderlyUserId) {
        User requester = userRepo.findById(requesterUserId)
                .orElseThrow(() -> new IllegalArgumentException("USER_NOT_FOUND"));

        if (requester.getRole() == Role.ADMIN)
            return;
        if (requester.getRole() == Role.COUNSELOR) {
            validateCounselorAccess(requesterUserId, elderlyUserId);
            return;
        }

        throw new AccessDeniedException("수정 권한이 없습니다.");
    }

    /**
     * 상담사의 담당 어르신 접근 권한 확인
     */
    private void validateCounselorAccess(Long counselorUserId, Long elderlyUserId) {
        // ✅ [실구현] 배정 관계가 ACTIVE인 경우만 허용
        boolean isAssigned = assignmentRepo.existsByCounselor_IdAndElderly_IdAndStatus(
                counselorUserId, elderlyUserId, AssignmentStatus.ACTIVE);

        if (!isAssigned) {
            log.warn("상담사 부정 접근 차단 - counselor: {}, elderly: {}", counselorUserId, elderlyUserId);
            throw new AccessDeniedException("담당하지 않는 어르신의 정보에 접근할 수 없습니다.");
        }
    }

    /**
     * 보호자의 민감정보 접근 권한 확인
     */
    private void validateGuardianAccess(Long guardianUserId, Long elderlyUserId, AccessScope scope) {
        boolean isGuardian = guardianElderlyRepo.existsByGuardianIdAndElderlyId(guardianUserId, elderlyUserId);
        if (!isGuardian)
            throw new AccessDeniedException("연결된 보호자가 아닙니다.");

        boolean hasValidAccess = accessRequestRepo.hasValidAccess(guardianUserId, elderlyUserId, scope,
                LocalDateTime.now());
        if (!hasValidAccess)
            throw new AccessDeniedException("민감정보 열람 승인이 필요합니다.");
    }
}