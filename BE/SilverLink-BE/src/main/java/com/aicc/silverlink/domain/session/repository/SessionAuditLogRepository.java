package com.aicc.silverlink.domain.session.repository;

import com.aicc.silverlink.domain.session.entity.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.Instant;
import java.util.List;

/**
 * 세션 감사 로그 Repository.
 */
public interface SessionAuditLogRepository extends JpaRepository<AuditLog, Long> {

    /**
     * 특정 사용자의 감사 로그 조회
     */
    List<AuditLog> findByUserIdOrderByCreatedAtDesc(Long userId);

    /**
     * 특정 이벤트 타입의 최근 로그 조회
     */
    Page<AuditLog> findByEventTypeOrderByCreatedAtDesc(String eventType, Pageable pageable);

    /**
     * 특정 기간의 감사 로그 조회
     */
    @Query("SELECT a FROM AuditLog a WHERE a.createdAt BETWEEN :from AND :to ORDER BY a.createdAt DESC")
    List<AuditLog> findByPeriod(@Param("from") Instant from, @Param("to") Instant to);

    /**
     * 중복 로그인 감지 횟수 (관리자 대시보드용)
     */
    long countByEventTypeAndCreatedAtAfter(String eventType, Instant after);
}
