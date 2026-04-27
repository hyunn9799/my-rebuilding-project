package com.aicc.silverlink.domain.audit.repository;

import com.aicc.silverlink.domain.audit.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {

    // 1. 특정 행위만 골라보기 (예: "약관 생성(CREATE_POLICY)" 로그만 조회)
    Page<AuditLog> findByAction(String action, Pageable pageable);

    // 2. 특정 관리자가 한 일만 추적하기 (예: "김철수 관리자가 무슨 사고 쳤지?")
    // 주의: User 객체 안에 id가 있으므로 언더바(_)로 탐색하거나 findByActorId 사용
    Page<AuditLog> findByActor_Id(Long actorId, Pageable pageable);

    // 3. 특정 대상의 이력 조회 (예: "15번 약관이 언제 수정되고 언제 삭제됐지?")
    // 3. 특정 대상의 이력 조회 (예: "15번 약관이 언제 수정되고 언제 삭제됐지?")
    Page<AuditLog> findByTargetEntityAndTargetId(String targetEntity, Long targetId, Pageable pageable);

    @Override
    @EntityGraph(attributePaths = { "actor" })
    Page<AuditLog> findAll(Pageable pageable);
}