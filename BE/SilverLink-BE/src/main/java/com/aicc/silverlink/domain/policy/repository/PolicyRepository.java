package com.aicc.silverlink.domain.policy.repository;

import com.aicc.silverlink.domain.policy.entity.Policy;
import com.aicc.silverlink.domain.policy.entity.PolicyType;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PolicyRepository extends JpaRepository<Policy, Long> {

    /**
     * 전체 정책 목록을 생성일 기준 내림차순으로 조회합니다.
     */
    List<Policy> findAllByOrderByCreatedAtDesc();

    /**
     * 특정 정책 타입(Enum)에 해당하는 가장 최신(생성일 기준 내림차순) 정책 1개를 조회합니다.
     * 사용 예:
     * repo.findFirstByPolicyTypeOrderByCreatedAtDesc(PolicyType.TERMS_OF_SERVICE);
     */
    Optional<Policy> findFirstByPolicyTypeOrderByCreatedAtDesc(PolicyType policyType);

    /**
     * 특정 정책 타입과 버전이 이미 존재하는지 확인합니다. (중복 등록 방지용)
     * 사용 예: repo.existsByPolicyTypeAndVersion(PolicyType.TERMS_OF_SERVICE, "v1.0");
     */
    boolean existsByPolicyTypeAndVersion(PolicyType policyType, String version);

}