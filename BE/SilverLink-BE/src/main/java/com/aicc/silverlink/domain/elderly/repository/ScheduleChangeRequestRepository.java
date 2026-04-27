package com.aicc.silverlink.domain.elderly.repository;

import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest;
import com.aicc.silverlink.domain.elderly.entity.ScheduleChangeRequest.RequestStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ScheduleChangeRequestRepository extends JpaRepository<ScheduleChangeRequest, Long> {

    /**
     * 특정 어르신의 변경 요청 목록 (최신순)
     */
    List<ScheduleChangeRequest> findByElderlyIdOrderByCreatedAtDesc(Long elderlyId);

    /**
     * 특정 상태의 변경 요청 목록 (최신순)
     */
    List<ScheduleChangeRequest> findByStatusOrderByCreatedAtDesc(RequestStatus status);

    /**
     * 대기중인 변경 요청 목록 (상담사용)
     */
    @Query("SELECT r FROM ScheduleChangeRequest r " +
            "JOIN FETCH r.elderly e " +
            "JOIN FETCH e.user u " +
            "WHERE r.status = :status " +
            "ORDER BY r.createdAt DESC")
    List<ScheduleChangeRequest> findPendingRequestsWithElderly(@Param("status") RequestStatus status);

    /**
     * 특정 어르신의 대기중인 요청이 있는지 확인
     */
    boolean existsByElderlyIdAndStatus(Long elderlyId, RequestStatus status);
}
