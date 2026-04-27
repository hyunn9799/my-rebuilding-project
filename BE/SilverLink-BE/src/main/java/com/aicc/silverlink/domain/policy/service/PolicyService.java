package com.aicc.silverlink.domain.policy.service;

import com.aicc.silverlink.domain.admin.entity.Admin;
import com.aicc.silverlink.domain.admin.repository.AdminRepository;
import com.aicc.silverlink.domain.audit.service.AuditLogService;
import com.aicc.silverlink.domain.policy.dto.PolicyRequest;
import com.aicc.silverlink.domain.policy.dto.PolicyResponse;
import com.aicc.silverlink.domain.policy.entity.Policy;
import com.aicc.silverlink.domain.policy.entity.PolicyType;
import com.aicc.silverlink.domain.policy.repository.PolicyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PolicyService {

    private final PolicyRepository policyRepository;
    private final AdminRepository adminRepository;
    private final AuditLogService auditLogService;

    /**
     * [관리자용] 전체 정책 목록 조회
     */
    public List<PolicyResponse> getAll() {
        log.info("전체 정책 목록 조회");
        return policyRepository.findAllByOrderByCreatedAtDesc()
                .stream()
                .map(PolicyResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * [관리자용] 새로운 약관 버전 등록
     * 수정됨: 관리자(Admin)라면 레벨 상관없이 누구나 등록 가능
     */
    @Transactional
    public PolicyResponse create(PolicyRequest req, Long adminUserId) {
        log.info("약관 생성 요청 - type: {}, version: {}, adminId: {}",
                req.getPolicyType(), req.getVersion(), adminUserId);

        // 1. 중복 버전 체크
        if (policyRepository.existsByPolicyTypeAndVersion(req.getPolicyType(), req.getVersion())) {
            throw new IllegalArgumentException("이미 존재하는 정책 버전입니다: " + req.getVersion());
        }

        // 2. 관리자 존재 여부만 확인 (레벨 체크 X)
        Admin admin = adminRepository.findByIdWithUser(adminUserId)
                .orElseThrow(() -> new IllegalArgumentException("관리자 권한이 없습니다. (등록된 관리자가 아님)"));

        // 3. 저장
        Policy savedPolicy = policyRepository.save(req.toEntity(admin.getUser()));

        log.info("약관 생성 완료 - ID: {}", savedPolicy.getId());
        log.info("약관 생성 완료 - ID: {}", savedPolicy.getId());

        // 감사 로그 기록
        auditLogService.recordLog(
                admin.getUser().getId(),
                "CREATE_POLICY",
                "Policy",
                savedPolicy.getId(),
                "API",
                "Type: " + savedPolicy.getPolicyType());

        return PolicyResponse.from(savedPolicy);
    }

    /**
     * [사용자/공통] 최신 약관 조회
     */
    public PolicyResponse getLatest(PolicyType policyType) {
        Policy policy = policyRepository.findFirstByPolicyTypeOrderByCreatedAtDesc(policyType)
                .orElseThrow(() -> new IllegalArgumentException("해당 정책을 찾을 수 없습니다: " + policyType.getDescription()));

        return PolicyResponse.from(policy);
    }
}