package com.aicc.silverlink.domain.counselor.service;

import com.aicc.silverlink.domain.assignment.repository.AssignmentRepository;
import com.aicc.silverlink.domain.counselor.dto.CounselorRequest;
import com.aicc.silverlink.domain.counselor.dto.CounselorResponse;
import com.aicc.silverlink.domain.counselor.dto.CounselorUpdateRequest;
import com.aicc.silverlink.domain.counselor.entity.Counselor;
import com.aicc.silverlink.domain.counselor.repository.CounselorRepository;
import com.aicc.silverlink.domain.system.entity.AdministrativeDivision;
import com.aicc.silverlink.domain.system.repository.AdministrativeDivisionRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@RequiredArgsConstructor
@Service
@Transactional(readOnly = true)
public class CounselorService {

    private final CounselorRepository counselorRepository;
    private final UserRepository userRepository;
    private final AdministrativeDivisionRepository divisionRepository;
    private final AssignmentRepository assignmentRepository;
    private final PasswordEncoder passwordEncoder;

    @Transactional
    public CounselorResponse register(CounselorRequest request) {
        if (userRepository.existsByLoginId(request.getLoginId())) {
            throw new IllegalArgumentException("이미 사용 중인 아이디입니다.");
        }

        AdministrativeDivision division = divisionRepository.findById(request.getAdmCode())
                .orElseThrow(() -> new IllegalArgumentException("존재하지 않는 행정구역 코드입니다: " + request.getAdmCode()));

        String encodedPassword = passwordEncoder.encode(request.getPassword());

        User user = User.createLocal(
                request.getLoginId(),
                encodedPassword,
                request.getName(),
                request.getPhone(),
                request.getEmail(),
                Role.COUNSELOR,
                null // 자가 등록이므로 createdBy 없음
        );

        userRepository.save(user);

        Counselor counselor = Counselor.create(
                user,
                request.getEmployeeNo(),
                request.getDepartment(),
                request.getOfficePhone(),
                request.getJoinedAt(),
                division);
        counselorRepository.save(counselor);

        log.info("상담사 등록 완료 - userId: {}, admCode: {}", user.getId(), request.getAdmCode());

        return CounselorResponse.from(counselor);
    }

    public CounselorResponse getCounselor(Long id) {
        Counselor counselor = counselorRepository.findByIdWithUser(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상담사를 찾을 수 없습니다."));
        int assignedCount = assignmentRepository.countActiveByCounselorId(counselor.getId());
        return CounselorResponse.from(counselor, assignedCount);
    }

    public List<CounselorResponse> getAllCounselors() {
        return counselorRepository.findAllWithUser().stream()
                .map(counselor -> {
                    int assignedCount = assignmentRepository.countActiveByCounselorId(counselor.getId());
                    return CounselorResponse.from(counselor, assignedCount);
                })
                .collect(Collectors.toList());
    }

    /**
     * 상담사 본인 정보 수정 로직
     */
    @Transactional
    public CounselorResponse updateCounselor(Long id, CounselorUpdateRequest request) {
        // 1. 기존 상담사 정보 조회 (Fetch Join 등을 통해 User를 함께 가져오는 것을 권장)
        Counselor counselor = counselorRepository.findByIdWithUser(id)
                .orElseThrow(() -> new IllegalArgumentException("해당 상담사를 찾을 수 없습니다."));

        // 2. 연관된 User 정보 업데이트 (User 엔티티에 updateProfile 메서드가 있어야 함)
        User user = counselor.getUser();
        user.updateProfile(request.getName(), request.getPhone(), request.getEmail());

        // 3. Counselor 전용 정보 업데이트
        counselor.updateInfo(request.getDepartment(), request.getOfficePhone());

        log.info("상담사 정보 수정 완료 - userId: {}", id);

        // 4. Dirty Checking에 의해 트랜잭션 종료 시 자동 반영됨
        return CounselorResponse.from(counselor);
    }

    public List<CounselorResponse> getCounselorsByAdmCode(Long admCode) {
        return counselorRepository.findByAdmCode(admCode).stream()
                .map(CounselorResponse::from)
                .collect(Collectors.toList());
    }

    public List<CounselorResponse> getCounselorsBySido(String sidoCode) {
        return counselorRepository.findBySidoCode(sidoCode).stream()
                .map(CounselorResponse::from)
                .collect(Collectors.toList());
    }
}