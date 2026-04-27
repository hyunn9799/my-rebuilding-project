package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallEmotion;
import com.aicc.silverlink.domain.call.entity.EmotionLevel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface CallEmotionRepository extends JpaRepository<CallEmotion, Long> {

    /**
     * 특정 통화의 최신 감정 분석 결과
     */
    @Query("SELECT e FROM CallEmotion e WHERE e.callRecord.id = :callId ORDER BY e.createdAt DESC LIMIT 1")
    Optional<CallEmotion> findLatestByCallId(@Param("callId") Long callId);

    /**
     * 특정 통화의 감정 분석 목록
     */
    List<CallEmotion> findByCallRecordIdOrderByCreatedAtDesc(Long callId);

    /**
     * 특정 어르신의 최근 감정 분석 결과들
     */
    @Query("SELECT e FROM CallEmotion e " +
            "JOIN e.callRecord cr " +
            "WHERE cr.elderly.id = :elderlyId " +
            "ORDER BY e.createdAt DESC")
    List<CallEmotion> findRecentByElderlyId(@Param("elderlyId") Long elderlyId);

    /**
     * 특정 어르신의 기간별 감정 통계
     */
    @Query("SELECT e.emotionLevel, COUNT(e) FROM CallEmotion e " +
            "JOIN e.callRecord cr " +
            "WHERE cr.elderly.id = :elderlyId " +
            "AND e.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY e.emotionLevel")
    List<Object[]> countByElderlyIdAndDateRange(
            @Param("elderlyId") Long elderlyId,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 특정 감정 레벨의 통화 수
     */
    @Query("SELECT COUNT(e) FROM CallEmotion e " +
            "JOIN e.callRecord cr " +
            "WHERE cr.elderly.id = :elderlyId AND e.emotionLevel = :level")
    long countByElderlyIdAndEmotionLevel(
            @Param("elderlyId") Long elderlyId,
            @Param("level") EmotionLevel level);

    @org.springframework.transaction.annotation.Transactional
    void deleteByCallRecord(com.aicc.silverlink.domain.call.entity.CallRecord callRecord);
}