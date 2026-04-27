package com.aicc.silverlink.domain.emergency.repository;

import com.aicc.silverlink.domain.emergency.entity.SmsLog;
import com.aicc.silverlink.domain.emergency.entity.SmsLog.MessageType;
import com.aicc.silverlink.domain.emergency.entity.SmsLog.SmsStatus;
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
public interface SmsLogRepository extends JpaRepository<SmsLog, Long> {

    // ========== 기본 조회 ==========

    /**
     * 수신자별 SMS 이력 조회
     */
    Page<SmsLog> findByReceiverIdOrderByCreatedAtDesc(Long userId, Pageable pageable);

    /**
     * 전화번호별 SMS 이력 조회
     */
    Page<SmsLog> findByReceiverPhoneOrderByCreatedAtDesc(String phone, Pageable pageable);

    /**
     * 외부 메시지 ID로 조회 (Webhook 콜백 처리용)
     */
    Optional<SmsLog> findByExternalMsgId(String externalMsgId);

    // ========== 참조 기반 조회 ==========

    /**
     * 특정 참조(긴급 알림, 문의 등)에 대한 SMS 이력
     */
    List<SmsLog> findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc(
            String referenceType,
            Long referenceId);

    /**
     * 특정 긴급 알림에 대한 SMS 이력
     */
    default List<SmsLog> findByEmergencyAlertId(Long alertId) {
        return findByReferenceTypeAndReferenceIdOrderByCreatedAtDesc("emergency_alerts", alertId);
    }

    // ========== 상태별 조회 ==========

    /**
     * 상태별 SMS 목록
     */
    Page<SmsLog> findByStatusOrderByCreatedAtDesc(SmsStatus status, Pageable pageable);

    /**
     * 발송 대기 중인 SMS 목록 (재시도용)
     */
    @Query("SELECT s FROM SmsLog s " +
            "WHERE s.status = 'PENDING' " +
            "AND s.createdAt > :since " +
            "ORDER BY s.createdAt ASC")
    List<SmsLog> findPendingSince(@Param("since") LocalDateTime since);

    /**
     * 발송 실패한 SMS 목록 (재시도용)
     */
    @Query("SELECT s FROM SmsLog s " +
            "WHERE s.status = 'FAILED' " +
            "AND s.createdAt > :since " +
            "ORDER BY s.createdAt ASC")
    List<SmsLog> findFailedSince(@Param("since") LocalDateTime since);

    // ========== 메시지 유형별 조회 ==========

    /**
     * 메시지 유형별 SMS 목록
     */
    Page<SmsLog> findByMessageTypeOrderByCreatedAtDesc(MessageType messageType, Pageable pageable);

    /**
     * 긴급 알림 SMS 목록
     */
    @Query("SELECT s FROM SmsLog s " +
            "WHERE s.messageType IN ('EMERGENCY_CRITICAL', 'EMERGENCY_WARNING') " +
            "ORDER BY s.createdAt DESC")
    Page<SmsLog> findEmergencySmsLogs(Pageable pageable);

    // ========== 통계 ==========

    /**
     * 상태별 SMS 수
     */
    long countByStatus(SmsStatus status);

    /**
     * 메시지 유형별 SMS 수
     */
    long countByMessageType(MessageType messageType);

    /**
     * 기간별 SMS 수
     */
    @Query("SELECT COUNT(s) FROM SmsLog s " +
            "WHERE s.createdAt BETWEEN :startDate AND :endDate")
    long countByDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 기간별 상태별 SMS 수
     */
    @Query("SELECT s.status, COUNT(s) FROM SmsLog s " +
            "WHERE s.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY s.status")
    List<Object[]> countByStatusAndDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    /**
     * 기간별 메시지 유형별 SMS 수
     */
    @Query("SELECT s.messageType, COUNT(s) FROM SmsLog s " +
            "WHERE s.createdAt BETWEEN :startDate AND :endDate " +
            "GROUP BY s.messageType")
    List<Object[]> countByMessageTypeAndDateRange(
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);

    // ========== 중복 방지 ==========

    /**
     * 최근 N분 내 동일 수신자에게 동일 유형 SMS 발송 여부 확인
     */
    @Query("SELECT COUNT(s) > 0 FROM SmsLog s " +
            "WHERE s.receiverPhone = :phone " +
            "AND s.messageType = :messageType " +
            "AND s.referenceId = :referenceId " +
            "AND s.createdAt > :since " +
            "AND s.status != 'FAILED'")
    boolean existsRecentSms(
            @Param("phone") String phone,
            @Param("messageType") MessageType messageType,
            @Param("referenceId") Long referenceId,
            @Param("since") LocalDateTime since);
}
