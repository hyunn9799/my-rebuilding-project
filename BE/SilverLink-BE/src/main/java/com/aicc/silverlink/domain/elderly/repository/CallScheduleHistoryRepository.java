package com.aicc.silverlink.domain.elderly.repository;

import com.aicc.silverlink.domain.elderly.entity.CallScheduleHistory;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * 통화 스케줄 변경 이력 Repository
 */
public interface CallScheduleHistoryRepository extends JpaRepository<CallScheduleHistory, Long> {

    /**
     * 특정 어르신의 변경 이력 조회 (최신순)
     */
    @Query("SELECT h FROM CallScheduleHistory h " +
            "JOIN FETCH h.elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH h.changedBy " +
            "WHERE h.elderly.id = :elderlyId " +
            "ORDER BY h.createdAt DESC")
    List<CallScheduleHistory> findByElderlyIdOrderByCreatedAtDesc(@Param("elderlyId") Long elderlyId);

    /**
     * 특정 어르신의 변경 이력 조회 (페이징)
     */
    @Query("SELECT h FROM CallScheduleHistory h " +
            "JOIN FETCH h.elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH h.changedBy " +
            "WHERE h.elderly.id = :elderlyId")
    Page<CallScheduleHistory> findByElderlyId(@Param("elderlyId") Long elderlyId, Pageable pageable);

    /**
     * 상담사가 담당하는 어르신들의 변경 이력 조회
     * - Assignment 테이블을 통해 담당 어르신 확인
     */
    @Query("SELECT h FROM CallScheduleHistory h " +
            "JOIN FETCH h.elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH h.changedBy " +
            "WHERE e.id IN (" +
            "    SELECT a.elderly.id FROM Assignment a " +
            "    WHERE a.counselor.id = :counselorId AND a.status = 'ACTIVE'" +
            ") " +
            "ORDER BY h.createdAt DESC")
    List<CallScheduleHistory> findByCounselorAssigned(@Param("counselorId") Long counselorId);

    /**
     * 상담사가 담당하는 어르신들의 변경 이력 조회 (페이징)
     */
    @Query("SELECT h FROM CallScheduleHistory h " +
            "JOIN FETCH h.elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH h.changedBy " +
            "WHERE e.id IN (" +
            "    SELECT a.elderly.id FROM Assignment a " +
            "    WHERE a.counselor.id = :counselorId AND a.status = 'ACTIVE'" +
            ")")
    Page<CallScheduleHistory> findByCounselorAssigned(@Param("counselorId") Long counselorId, Pageable pageable);

    /**
     * 전체 변경 이력 조회 (관리자용, 최신순)
     */
    @Query("SELECT h FROM CallScheduleHistory h " +
            "JOIN FETCH h.elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH h.changedBy " +
            "ORDER BY h.createdAt DESC")
    List<CallScheduleHistory> findAllWithDetails();

    /**
     * 전체 변경 이력 조회 (관리자용, 페이징)
     */
    @Query("SELECT h FROM CallScheduleHistory h " +
            "JOIN FETCH h.elderly e " +
            "JOIN FETCH e.user " +
            "JOIN FETCH h.changedBy")
    Page<CallScheduleHistory> findAllWithDetails(Pageable pageable);
}
