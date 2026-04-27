package com.aicc.silverlink.domain.admin.service;

import com.aicc.silverlink.domain.admin.dto.request.AdminCreateRequest;
import com.aicc.silverlink.domain.admin.dto.request.AdminUpdateRequest;
import com.aicc.silverlink.domain.admin.dto.response.AdminResponse;
import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.entity.Admin.AdminLevel;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Admin Service 구현체
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminServiceImpl implements AdminService {

    private final AdminRepository adminRepository;
    private final UserRepository userRepository;
    private final AdministrativeDivisionRepository divisionRepository;

    /**
     * 관리자 생성
     */
    @Override
    @Transactional
    public AdminResponse createAdmin(AdminCreateRequest request) {
        log.info("관리자 생성 요청 - userId: {}, admCode: {}, adminLevel: {}",
                request.getUserId(), request.getAdmCode(), request.getAdminLevel());

        // 사용자 존재 여부 확인
        User user = userRepository.findById(request.getUserId())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 사용자입니다."));

        // 이미 관리자로 등록되었는지 확인
        if (adminRepository.existsByUserId(request.getUserId())) {
            throw new IllegalArgumentException("이미 관리자로 등록된 사용자입니다.");
        }

        // 행정구역 존재 여부 확인
        AdministrativeDivision division = divisionRepository.findById(request.getAdmCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정구역 코드입니다: " + request.getAdmCode()));

        // 관리자 생성 (adminLevel이 null이면 자동으로 결정됨)
        Admin admin = Admin.builder()
                .user(user)
                .administrativeDivision(division)
                .adminLevel(request.getAdminLevel())
                .build();

        Admin savedAdmin = adminRepository.save(admin);
        log.info("관리자 생성 완료 - userId: {}, level: {}",
                savedAdmin.getUserId(), savedAdmin.getAdminLevel());

        return AdminResponse.from(savedAdmin);
    }

    /**
     * 관리자 정보 조회
     */
    @Override
    public AdminResponse getAdmin(Long userId) {
        log.info("관리자 조회 - userId: {}", userId);

        Admin admin = adminRepository.findByIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        return AdminResponse.from(admin);
    }

    /**
     * 모든 관리자 조회
     */
    @Override
    public List<AdminResponse> getAllAdmins() {
        log.info("모든 관리자 조회");

        return adminRepository.findAllWithUser().stream()
                .map(AdminResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 행정구역 코드로 관리자 조회
     */
    @Override
    public List<AdminResponse> getAdminsByAdmCode(Long admCode) {
        log.info("행정구역 코드로 관리자 조회 - admCode: {}", admCode);

        return adminRepository.findByAdmCode(admCode).stream()
                .map(AdminResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 관리자 레벨로 조회
     */
    @Override
    public List<AdminResponse> getAdminsByLevel(AdminLevel adminLevel) {
        log.info("관리자 레벨로 조회 - adminLevel: {}", adminLevel);

        return adminRepository.findByAdminLevel(adminLevel).stream()
                .map(AdminResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 행정구역의 상위 관리자들 조회
     */
    @Override
    public List<AdminResponse> getSupervisors(Long admCode) {
        log.info("상위 관리자 조회 - admCode: {}", admCode);

        return adminRepository.findSupervisors(admCode).stream()
                .map(AdminResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 특정 관리자의 하위 관리자들 조회
     */
    @Override
    public List<AdminResponse> getSubordinates(Long adminUserId) {
        log.info("하위 관리자 조회 - adminUserId: {}", adminUserId);

        Admin admin = adminRepository.findByIdWithUser(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        return adminRepository.findSubordinates(
                        admin.getAdminLevel(),
                        admin.getAdmCode(),
                        admin.getAdminLevel().getCodeLength()
                ).stream()
                .map(AdminResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 관리자가 특정 구역에 대한 권한이 있는지 확인
     */
    @Override
    public boolean hasJurisdiction(Long adminUserId, Long targetAdmCode) {
        log.info("권한 확인 - adminUserId: {}, targetAdmCode: {}",
                adminUserId, targetAdmCode);

        Admin admin = adminRepository.findByIdWithUser(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        boolean hasJurisdiction = admin.hasJurisdiction(targetAdmCode);
        log.info("권한 확인 결과: {}", hasJurisdiction);

        return hasJurisdiction;
    }

    /**
     * 관리자 정보 수정
     */
    @Override
    @Transactional
    public AdminResponse updateAdmin(Long userId, AdminUpdateRequest request) {
        log.info("관리자 정보 수정 - userId: {}, newAdmCode: {}",
                userId, request.getAdmCode());

        Admin admin = adminRepository.findByIdWithUser(userId)
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 관리자입니다."));

        // 새로운 행정구역 존재 여부 확인
        AdministrativeDivision newDivision = divisionRepository.findById(request.getAdmCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정구역 코드입니다: " + request.getAdmCode()));

        // 담당 구역 변경 (레벨도 자동으로 재계산됨)
        admin.updateAdministrativeDivision(newDivision);

        log.info("관리자 정보 수정 완료 - userId: {}, newLevel: {}",
                userId, admin.getAdminLevel());
        return AdminResponse.from(admin);
    }

    /**
     * 관리자 삭제
     */
    @Override
    @Transactional
    public void deleteAdmin(Long userId) {
        log.info("관리자 삭제 - userId: {}", userId);

        if (!adminRepository.existsByUserId(userId)) {
            throw new IllegalArgumentException("존재하지 않는 관리자입니다.");
        }

        adminRepository.deleteById(userId);
        log.info("관리자 삭제 완료 - userId: {}", userId);
    }
}