package com.aicc.silverlink.domain.guardian.service;

import com.aicc.silverlink.domain.assignment.entity.AssignmentStatus;
import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.guardian.dto.GuardianElderlyResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianRequest;
import com.aicc.silverlink.domain.guardian.dto.GuardianResponse;
import com.aicc.silverlink.domain.guardian.dto.GuardianUpdateRequest;
import com.aicc.silverlink.domain.guardian.entity.Guardian;
import com.aicc.silverlink.domain.guardian.entity.GuardianElderly;
import com.aicc.silverlink.domain.guardian.entity.RelationType;
import com.aicc.silverlink.domain.guardian.repository.GuardianElderlyRepository;
import com.aicc.silverlink.domain.guardian.repository.GuardianRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Service
public class GuardianService {

    private final GuardianRepository guardianRepository;
    private final GuardianElderlyRepository guardianElderlyRepository;
    private final UserRepository userRepository;
    private final ElderlyRepository elderlyRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public GuardianResponse register(GuardianRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.createLocal(
                request.getLoginId(),
                encodedPassword,
                request.getName(),
                request.getPhone(),
                request.getEmail(),
                Role.GUARDIAN,
                null // 자가 등록이므로 createdBy 없음
        );
        userRepository.save(user);

        Guardian guardian = Guardian.create(
                user,
                request.getAddressLine1(),
                request.getAddressLine2(),
                request.getZipcode(),
                LocalDateTime.now());
        guardianRepository.save(guardian);
        return GuardianResponse.from(guardian);
    }

    public GuardianResponse getGuardian(Long id) {
        Guardian guardian = guardianRepository.findByIdWithUser(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 보호자를 찾을 수 없습니다."));
        return GuardianResponse.from(guardian);
    }

    // ✅ [추가] 어르신 ID를 통해 연결된 보호자 정보 조회
    public GuardianResponse getGuardianByElderly(Long elderlyId) {
        GuardianElderly relation = guardianElderlyRepository.findByElderlyId(elderlyId)
                .orElseThrow(() -> new IllegalArgumentException("해당 어르신과 연결된 보호자 정보가 없습니다."));

        return GuardianResponse.from(relation.getGuardian());
    }

    public GuardianResponse getGuardianByElderlyOrNull(Long elderlyId) {
        return guardianElderlyRepository.findByElderlyId(elderlyId)
                .map(relation -> GuardianResponse.from(relation.getGuardian()))
                .orElse(null);
    }

    public GuardianResponse getGuardianForCounselor(Long guardianId, Long counselorId) {
        Guardian guardian = guardianRepository.findByIdWithUser(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("해당 보호자를 찾을 수 없습니다."));

        GuardianElderly relation = guardianElderlyRepository.findByGuardianId(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("이 보호자와 연결된 어르신 정보가 없습니다."));

        validateAssignment(counselorId, relation.getElderly().getId());
        return GuardianResponse.from(guardian);
    }

    @Transactional
    public void connectElderly(Long guardianId, Long elderlyId, RelationType relationType) {
        // 어르신 기준으로 이미 보호자가 있는지 체크 (1:1 규칙)
        if (guardianElderlyRepository.existsByElderly_Id(elderlyId)) {
            throw new IllegalArgumentException("이미 다른 보호자가 등록한 어르신입니다.");
        }
        // 보호자 기준으로 이미 어르신이 있는지 체크 (추가된 1:1 규칙)
        if (guardianElderlyRepository.existsByGuardian_Id(guardianId)) {
            throw new IllegalArgumentException("이 보호자는 이미 다른 어르신을 담당하고 있습니다.");
        }

        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("보호자를 찾을 수 없습니다."));
        Elderly elderly = elderlyRepository.findById(elderlyId)
                .orElseThrow(() -> new IllegalArgumentException("어르신을 찾을 수 없습니다."));

        GuardianElderly relation = GuardianElderly.create(guardian, elderly, relationType, LocalDateTime.now());
        guardianElderlyRepository.save(relation);
    }

    public List<GuardianResponse> getAllGuardian() {
        Map<Long, String> elderlyMap = guardianElderlyRepository.findAllWithDetails().stream()
                .collect(Collectors.toMap(
                        ge -> ge.getGuardian().getId(),
                        ge -> ge.getElderly().getUser().getName(),
                        (existing, replacement) -> existing));

        return guardianRepository.findAllWithUser().stream()
                .map(guardian -> {
                    String elderlyName = elderlyMap.get(guardian.getId());
                    int elderlyCount = elderlyName != null ? 1 : 0;
                    return GuardianResponse.from(guardian, elderlyCount, elderlyName);
                })
                .collect(Collectors.toList());
    }

    public GuardianElderlyResponse getElderlyByGuardian(Long guardianId) {
        GuardianElderly guardianElderly = guardianElderlyRepository.findByGuardianId(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("보호할 어르신이 없습니다."));
        return GuardianElderlyResponse.from(guardianElderly);
    }

    public GuardianElderlyResponse getElderlyByGuardianForCounselor(Long guardianId, Long counselorId) {
        GuardianElderly guardianElderly = guardianElderlyRepository.findByGuardianId(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("보호할 어르신이 없습니다."));

        validateAssignment(counselorId, guardianElderly.getElderly().getId());
        return GuardianElderlyResponse.from(guardianElderly);
    }

    private void validateAssignment(Long counselorId, Long elderlyId) {
        boolean isAssigned = assignmentRepository.existsByCounselor_IdAndElderly_IdAndStatus(
                counselorId, elderlyId, AssignmentStatus.ACTIVE);

        if (!isAssigned) {
            log.warn("Access Denied: Counselor {} tried to access unassigned Elderly {}", counselorId, elderlyId);
            throw new IllegalArgumentException("본인이 담당하는 어르신의 보호자 정보만 조회할 수 있습니다.");
        }
    }

    @Transactional
    public void withdrawGuardian(Long guardianId) {
        Guardian guardian = guardianRepository.findById(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("GUARDIAN_NOT_FOUND"));

        // 1. 관계 데이터 삭제 (서비스 상에서 즉시 분리)
        // 법적 보관이 필요하다면 이 매핑 데이터도 Soft Delete를 고려할 수 있지만,
        // 보통 유저 로그로 증빙이 가능하므로 매핑은 Hard Delete 하거나 '종료일'을 기록합니다.
        guardianElderlyRepository.deleteByGuardianId(guardianId);

        // 2. 유저 소프트 딜리트 (5년 보관을 위해 상태만 변경)
        guardian.getUser().softDelete();

        log.info("보호자 탈퇴 처리 완료 (5년 보관 모드) - guardianId: {}", guardianId);
    }

    @Transactional
    public GuardianResponse updateGuardianProfile(Long guardianId, GuardianUpdateRequest req) {
        Guardian guardian = guardianRepository.findByIdWithUser(guardianId)
                .orElseThrow(() -> new IllegalArgumentException("GUARDIAN_NOT_FOUND"));

        // User 정보 수정
        guardian.getUser().updateProfile(req.name(), req.phone(), req.email());

        // Guardian 주소 정보 수정
        guardian.updateAddress(req.addressLine1(), req.addressLine2(), req.zipcode());

        return GuardianResponse.from(guardian);
    }
}