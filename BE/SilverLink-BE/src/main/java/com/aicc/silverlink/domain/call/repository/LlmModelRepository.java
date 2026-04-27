package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.LlmModel;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface LlmModelRepository extends JpaRepository<LlmModel, Long> {

    /**
     * 특정 통화의 LLM 발화 목록 조회 (시간순)
     */
    @Query("SELECT m FROM LlmModel m WHERE m.callRecord.id = :callId ORDER BY m.createdAt ASC")
    List<LlmModel> findByCallIdOrderByCreatedAtAsc(@Param("callId") Long callId);

    /**
     * 특정 통화의 LLM 발화 개수
     */
    long countByCallRecordId(Long callId);

    /**
     * 특정 통화의 가장 최근 LLM 발화 조회
     */
    /**
     * 특정 통화의 가장 최근 LLM 발화 조회 (ID 기준 내림차순)
     */
    java.util.Optional<LlmModel> findTopByCallRecordOrderByIdDesc(
            com.aicc.silverlink.domain.call.entity.CallRecord callRecord);

    /**
     * 특정 통화의 가장 최근 LLM 발화 조회 (시간순)
     */
    java.util.Optional<LlmModel> findFirstByCallRecordOrderByCreatedAtDesc(
            com.aicc.silverlink.domain.call.entity.CallRecord callRecord);
}