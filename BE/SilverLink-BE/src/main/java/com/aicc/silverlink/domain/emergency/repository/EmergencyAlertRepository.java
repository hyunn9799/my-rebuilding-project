package com.aicc.silverlink.domain.emergency.repository;

import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.AlertStatus;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.AlertType;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert.Severity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyAlertRepository extends JpaRepository<EmergencyAlert, Long> {

        // ========== 기본 조회 ==========

        /**
         * 긴급 알림 상세 조회 (Fetch Join)
         */
        @Query("SELECT ea FROM EmergencyAlert ea " +
                        "LEFT JOIN FETCH ea.elderly e " +
                        "LEFT JOIN FETCH e.user " +
                        "LEFT JOIN FETCH ea.callRecord " +
                        "LEFT JOIN FETCH ea.assignedCounselor c " +
                        "LEFT JOIN FETCH c.user " +
                        "WHERE ea.id = :alertId")
        Optional<EmergencyAlert> findByIdWithDetails(@Param("alertId") Long alertId);

        // ========== 상담사용 조회 ==========

        /**
         * 상담사 담당 어르신의 긴급 알림 목록 (미처리 우선, 최신순)
         */
        @Query("SELECT ea FROM EmergencyAlert ea " +
                        "JOIN ea.elderly e " +
                        "JOIN Assignment a ON a.elderly.id = e.id " +
                        "WHERE a.counselor.id = :counselorId " +
                        "AND a.status = com.aicc.silverlink.domain.assignment.entity.AssignmentStatus.ACTIVE " +
                        "ORDER BY " +
                        "CASE ea.status WHEN 'PENDING' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END, " +
                        "ea.createdAt DESC")
        Page<EmergencyAlert> findByCounselorId(@Param("counselorId") Long counselorId, Pageable pageable);

        /**
         * 상담사 담당 어르신의 미처리 긴급 알림 목록
         */
        @Query("SELECT ea FROM EmergencyAlert ea " +
                        "JOIN ea.elderly e " +
                        "JOIN Assignment a ON a.elderly.id = e.id " +
                        "WHERE a.counselor.id = :counselorId " +
                        "AND a.status = com.aicc.silverlink.domain.assignment.entity.AssignmentStatus.ACTIVE " +
                        "AND ea.status = 'PENDING' " +
                        "ORDER BY ea.severity DESC, ea.createdAt DESC")
        List<EmergencyAlert> findPendingByCounselorId(@Param("counselorId") Long counselorId);

        /**
         * 상담사 담당 어르신의 미처리 긴급 알림 수
         */
        @Query("SELECT COUNT(ea) FROM EmergencyAlert ea " +
                        "JOIN ea.elderly e " +
                        "JOIN Assignment a ON a.elderly.id = e.id " +
                        "WHERE a.counselor.id = :counselorId " +
                        "AND a.status = com.aicc.silverlink.domain.assignment.entity.AssignmentStatus.ACTIVE " +
                        "AND ea.status = 'PENDING'")
        long countPendingByCounselorId(@Param("counselorId") Long counselorId);

        // ========== 관리자용 조회 ==========

        /**
         * 관리자 관할 구역의 긴급 알림 목록
         */
        @Query("SELECT ea FROM EmergencyAlert ea " +
                        "JOIN ea.elderly e " +
                        "WHERE CAST(e.administrativeDivision.admCode AS string) LIKE :admCodePattern " +
                        "ORDER BY " +
                        "CASE ea.status WHEN 'PENDING' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END, " +
                        "ea.createdAt DESC")
        Page<EmergencyAlert> findByAdmCodePattern(
                        @Param("admCodePattern") String admCodePattern,
                        Pageable pageable);

        /**
         * 전체 긴급 알림 목록 (관리자용)
         */
        @Query("SELECT ea FROM EmergencyAlert ea " +
                        "LEFT JOIN FETCH ea.elderly e " +
                        "LEFT JOIN FETCH e.user " +
                        "ORDER BY " +
                        "CASE ea.status WHEN 'PENDING' THEN 0 WHEN 'IN_PROGRESS' THEN 1 ELSE 2 END, " +
                        "ea.createdAt DESC")
        Page<EmergencyAlert> findAllWithDetails(Pageable pageable);

        // ========== 보호자용 조회 ==========

        /**
         * 보호자 담당 어르신의 긴급 알림 목록
         */
        @Query("SELECT ea FROM EmergencyAlert ea " +
                        "JOIN ea.elderly e " +
                        "JOIN GuardianElderly ge ON ge.elderly.id = e.id " +
                        "WHERE ge.guardian.id = :guardianId " +
                        "ORDER BY ea.createdAt DESC")
        Page<EmergencyAlert> findByGuardianId(@Param("guardianId") Long guardianId, Pageable pageable);

        // ========== 필터 조회 ==========

        /**
         * 상태별 조회
         */
        Page<EmergencyAlert> findByStatusOrderByCreatedAtDesc(AlertStatus status, Pageable pageable);

        /**
         * 위험도별 조회
         */
        Page<EmergencyAlert> findBySeverityOrderByCreatedAtDesc(Severity severity, Pageable pageable);

        /**
         * 상태 + 위험도 조회
         */
        Page<EmergencyAlert> findByStatusAndSeverityOrderByCreatedAtDesc(
                        AlertStatus status,
                        Severity severity,
                        Pageable pageable);

        /**
         * 알림 유형별 조회
         */
        Page<EmergencyAlert> findByAlertTypeOrderByCreatedAtDesc(AlertType alertType, Pageable pageable);

        /**
         * 어르신별 긴급 알림 목록
         */
        Page<EmergencyAlert> findByElderlyIdOrderByCreatedAtDesc(Long elderlyId, Pageable pageable);

        // ========== 통계 조회 ==========

        /**
         * 상태별 긴급 알림 수
         */
        long countByStatus(AlertStatus status);

        /**
         * 위험도별 긴급 알림 수
         */
        long countBySeverity(Severity severity);

        /**
         * 상태 + 위험도별 긴급 알림 수
         */
        long countByStatusAndSeverity(AlertStatus status, Severity severity);

        /**
         * 기간별 긴급 알림 수
         */
        @Query("SELECT COUNT(ea) FROM EmergencyAlert ea " +
                        "WHERE ea.createdAt BETWEEN :startDate AND :endDate")
        long countByDateRange(
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * 상담사별 상태 통계 조회
         */
        @Query("SELECT ea.status, COUNT(ea) FROM EmergencyAlert ea " +
                        "JOIN ea.elderly e " +
                        "JOIN Assignment a ON a.elderly.id = e.id " +
                        "WHERE a.counselor.id = :counselorId " +
                        "AND a.status = com.aicc.silverlink.domain.assignment.entity.AssignmentStatus.ACTIVE " +
                        "GROUP BY ea.status")
        List<Object[]> countByStatusForCounselor(@Param("counselorId") Long counselorId);

        /**
         * 상담사별 위험도 통계 조회
         */
        @Query("SELECT ea.severity, COUNT(ea) FROM EmergencyAlert ea " +
                        "JOIN ea.elderly e " +
                        "JOIN Assignment a ON a.elderly.id = e.id " +
                        "WHERE a.counselor.id = :counselorId " +
                        "AND a.status = com.aicc.silverlink.domain.assignment.entity.AssignmentStatus.ACTIVE " +
                        "GROUP BY ea.severity")
        List<Object[]> countBySeverityForCounselor(@Param("counselorId") Long counselorId);

        // ========== 실시간 알림용 ==========

        /**
         * 특정 시간 이후 생성된 미처리 알림 조회 (실시간 폴링용)
         */
        @Query("SELECT ea FROM EmergencyAlert ea " +
                        "WHERE ea.status = 'PENDING' " +
                        "AND ea.createdAt > :since " +
                        "ORDER BY ea.severity DESC, ea.createdAt DESC")
        List<EmergencyAlert> findPendingSince(@Param("since") LocalDateTime since);

        /**
         * 특정 수신자의 미확인 알림 목록 (실시간 알림용)
         */
        @Query("SELECT ea FROM EmergencyAlert ea " +
                        "JOIN ea.recipients r " +
                        "WHERE r.receiver.id = :userId " +
                        "AND r.isRead = false " +
                        "ORDER BY ea.severity DESC, ea.createdAt DESC")
        List<EmergencyAlert> findUnreadByReceiverId(@Param("userId") Long userId);

        // ========== 중복 체크 ==========

        /**
         * 특정 통화에서 이미 긴급 알림이 생성되었는지 확인
         */
        boolean existsByCallRecordId(Long callRecordId);
}
