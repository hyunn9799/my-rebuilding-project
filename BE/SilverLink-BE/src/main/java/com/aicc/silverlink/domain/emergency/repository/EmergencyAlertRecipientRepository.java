package com.aicc.silverlink.domain.emergency.repository;

import com.aicc.silverlink.domain.emergency.entity.EmergencyAlertRecipient;
import com.aicc.silverlink.domain.emergency.entity.EmergencyAlertRecipient.ReceiverRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface EmergencyAlertRecipientRepository extends JpaRepository<EmergencyAlertRecipient, Long> {

    // ========== 기본 조회 ==========

    /**
     * 특정 알림의 모든 수신자 조회
     */
    List<EmergencyAlertRecipient> findByEmergencyAlertId(Long alertId);

    /**
     * 특정 알림의 특정 수신자 조회
     */
    Optional<EmergencyAlertRecipient> findByEmergencyAlertIdAndReceiverId(Long alertId, Long receiverId);

    /**
     * 특정 사용자의 수신 알림 목록 (미확인 우선, 최신순)
     */
    @Query("SELECT r FROM EmergencyAlertRecipient r " +
            "JOIN FETCH r.emergencyAlert ea " +
            "JOIN FETCH ea.elderly e " +
            "JOIN FETCH e.user " +
            "WHERE r.receiver.id = :userId " +
            "ORDER BY r.isRead ASC, ea.createdAt DESC")
    List<EmergencyAlertRecipient> findByReceiverIdWithAlert(@Param("userId") Long userId);

    /**
     * 특정 사용자의 미확인 알림 목록
     */
    @Query("SELECT r FROM EmergencyAlertRecipient r " +
            "JOIN FETCH r.emergencyAlert ea " +
            "WHERE r.receiver.id = :userId " +
            "AND r.isRead = false " +
            "ORDER BY ea.severity DESC, ea.createdAt DESC")
    List<EmergencyAlertRecipient> findUnreadByReceiverId(@Param("userId") Long userId);

    /**
     * 특정 사용자의 미확인 알림 수
     */
    long countByReceiverIdAndIsReadFalse(Long userId);

    // ========== 역할별 조회 ==========

    /**
     * 특정 알림의 역할별 수신자 조회
     */
    List<EmergencyAlertRecipient> findByEmergencyAlertIdAndReceiverRole(Long alertId, ReceiverRole role);

    /**
     * 특정 역할의 미확인 알림 수
     */
    long countByReceiverRoleAndIsReadFalse(ReceiverRole role);

    // ========== SMS 관련 ==========

    /**
     * SMS 발송이 필요한 수신자 목록
     */
    @Query("SELECT r FROM EmergencyAlertRecipient r " +
            "JOIN FETCH r.receiver u " +
            "WHERE r.emergencyAlert.id = :alertId " +
            "AND r.smsRequired = true " +
            "AND r.smsSent = false")
    List<EmergencyAlertRecipient> findPendingSmsRecipients(@Param("alertId") Long alertId);

    /**
     * SMS 발송 실패한 수신자 목록 (재시도용)
     */
    @Query("SELECT r FROM EmergencyAlertRecipient r " +
            "JOIN FETCH r.receiver u " +
            "WHERE r.smsRequired = true " +
            "AND r.smsDeliveryStatus = 'FAILED'")
    List<EmergencyAlertRecipient> findFailedSmsRecipients();

    // ========== 일괄 업데이트 ==========

    /**
     * 특정 알림의 모든 수신자 읽음 처리
     */
    @Modifying
    @Query("UPDATE EmergencyAlertRecipient r " +
            "SET r.isRead = true, r.readAt = CURRENT_TIMESTAMP " +
            "WHERE r.emergencyAlert.id = :alertId " +
            "AND r.isRead = false")
    int markAllAsReadByAlertId(@Param("alertId") Long alertId);

    /**
     * 특정 사용자의 모든 알림 읽음 처리
     */
    @Modifying
    @Query("UPDATE EmergencyAlertRecipient r " +
            "SET r.isRead = true, r.readAt = CURRENT_TIMESTAMP " +
            "WHERE r.receiver.id = :userId " +
            "AND r.isRead = false")
    int markAllAsReadByReceiverId(@Param("userId") Long userId);

    // ========== 존재 여부 확인 ==========

    /**
     * 특정 알림에 특정 수신자가 있는지 확인
     */
    boolean existsByEmergencyAlertIdAndReceiverId(Long alertId, Long receiverId);

    /**
     * 특정 사용자가 미확인 알림이 있는지 확인
     */
    boolean existsByReceiverIdAndIsReadFalse(Long userId);
}
