package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallDailyStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CallDailyStatusRepository extends JpaRepository<CallDailyStatus, Long> {

        /**
         * 특정 통화의 일일 상태 조회
         */
        Optional<CallDailyStatus> findByCallRecordId(Long callId);

        /**
         * 특정 어르신의 최근 일일 상태 목록
         */
        @Query("SELECT ds FROM CallDailyStatus ds " +
                        "JOIN ds.callRecord cr " +
                        "WHERE cr.elderly.id = :elderlyId " +
                        "ORDER BY cr.callAt DESC")
        List<CallDailyStatus> findRecentByElderlyId(@Param("elderlyId") Long elderlyId);

        /**
         * 특정 어르신의 기간별 식사 통계
         */
        @Query("SELECT COUNT(ds) FROM CallDailyStatus ds " +
                        "JOIN ds.callRecord cr " +
                        "WHERE cr.elderly.id = :elderlyId " +
                        "AND ds.mealTaken = true " +
                        "AND cr.callAt BETWEEN :startDate AND :endDate")
        long countMealTakenByElderlyIdAndDateRange(
                        @Param("elderlyId") Long elderlyId,
                        @Param("startDate") LocalDateTime startDate,
                        @Param("endDate") LocalDateTime endDate);

        /**
         * 특정 통화의 일일 상태 삭제
         */
        @Transactional
        void deleteByCallRecord(com.aicc.silverlink.domain.call.entity.CallRecord callRecord);
}