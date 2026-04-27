package com.aicc.silverlink.domain.notification.repository;

import com.aicc.silverlink.domain.notification.entity.Notification;
import com.aicc.silverlink.domain.notification.entity.Notification.NotificationType;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 일반 알림 Repository
 */
@Repository
public interface NotificationRepository extends JpaRepository<Notification, Long> {

        // ========== 사용자별 알림 조회 ==========

        /**
         * 사용자별 알림 목록 조회 (최신순)
         */
        @Query("SELECT n FROM Notification n " +
                        "WHERE n.receiver.id = :userId " +
                        "ORDER BY n.createdAt DESC")
        Page<Notification> findByReceiverId(@Param("userId") Long userId, Pageable pageable);

        /**
         * 사용자별 미확인 알림 목록 조회
         */
        @Query("SELECT n FROM Notification n " +
                        "WHERE n.receiver.id = :userId AND n.isRead = false " +
                        "ORDER BY n.createdAt DESC")
        List<Notification> findUnreadByReceiverId(@Param("userId") Long userId);

        /**
         * 사용자별 미확인 알림 수
         */
        @Query("SELECT COUNT(n) FROM Notification n " +
                        "WHERE n.receiver.id = :userId AND n.isRead = false")
        long countUnreadByReceiverId(@Param("userId") Long userId);

        /**
         * 사용자별 알림 유형별 조회
         */
        @Query("SELECT n FROM Notification n " +
                        "WHERE n.receiver.id = :userId AND n.notificationType = :type " +
                        "ORDER BY n.createdAt DESC")
        Page<Notification> findByReceiverIdAndType(
                        @Param("userId") Long userId,
                        @Param("type") NotificationType type,
                        Pageable pageable);

        // ========== 참조 기반 조회 ==========

        /**
         * 참조 테이블 기준 알림 조회
         */
        @Query("SELECT n FROM Notification n " +
                        "WHERE n.referenceType = :refType AND n.referenceId = :refId")
        List<Notification> findByReference(
                        @Param("refType") String referenceType,
                        @Param("refId") Long referenceId);

        /**
         * 특정 문의에 대한 알림 조회
         */
        default List<Notification> findByInquiryId(Long inquiryId) {
                return findByReference("inquiries", inquiryId);
        }

        /**
         * 특정 민원에 대한 알림 조회
         */
        default List<Notification> findByComplaintId(Long complaintId) {
                return findByReference("complaints", complaintId);
        }

        /**
         * 특정 접근권한 요청에 대한 알림 조회
         */
        default List<Notification> findByAccessRequestId(Long accessRequestId) {
                return findByReference("access_requests", accessRequestId);
        }

        // ========== 일괄 읽음 처리 ==========

        /**
         * 사용자의 모든 알림 읽음 처리
         */
        @Modifying
        @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
                        "WHERE n.receiver.id = :userId AND n.isRead = false")
        int markAllAsReadByReceiverId(@Param("userId") Long userId, @Param("now") LocalDateTime now);

        /**
         * 특정 유형의 알림 일괄 읽음 처리
         */
        @Modifying
        @Query("UPDATE Notification n SET n.isRead = true, n.readAt = :now " +
                        "WHERE n.receiver.id = :userId AND n.notificationType = :type AND n.isRead = false")
        int markAllAsReadByType(
                        @Param("userId") Long userId,
                        @Param("type") NotificationType type,
                        @Param("now") LocalDateTime now);

        // ========== 알림 삭제 ==========

        /**
         * 읽은 알림 중 N일 경과한 알림 삭제 (배치용)
         */
        @Modifying
        @Query("DELETE FROM Notification n " +
                        "WHERE n.isRead = true AND n.readAt < :threshold")
        int deleteOldReadNotifications(@Param("threshold") LocalDateTime threshold);

        /**
         * 사용자별 알림 전체 삭제
         */
        @Modifying
        @Query("DELETE FROM Notification n WHERE n.receiver.id = :userId")
        int deleteAllByReceiverId(@Param("userId") Long userId);

        // ========== 통계 ==========

        /**
         * 알림 유형별 개수 (사용자별)
         */
        @Query("SELECT n.notificationType, COUNT(n) FROM Notification n " +
                        "WHERE n.receiver.id = :userId " +
                        "GROUP BY n.notificationType")
        List<Object[]> countByTypeForUser(@Param("userId") Long userId);

        /**
         * 기간별 알림 수
         */
        @Query("SELECT COUNT(n) FROM Notification n " +
                        "WHERE n.createdAt BETWEEN :start AND :end")
        long countByDateRange(@Param("start") LocalDateTime start, @Param("end") LocalDateTime end);

        /**
         * 미발송 SMS 알림 조회 (재시도용)
         */
        @Query("SELECT n FROM Notification n " +
                        "WHERE n.smsSent = false " +
                        "AND n.notificationType IN :smsTypes " +
                        "AND n.createdAt > :since")
        List<Notification> findPendingSmsNotifications(
                        @Param("smsTypes") List<NotificationType> smsRequiredTypes,
                        @Param("since") LocalDateTime since);

        // ========== 최근 알림 조회 (실시간 표시용) ==========

        /**
         * 최근 N개 알림 조회
         */
        @Query("SELECT n FROM Notification n " +
                        "WHERE n.receiver.id = :userId " +
                        "ORDER BY n.createdAt DESC")
        List<Notification> findRecentByReceiverId(@Param("userId") Long userId, Pageable pageable);

        /**
         * 특정 시간 이후 생성된 알림 조회 (폴링용)
         */
        @Query("SELECT n FROM Notification n " +
                        "WHERE n.receiver.id = :userId AND n.createdAt > :since " +
                        "ORDER BY n.createdAt DESC")
        List<Notification> findNewNotificationsSince(
                        @Param("userId") Long userId,
                        @Param("since") LocalDateTime since);
}
