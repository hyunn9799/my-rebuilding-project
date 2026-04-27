package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallRecord;
import com.aicc.silverlink.domain.call.entity.CallState;
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
public interface CallRecordRepository extends JpaRepository<CallRecord, Long> {

        /**
         * 어르신 ID로 통화 기록 조회 (최신순)
         */
        Page<CallRecord> findByElderlyIdOrderByCallAtDesc(Long elderlyId, Pageable pageable);

        /**
         * 어르신 ID와 기간으로 통화 기록 조회
         */
        @Query("SELECT c FROM CallRecord c WHERE c.elderly.id = :elderlyId " +
                        "AND c.callAt BETWEEN :startDate AND :endDate ORDER BY c.callAt DESC")
        List<CallRecord> findByElderlyIdAndDateRange(
                        @Param("elderlyId") Long elderlyId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * 특정 상담사가 담당하는 어르신들의 통화 기록 조회 (미확인 건 우선)
         */
        @Query("SELECT DISTINCT c FROM CallRecord c " +
                        "JOIN Assignment a ON c.elderly.id = a.elderly.id " +
                        "LEFT JOIN CounselorCallReview r ON c.id = r.callRecord.id AND r.counselor.id = :counselorId " +
                        "WHERE a.counselor.id = :counselorId AND a.status = 'ACTIVE' " +
                        "ORDER BY c.callAt DESC")
        Page<CallRecord> findCallRecordsForCounselor(@Param("counselorId") Long counselorId, Pageable pageable);

        /**
         * 상담사가 확인하지 않은 통화 기록 개수
         */
        @Query("SELECT COUNT(c) FROM CallRecord c " +
                        "JOIN Assignment a ON c.elderly.id = a.elderly.id " +
                        "LEFT JOIN CounselorCallReview r ON c.id = r.callRecord.id AND r.counselor.id = :counselorId " +
                        "WHERE a.counselor.id = :counselorId AND a.status = 'ACTIVE' AND r.id IS NULL " +
                        "AND c.state = 'COMPLETED'")
        long countUnreviewedCallsForCounselor(@Param("counselorId") Long counselorId);

        /**
         * 위험 응답이 있는 통화 기록 조회
         */
        @Query("SELECT DISTINCT c FROM CallRecord c " +
                        "JOIN c.elderlyResponses er " +
                        "WHERE er.danger = true ORDER BY c.callAt DESC")
        Page<CallRecord> findCallsWithDangerResponse(Pageable pageable);

        /**
         * 통화 기록과 연관된 기본 데이터를 조회 (Fetch Join)
         * 참고: List 타입 컬렉션은 1개만 fetch join 가능 (MultipleBagFetchException 방지)
         * emotions, summaries, llmModels, elderlyResponses는 Service에서 별도 조회
         */
        @Query("SELECT c FROM CallRecord c " +
                        "LEFT JOIN FETCH c.elderly e " +
                        "LEFT JOIN FETCH e.user " +
                        "WHERE c.id = :callId")
        Optional<CallRecord> findByIdWithDetails(@Param("callId") Long callId);

        /**
         * 특정 기간 내 완료된 통화 수
         */
        long countByStateAndCallAtBetween(CallState state, LocalDateTime start, LocalDateTime end);

        /**
         * 어르신 ID로 완료된 통화 기록 조회 (보호자 통화 목록용)
         * 리뷰 여부와 관계없이 COMPLETED 상태인 모든 통화를 반환
         */
        @Query("SELECT c FROM CallRecord c " +
                        "LEFT JOIN FETCH c.elderly e " +
                        "LEFT JOIN FETCH e.user " +
                        "WHERE c.elderly.id = :elderlyId AND c.state = 'COMPLETED' " +
                        "ORDER BY c.callAt DESC")
        Page<CallRecord> findCompletedByElderlyId(@Param("elderlyId") Long elderlyId, Pageable pageable);

        /**
         * 어르신 ID로 모든 통화 기록 조회 (보호자 통화 목록용 - 진행중 포함)
         * 리뷰 여부와 관계없이 모든 상태의 통화를 반환 (ANSWERED, COMPLETED, FAILED)
         */
        @Query("SELECT c FROM CallRecord c " +
                        "LEFT JOIN FETCH c.elderly e " +
                        "LEFT JOIN FETCH e.user " +
                        "WHERE c.elderly.id = :elderlyId " +
                        "ORDER BY c.callAt DESC")
        Page<CallRecord> findAllByElderlyId(@Param("elderlyId") Long elderlyId, Pageable pageable);

        /**
         * 상담사의 기간별 통화 수 조회 (오늘 통화 수 계산용)
         */
        @Query("SELECT COUNT(c) FROM CallRecord c " +
                        "JOIN Assignment a ON c.elderly.id = a.elderly.id " +
                        "WHERE a.counselor.id = :counselorId AND a.status = 'ACTIVE' " +
                        "AND c.callAt BETWEEN :startDate AND :endDate")
        long countCallsForCounselorByDateRange(
                        @Param("counselorId") Long counselorId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);
}