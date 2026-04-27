package com.aicc.silverlink.domain.consent.repository;

import com.aicc.silverlink.domain.consent.entity.AccessRequest;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessRequestStatus;
import com.aicc.silverlink.domain.consent.entity.AccessRequest.AccessScope;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AccessRequestRepository extends JpaRepository<AccessRequest, Long> {

    /**
     * 특정 요청자의 특정 어르신에 대한 특정 범위 접근 요청 조회
     * (중복 요청 방지용)
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "WHERE ar.requester.id = :requesterId " +
            "AND ar.elderly.id = :elderlyId " +
            "AND ar.scope = :scope " +
            "AND ar.status IN ('PENDING', 'APPROVED')")
    Optional<AccessRequest> findActiveRequest(
            @Param("requesterId") Long requesterId,
            @Param("elderlyId") Long elderlyId,
            @Param("scope") AccessScope scope
    );

    /**
     * 특정 요청자가 특정 어르신에 대해 특정 범위의 승인된 접근 권한이 있는지 확인
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "WHERE ar.requester.id = :requesterId " +
            "AND ar.elderly.id = :elderlyId " +
            "AND ar.scope = :scope " +
            "AND ar.status = 'APPROVED' " +
            "AND (ar.expiresAt IS NULL OR ar.expiresAt > :now)")
    Optional<AccessRequest> findValidAccess(
            @Param("requesterId") Long requesterId,
            @Param("elderlyId") Long elderlyId,
            @Param("scope") AccessScope scope,
            @Param("now") LocalDateTime now
    );

    /**
     * 특정 요청자의 모든 접근 요청 조회
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "JOIN FETCH ar.elderly e " +
            "JOIN FETCH e.user " +
            "WHERE ar.requester.id = :requesterId " +
            "ORDER BY ar.requestedAt DESC")
    List<AccessRequest> findByRequesterId(@Param("requesterId") Long requesterId);

    /**
     * 특정 어르신에 대한 모든 접근 요청 조회
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "JOIN FETCH ar.requester " +
            "WHERE ar.elderly.id = :elderlyId " +
            "ORDER BY ar.requestedAt DESC")
    List<AccessRequest> findByElderlyId(@Param("elderlyId") Long elderlyId);

    /**
     * 대기 중인 요청 목록 조회 (관리자용)
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "JOIN FETCH ar.requester " +
            "JOIN FETCH ar.elderly e " +
            "JOIN FETCH e.user " +
            "WHERE ar.status = 'PENDING' " +
            "ORDER BY ar.requestedAt ASC")
    List<AccessRequest> findPendingRequests();

    /**
     * 대기 중인 요청 목록 조회 - 페이징 (관리자용)
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "JOIN FETCH ar.requester " +
            "JOIN FETCH ar.elderly e " +
            "JOIN FETCH e.user " +
            "WHERE ar.status = 'PENDING'")
    Page<AccessRequest> findPendingRequestsWithPaging(Pageable pageable);

    /**
     * 서류 확인 완료되었지만 아직 대기 중인 요청 목록
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "JOIN FETCH ar.requester " +
            "JOIN FETCH ar.elderly e " +
            "JOIN FETCH e.user " +
            "WHERE ar.status = 'PENDING' " +
            "AND ar.documentVerified = true " +
            "ORDER BY ar.requestedAt ASC")
    List<AccessRequest> findVerifiedPendingRequests();

    /**
     * 만료된 승인 건 조회 (배치 처리용)
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "WHERE ar.status = 'APPROVED' " +
            "AND ar.expiresAt IS NOT NULL " +
            "AND ar.expiresAt < :now")
    List<AccessRequest> findExpiredApprovals(@Param("now") LocalDateTime now);

    /**
     * 만료된 승인 건 일괄 상태 변경
     */
    @Modifying
    @Query("UPDATE AccessRequest ar " +
            "SET ar.status = 'EXPIRED' " +
            "WHERE ar.status = 'APPROVED' " +
            "AND ar.expiresAt IS NOT NULL " +
            "AND ar.expiresAt < :now")
    int expireOldApprovals(@Param("now") LocalDateTime now);

    /**
     * 특정 상태의 요청 목록 조회
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "JOIN FETCH ar.requester " +
            "JOIN FETCH ar.elderly e " +
            "JOIN FETCH e.user " +
            "WHERE ar.status = :status " +
            "ORDER BY ar.requestedAt DESC")
    List<AccessRequest> findByStatus(@Param("status") AccessRequestStatus status);

    /**
     * 요청 상세 조회 (연관 엔티티 모두 fetch)
     */
    @Query("SELECT ar FROM AccessRequest ar " +
            "JOIN FETCH ar.requester " +
            "JOIN FETCH ar.elderly e " +
            "JOIN FETCH e.user " +
            "LEFT JOIN FETCH ar.reviewedByAdmin " +
            "WHERE ar.id = :id")
    Optional<AccessRequest> findByIdWithDetails(@Param("id") Long id);

    /**
     * 특정 요청자가 특정 어르신에 대해 유효한 접근 권한이 있는지 확인 (boolean)
     */
    @Query("SELECT COUNT(ar) > 0 FROM AccessRequest ar " +
            "WHERE ar.requester.id = :requesterId " +
            "AND ar.elderly.id = :elderlyId " +
            "AND ar.scope = :scope " +
            "AND ar.status = 'APPROVED' " +
            "AND (ar.expiresAt IS NULL OR ar.expiresAt > :now)")
    boolean hasValidAccess(
            @Param("requesterId") Long requesterId,
            @Param("elderlyId") Long elderlyId,
            @Param("scope") AccessScope scope,
            @Param("now") LocalDateTime now
    );
}