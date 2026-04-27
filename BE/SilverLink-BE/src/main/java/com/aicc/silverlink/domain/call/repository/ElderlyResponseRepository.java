package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.ElderlyResponse;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface ElderlyResponseRepository extends JpaRepository<ElderlyResponse, Long> {

    /**
     * 특정 통화의 어르신 응답 목록 (시간순)
     */
    @Query("SELECT r FROM ElderlyResponse r WHERE r.callRecord.id = :callId ORDER BY r.respondedAt ASC")
    List<ElderlyResponse> findByCallRecordIdOrderByRespondedAtAsc(@Param("callId") Long callId);

    /**
     * 특정 통화의 위험 응답만 조회
     */
    @Query("SELECT r FROM ElderlyResponse r WHERE r.callRecord.id = :callId AND r.danger = true ORDER BY r.respondedAt ASC")
    List<ElderlyResponse> findDangerResponsesByCallId(@Param("callId") Long callId);

    /**
     * 특정 어르신의 최근 위험 응답들
     */
    @Query("SELECT r FROM ElderlyResponse r " +
            "JOIN r.callRecord cr " +
            "WHERE cr.elderly.id = :elderlyId AND r.danger = true " +
            "ORDER BY r.respondedAt DESC")
    List<ElderlyResponse> findRecentDangerResponsesByElderlyId(@Param("elderlyId") Long elderlyId);

    /**
     * 특정 통화의 응답 개수
     */
    long countByCallRecordId(Long callId);

    /**
     * 특정 통화의 위험 응답 개수
     */
    @Query("SELECT COUNT(r) FROM ElderlyResponse r WHERE r.callRecord.id = :callId AND r.danger = true")
    long countDangerByCallId(@Param("callId") Long callId);
}