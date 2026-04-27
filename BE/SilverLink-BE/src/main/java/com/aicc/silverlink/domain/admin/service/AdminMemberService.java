package com.aicc.silverlink.domain.admin.service;

import com.aicc.silverlink.domain.admin.dto.AdminMemberDtos;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianRepository;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import com.aicc.silverlink.global.exception.BusinessException;
import com.aicc.silverlink.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * 관리자 회원 관리 서비스
 * 회원 수정/삭제 기능을 제공합니다.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class AdminMemberService {

    private final UserRepository userRepository;
    private final ElderlyRepository elderlyRepository;
    private final GuardianRepository guardianRepository;
    private final CounselorRepository counselorRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final AssignmentRepository assignmentRepository;

    /**
     * 회원 정보 수정 (이름, 전화번호, 이메일)
     */
    @Transactional
    public AdminMemberDtos.UpdateMemberResponse updateUser(Long userId, AdminMemberDtos.UpdateMemberRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        log.info("회원 수정 시작: userId={}, name={}", userId, request.name());

        user.updateProfile(request.name(), request.phone(), request.email());

        log.info("회원 수정 완료: userId={}", userId);

        return new AdminMemberDtos.UpdateMemberResponse(
                user.getId(),
                user.getName(),
                user.getPhone(),
                user.getEmail(),
                user.getRole().name());
    }

    /**
     * 회원 삭제 (역할에 따라 관련된 데이터를 함께 삭제)
     *
     * - ELDERLY: 어르신-보호자 관계, 어르신-상담사 배정 삭제 후 어르신 삭제
     * - GUARDIAN: 보호자-어르신 관계 삭제 후 보호자 삭제
     * - COUNSELOR: 상담사-어르신 배정 해제 후 상담사 삭제
     * - ADMIN: Soft delete 처리
     */
    @Transactional
    public void deleteUser(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다."));

        Role role = user.getRole();
        log.info("회원 삭제 시작: userId={}, role={}", userId, role);

        switch (role) {
            case ELDERLY -> deleteElderly(userId);
            case GUARDIAN -> deleteGuardian(userId);
            case COUNSELOR -> deleteCounselor(userId);
            case ADMIN -> deleteAdmin(userId);
            default -> throw new BusinessException(ErrorCode.INVALID_INPUT_VALUE, "알 수 없는 역할입니다.");
        }

        log.info("회원 삭제 완료: userId={}", userId);
    }

    /**
     * 어르신 삭제
     * 1. 어르신-보호자 관계 삭제
     * 2. 어르신-상담사 배정 삭제
     * 3. Elderly 엔티티 삭제
     * 4. User soft delete
     */
    private void deleteElderly(Long elderlyId) {
        // 1. 보호자-어르신 관계 삭제
        guardianElderlyRepository.deleteByElderlyId(elderlyId);
        log.debug("어르신-보호자 관계 삭제 완료: elderlyId={}", elderlyId);

        // 2. 상담사-어르신 배정 삭제
        assignmentRepository.deleteByElderlyId(elderlyId);
        log.debug("어르신 배정 삭제 완료: elderlyId={}", elderlyId);

        // 3. Elderly 엔티티 삭제
        elderlyRepository.deleteById(elderlyId);
        log.debug("Elderly 엔티티 삭제 완료: elderlyId={}", elderlyId);

        // 4. User soft delete
        userRepository.findById(elderlyId).ifPresent(user -> {
            user.softDelete();
            log.debug("User soft delete 완료: userId={}", elderlyId);
        });
    }

    /**
     * 보호자 삭제
     * 1. 보호자-어르신 관계 삭제
     * 2. Guardian 엔티티 삭제
     * 3. User soft delete
     */
    private void deleteGuardian(Long guardianId) {
        // 1. 보호자-어르신 관계 삭제
        guardianElderlyRepository.deleteByGuardianId(guardianId);
        log.debug("보호자-어르신 관계 삭제 완료: guardianId={}", guardianId);

        // 2. Guardian 엔티티 삭제
        guardianRepository.deleteById(guardianId);
        log.debug("Guardian 엔티티 삭제 완료: guardianId={}", guardianId);

        // 3. User soft delete
        userRepository.findById(guardianId).ifPresent(user -> {
            user.softDelete();
            log.debug("User soft delete 완료: userId={}", guardianId);
        });
    }

    /**
     * 상담사 삭제
     * 1. 상담사-어르신 배정 삭제
     * 2. Counselor 엔티티 삭제
     * 3. User soft delete
     */
    private void deleteCounselor(Long counselorId) {
        // 1. 모든 배정 삭제
        assignmentRepository.deleteByCounselorId(counselorId);
        log.debug("상담사 배정 삭제 완료: counselorId={}", counselorId);

        // 2. Counselor 엔티티 삭제
        counselorRepository.deleteById(counselorId);
        log.debug("Counselor 엔티티 삭제 완료: counselorId={}", counselorId);

        // 3. User soft delete
        userRepository.findById(counselorId).ifPresent(user -> {
            user.softDelete();
            log.debug("User soft delete 완료: userId={}", counselorId);
        });
    }

    /**
     * 관리자 삭제 (Soft delete)
     */
    private void deleteAdmin(Long adminId) {
        userRepository.findById(adminId).ifPresent(user -> {
            user.softDelete();
            log.debug("Admin soft delete 완료: userId={}", adminId);
        });
    }
}
