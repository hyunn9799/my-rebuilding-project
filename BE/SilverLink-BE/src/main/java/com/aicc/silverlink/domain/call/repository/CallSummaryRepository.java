package com.aicc.silverlink.domain.call.repository;

import com.aicc.silverlink.domain.call.entity.CallRecord;
import com.aicc.silverlink.domain.call.entity.CallSummary;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

public interface CallSummaryRepository extends JpaRepository<CallSummary, Long> {
    
    List<CallSummary> findByCallRecordId(Long callId);

    /**
     * 특정 통화의 최신 요약 조회
     */
    @Query("SELECT s FROM CallSummary s WHERE s.callRecord.id = :callId ORDER BY s.createdAt DESC LIMIT 1")
    Optional<CallSummary> findLatestByCallId(@Param("callId") Long callId);

    @Transactional
    void deleteByCallRecord(CallRecord callRecord);
}