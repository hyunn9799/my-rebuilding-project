package com.aicc.silverlink.domain.admin.service;

import com.aicc.silverlink.domain.admin.dto.AdminMemberDtos;
import com.aicc.silverlink.domain.admin.entity.OfflineRegistrationLog;
import com.aicc.silverlink.domain.admin.repository.OfflineRegistrationLogRepository;
import com.aicc.silverlink.domain.auth.entity.PhoneVerification;
import com.aicc.silverlink.domain.auth.repository.PhoneVerificationRepository;
import com.aicc.silverlink.domain.consent.entity.ConsentHistory;
import com.aicc.silverlink.domain.consent.repository.ConsentHistoryRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.util.SecurityUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class OfflineRegistrationService {

    private final UserRepository userRepository;
    private final ElderlyRepository elderlyRepository;
    private final GuardianRepository guardianRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final AdministrativeDivisionRepository divRepository;
    private final OfflineRegistrationLogRepository logRepository;
    private final ConsentHistoryRepository consentRepository;
    private final PhoneVerificationRepository phoneVerificationRepository;
    private final PasswordEncoder passwordEncoder;
    private final com.aicc.silverlink.domain.admin.repository.AdminRepository adminRepository;
    private final com.aicc.silverlink.domain.consent.repository.AccessRequestRepository accessRequestRepository;

    @Transactional
    public Long registerElderly(AdminMemberDtos.RegisterElderlyRequest req) {
        Long adminId = SecurityUtils.currentUserId();

        // 1. User 생성
        validateDuplicate(req.loginId(), req.phone());
        User user = User.createLocal(
                req.loginId(),
                passwordEncoder.encode(req.password()),
                req.name(),
                req.phone(),
                req.email(),
                Role.ELDERLY,
                adminId); // created_by에 관리자 ID 저장
        user.markPhoneVerified(); // 대면 인증 처리
        userRepository.save(user);

        // 2. Elderly 프로필 생성
        // Daum bcode(법정동 10자리) → DB adm_code 매핑
        AdministrativeDivision div = findDivisionByCode(req.admCode());

        Elderly elderly = Elderly.create(user, div, req.birthDate(), req.gender());
        elderly.updateAddress(req.addressLine1(), req.addressLine2(), req.zipcode());

        // 통화 스케줄 설정 (선택사항)
        if (req.preferredCallTime() != null || req.preferredCallDays() != null) {
            elderly.updateCallSchedule(
                    req.preferredCallTime(),
                    req.getCallDaysAsString(),
                    req.callScheduleEnabled() != null ? req.callScheduleEnabled() : false);
        }

        elderlyRepository.save(elderly);

        // 3. 로그 기록
        saveLog(adminId, user.getId(), req.memo());
        saveVerification(user);
        saveConsent(user, "PRIVACY", "AGREE");
        saveConsent(user, "SENSITIVE_INFO", "AGREE");
        saveConsent(user, "MEDICATION", "AGREE"); // 기본 동의 간주

        return user.getId();
    }

    @Transactional
    public Long registerGuardian(AdminMemberDtos.RegisterGuardianRequest req) {
        Long adminId = SecurityUtils.currentUserId();

        // 1. User 생성
        validateDuplicate(req.loginId(), req.phone());
        User user = User.createLocal(
                req.loginId(),
                passwordEncoder.encode(req.password()),
                req.name(),
                req.phone(),
                req.email(),
                Role.GUARDIAN,
                adminId); // created_by에 관리자 ID 저장
        user.markPhoneVerified();
        userRepository.save(user);

        // 2. Guardian 프로필
        Guardian guardian = Guardian.create(
                user,
                req.addressLine1(),
                req.addressLine2(),
                req.zipcode(),
                LocalDateTime.now());
        guardianRepository.save(guardian);

        // 3. 연결
        Elderly elderly = elderlyRepository.findById(req.elderlyUserId())
                .orElseThrow(() -> new IllegalArgumentException("ELDERLY_NOT_FOUND"));

        // Enum 변환
        RelationType relationType;
        try {
            relationType = RelationType.valueOf(req.relationType());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("INVALID_RELATION_TYPE");
        }

        GuardianElderly relation = GuardianElderly.create(
                guardian,
                elderly,
                relationType,
                LocalDateTime.now());
        guardianElderlyRepository.save(relation);

        // 4. 로그
        saveLog(adminId, user.getId(), req.memo());
        saveVerification(user);
        saveConsent(user, "PRIVACY", "AGREE");
        saveConsent(user, "THIRD_PARTY", "AGREE");

        // 5. 민감 정보 접근 권한 자동 승인 (오프라인 가입 혜택)
        createApprovedAccessRequests(adminId, user, elderly);

        return user.getId();
    }

    private void createApprovedAccessRequests(Long adminId, User guardian, Elderly elderly) {
        // 관리자 정보 조회 (승인자)
        com.aicc.silverlink.domain.admin.entity.Admin admin = adminRepository.findByUserId(adminId)
                .orElseThrow(() -> new IllegalArgumentException("ADMIN_NOT_FOUND"));

        createApprovedAccessRequest(admin, guardian, elderly,
                com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope.CALL_RECORDS);
        createApprovedAccessRequest(admin, guardian, elderly,
                com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope.HEALTH_INFO);
        createApprovedAccessRequest(admin, guardian, elderly,
                com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope.MEDICATION);
    }

    private void createApprovedAccessRequest(com.aicc.silverlink.domain.admin.entity.Admin admin,
            User guardian,
            Elderly elderly,
            com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope scope) {
        // 이미 존재하는지 확인
        if (accessRequestRepository.findActiveRequest(guardian.getId(), elderly.getId(), scope).isPresent()) {
            return;
        }

        com.aicc.silverlink.domain.consent.entity.AccessRequest request = com.aicc.silverlink.domain.consent.entity.AccessRequest
                .builder()
                .requester(guardian)
                .elderly(elderly)
                .scope(scope) // status, verified, etc are set via builder if possible, or set after?
                // Builder in AccessRequest is private all args? No, it's public class builder.
                // But does it let me set status? Yes.
                .status(com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessRequestStatus.APPROVED)
                .documentVerified(true)
                .reviewedByAdmin(admin)
                .requestedAt(LocalDateTime.now())
                .decidedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusYears(1)) // 1년 유효
                .decisionNote("오프라인 가입 자동 승인")
                .build();

        accessRequestRepository.save(request);
    }

    private void validateDuplicate(String loginId, String phone) {
        if (userRepository.findByLoginId(loginId).isPresent()) {
            throw new IllegalArgumentException("LOGIN_ID_DUPLICATE");
        }
        if (userRepository.existsByPhone(phone)) {
            throw new IllegalArgumentException("PHONE_DUPLICATE");
        }
    }

    private void saveLog(Long adminId, Long targetId, String memo) {
        logRepository.save(OfflineRegistrationLog.builder()
                .registrarUserId(adminId)
                .targetUserId(targetId)
                .channel("CENTER_VISIT")
                .memo(memo)
                .build());
    }

    private void saveVerification(User user) {
        phoneVerificationRepository.save(PhoneVerification.builder()
                .user(user)
                .phoneE164(user.getPhone())
                .codeHash("OFFLINE")
                .status(PhoneVerification.Status.VERIFIED)
                .purpose(PhoneVerification.Purpose.SIGNUP)
                .failCount(0)
                .expiresAt(LocalDateTime.now().plusYears(1))
                .requestIp("OFFLINE")
                .build());
    }

    private void saveConsent(User user, String type, String action) {
        consentRepository.save(ConsentHistory.builder()
                .user(user)
                .consentType(type)
                .actionType(action)
                .consentDate(LocalDateTime.now())
                .ipAddress("OFFLINE")
                .userAgent("ADMIN_CONSOLE")
                .build());
    }

    /**
     * Daum bcode(법정동 10자리) → DB adm_code 매핑
     * 1차: 정확한 코드 매칭
     * 2차: sidoCode + sigunguCode로 검색
     * 3차: sidoCode만으로 검색
     * 4차: 기본 행정구역 (fallback)
     */
    private AdministrativeDivision findDivisionByCode(Long codeInput) {
        // 1차: 정확한 매칭 시도
        var exact = divRepository.findById(codeInput);
        if (exact.isPresent()) {
            return exact.get();
        }

        String codeStr = String.valueOf(codeInput);

        // 10자리 법정동 코드인 경우
        if (codeStr.length() == 10) {
            // 법정동 코드 앞 8자리 시도
            Long code8 = Long.parseLong(codeStr.substring(0, 8));
            var match8 = divRepository.findById(code8);
            if (match8.isPresent()) {
                return match8.get();
            }

            // sido(앞 2자리) + sigungu(3-5자리) 로 검색
            String sidoCode = codeStr.substring(0, 2);
            String sigunguCode = codeStr.substring(2, 5);

            var byCodeList = divRepository.findBySidoAndSigungu(sidoCode, sigunguCode);
            if (!byCodeList.isEmpty()) {
                return byCodeList.get(0);
            }

            // sido만으로 검색
            var bySidoList = divRepository.findBySidoAndSigungu(sidoCode, null);
            if (!bySidoList.isEmpty()) {
                return bySidoList.get(0);
            }
        }

        // 8자리인 경우
        if (codeStr.length() == 8) {
            String sidoCode = codeStr.substring(0, 2);
            String sigunguCode = codeStr.substring(2, 5);

            var byCodeList = divRepository.findBySidoAndSigungu(sidoCode, sigunguCode);
            if (!byCodeList.isEmpty()) {
                return byCodeList.get(0);
            }

            var bySidoList = divRepository.findBySidoAndSigungu(sidoCode, null);
            if (!bySidoList.isEmpty()) {
                return bySidoList.get(0);
            }
        }

        // 최후의 fallback: 아무 활성 행정구역이나 사용
        return divRepository.findAnyActive()
                .orElseThrow(() -> new IllegalArgumentException("INVALID_ADM_CODE"));
    }
}
