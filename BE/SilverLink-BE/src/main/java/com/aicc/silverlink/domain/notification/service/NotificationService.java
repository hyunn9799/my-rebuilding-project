package com.aicc.silverlink.domain.notification.service;

import com.aicc.silverlink.domain.notification.dto.NotificationDto.*;
import com.aicc.silverlink.domain.notification.entity.Notification;
import com.aicc.silverlink.domain.notification.entity.Notification.NotificationType;
import com.aicc.silverlink.domain.notification.repository.NotificationRepository;
import com.aicc.silverlink.domain.user.entity.Role;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * 일반 알림 서비스
 */
@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final UnifiedSseService unifiedSseService; // 통합 SSE 서비스
    private final NotificationSmsService notificationSmsService; // 알림 SMS 서비스

    // ========== 알림 생성 ==========

    /**
     * 상담사 코멘트 알림 생성 및 발송
     */
    @Transactional
    public Notification createCounselorCommentNotification(Long receiverUserId, Long callId, String elderlyName) {
        User receiver = findUserById(receiverUserId);

        Notification notification = Notification.createCounselorCommentNotification(receiver, callId, elderlyName);
        Notification saved = notificationRepository.save(notification);

        sendRealtimeNotification(receiverUserId, saved);

        log.info("[NotificationService] 상담사 코멘트 알림 생성. receiverId={}, callId={}", receiverUserId, callId);
        return saved;
    }

    /**
     * 문의 답변 알림 생성 및 발송
     */
    @Transactional
    public Notification createInquiryReplyNotification(Long receiverUserId, Long inquiryId, String inquiryTitle) {
        User receiver = findUserById(receiverUserId);

        Notification notification = Notification.createInquiryReplyNotification(receiver, inquiryId, inquiryTitle);
        Notification saved = notificationRepository.save(notification);

        // SSE 실시간 전송
        sendRealtimeNotification(receiverUserId, saved);

        // SMS 발송 (보호자에게만)
        if (notification.getNotificationType().isSmsRequired()) {
            notificationSmsService.sendInquiryReplySmsAsync(receiver, inquiryId);
        }

        log.info("[NotificationService] 문의 답변 알림 생성. receiverId={}, inquiryId={}", receiverUserId, inquiryId);
        return saved;
    }

    /**
     * 민원 답변 알림 생성 및 발송
     */
    @Transactional
    public Notification createComplaintReplyNotification(Long receiverUserId, Long complaintId, String complaintTitle) {
        User receiver = findUserById(receiverUserId);

        Notification notification = Notification.createComplaintReplyNotification(receiver, complaintId,
                complaintTitle);
        Notification saved = notificationRepository.save(notification);

        sendRealtimeNotification(receiverUserId, saved);

        if (notification.getNotificationType().isSmsRequired()) {
            notificationSmsService.sendComplaintReplySmsAsync(receiver, complaintId);
        }

        log.info("[NotificationService] 민원 답변 알림 생성. receiverId={}, complaintId={}", receiverUserId, complaintId);
        return saved;
    }

    /**
     * 접근권한 승인 알림 생성 및 발송
     */
    @Transactional
    public Notification createAccessApprovedNotification(Long receiverUserId, Long requestId, String elderlyName) {
        User receiver = findUserById(receiverUserId);

        Notification notification = Notification.createAccessApprovedNotification(receiver, requestId, elderlyName);
        Notification saved = notificationRepository.save(notification);

        sendRealtimeNotification(receiverUserId, saved);

        if (notification.getNotificationType().isSmsRequired()) {
            notificationSmsService.sendAccessApprovedSmsAsync(receiver, requestId, elderlyName);
        }

        log.info("[NotificationService] 접근권한 승인 알림 생성. receiverId={}, requestId={}", receiverUserId, requestId);
        return saved;
    }

    /**
     * 접근권한 거절 알림 생성 및 발송
     */
    @Transactional
    public Notification createAccessRejectedNotification(Long receiverUserId, Long requestId, String elderlyName,
            String reason) {
        User receiver = findUserById(receiverUserId);

        Notification notification = Notification.createAccessRejectedNotification(receiver, requestId, elderlyName,
                reason);
        Notification saved = notificationRepository.save(notification);

        sendRealtimeNotification(receiverUserId, saved);

        if (notification.getNotificationType().isSmsRequired()) {
            notificationSmsService.sendAccessRejectedSmsAsync(receiver, requestId, elderlyName, reason);
        }

        log.info("[NotificationService] 접근권한 거절 알림 생성. receiverId={}, requestId={}", receiverUserId, requestId);
        return saved;
    }

    /**
     * 새 접근권한 요청 알림 (관리자에게)
     */
    @Transactional
    public Notification createAccessRequestNotification(Long adminUserId, Long requestId, String requesterName,
            String elderlyName) {
        User receiver = findUserById(adminUserId);

        Notification notification = Notification.createAccessRequestNotification(receiver, requestId, requesterName,
                elderlyName);
        Notification saved = notificationRepository.save(notification);

        sendRealtimeNotification(adminUserId, saved);

        log.info("[NotificationService] 접근권한 요청 알림 생성. adminId={}, requestId={}", adminUserId, requestId);
        return saved;
    }

    /**
     * 담당 배정 알림 (상담사에게)
     */
    @Transactional
    public Notification createAssignmentNotification(Long counselorUserId, Long assignmentId, String elderlyName) {
        User receiver = findUserById(counselorUserId);

        Notification notification = Notification.createAssignmentNotification(receiver, assignmentId, elderlyName);
        Notification saved = notificationRepository.save(notification);

        sendRealtimeNotification(counselorUserId, saved);

        log.info("[NotificationService] 담당 배정 알림 생성. counselorId={}, assignmentId={}", counselorUserId, assignmentId);
        return saved;
    }

    /**
     * 공지사항 알림 (대상자 전체)
     */
    @Transactional
    public List<Notification> createNoticeNotifications(Long noticeId, String noticeTitle, List<Long> receiverUserIds) {
        // [수정] 공지사항 알림 비활성화 요청으로 인한 로직 주석 처리 (팝업만 유지)
        /*
         * List<Notification> notifications = new ArrayList<>();
         * 
         * for (Long userId : receiverUserIds) {
         * try {
         * User receiver = findUserById(userId);
         * Notification notification = Notification.createNoticeNotification(receiver,
         * noticeId, noticeTitle);
         * Notification saved = notificationRepository.save(notification);
         * notifications.add(saved);
         * 
         * sendRealtimeNotification(userId, saved);
         * } catch (Exception e) {
         * log.warn("[NotificationService] 공지 알림 생성 실패. userId={}, noticeId={}", userId,
         * noticeId, e);
         * }
         * }
         * 
         * log.info("[NotificationService] 공지사항 알림 생성 완료. noticeId={}, 발송 수={}",
         * noticeId, notifications.size());
         * return notifications;
         */
        log.info("[NotificationService] 공지사항 알림 생성 건너뜀 (비활성화됨). noticeId={}", noticeId);
        return java.util.Collections.emptyList();
    }

    /**
     * 새 문의 알림 (관리자에게)
     */
    @Transactional
    public Notification createInquiryNewNotification(Long adminUserId, Long inquiryId, String writerName,
            String inquiryTitle) {
        User receiver = findUserById(adminUserId);

        Notification notification = Notification.createInquiryNewNotification(receiver, inquiryId, writerName,
                inquiryTitle);
        Notification saved = notificationRepository.save(notification);

        sendRealtimeNotification(adminUserId, saved);

        log.info("[NotificationService] 새 문의 알림 생성. adminId={}, inquiryId={}", adminUserId, inquiryId);
        return saved;
    }

    /**
     * 새 민원 알림 (관리자에게)
     */
    @Transactional
    public Notification createComplaintNewNotification(Long adminUserId, Long complaintId, String writerName,
            String complaintTitle) {
        User receiver = findUserById(adminUserId);

        Notification notification = Notification.createComplaintNewNotification(receiver, complaintId, writerName,
                complaintTitle);
        Notification saved = notificationRepository.save(notification);

        sendRealtimeNotification(adminUserId, saved);

        log.info("[NotificationService] 새 민원 알림 생성. adminId={}, complaintId={}", adminUserId, complaintId);
        return saved;
    }

    /**
     * 긴급 알림 (수신자용)
     */
    @Transactional
    public Notification createEmergencyNewNotification(Long receiverUserId, Long alertId, String elderlyName,
            String severityDescription, String linkUrl) {
        User receiver = findUserById(receiverUserId);

        // linkUrl이 null이면 역할에 따라 자동 생성
        if (linkUrl == null || linkUrl.isEmpty()) {
            switch (receiver.getRole()) {
                case GUARDIAN:
                    linkUrl = "/guardian/alerts";
                    break;
                case COUNSELOR:
                    linkUrl = "/counselor/alerts";
                    break;
                case ADMIN:
                    linkUrl = "/admin"; // 관리자는 별도 알림 페이지가 없으므로 대시보드로
                    break;
                default:
                    // 기본값 또는 처리 안함
                    break;
            }
        }

        Notification notification = Notification.createEmergencyNewNotification(
                receiver,
                alertId,
                elderlyName,
                severityDescription,
                linkUrl);
        Notification saved = notificationRepository.save(notification);

        sendRealtimeNotification(receiverUserId, saved);

        log.info("[NotificationService] 긴급 알림(Notification) 생성. receiverId={}, role={}, alertId={}",
                receiverUserId, receiver.getRole(), alertId);
        return saved;
    }

    /**
     * 시스템 알림 (관리자가 직접 생성)
     */
    @Transactional
    public List<Notification> createSystemNotifications(SystemNotificationRequest request) {
        List<Notification> notifications = new ArrayList<>();

        List<Long> receiverIds = request.getReceiverUserIds();
        if (receiverIds == null || receiverIds.isEmpty()) {
            // 전체 사용자 대상
            receiverIds = userRepository.findAll().stream()
                    .map(User::getId)
                    .collect(Collectors.toList());
        }

        for (Long userId : receiverIds) {
            try {
                User receiver = findUserById(userId);
                Notification notification = Notification.createSystemNotification(receiver, request.getTitle(),
                        request.getContent());
                Notification saved = notificationRepository.save(notification);
                notifications.add(saved);

                sendRealtimeNotification(userId, saved);
            } catch (Exception e) {
                log.warn("[NotificationService] 시스템 알림 생성 실패. userId={}", userId, e);
            }
        }

        log.info("[NotificationService] 시스템 알림 생성 완료. 발송 수={}", notifications.size());
        return notifications;
    }

    // ========== 알림 조회 ==========

    /**
     * 사용자별 알림 목록 조회
     */
    public Page<SummaryResponse> getNotifications(Long userId, Pageable pageable) {
        return notificationRepository.findByReceiverId(userId, pageable)
                .map(SummaryResponse::from);
    }

    /**
     * 사용자별 미확인 알림 목록
     */
    public List<SummaryResponse> getUnreadNotifications(Long userId) {
        return notificationRepository.findUnreadByReceiverId(userId).stream()
                .map(SummaryResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 미확인 알림 수
     */
    public long getUnreadCount(Long userId) {
        return notificationRepository.countUnreadByReceiverId(userId);
    }

    /**
     * 알림 상세 조회 (읽음 처리 포함)
     */
    @Transactional
    public DetailResponse getNotificationDetail(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        // 본인 알림인지 확인
        if (!notification.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        // 읽음 처리
        if (notification.isUnread()) {
            notification.markAsRead();
            notificationRepository.save(notification);

            // 미확인 수 업데이트 전송
            long unreadCount = notificationRepository.countUnreadByReceiverId(userId);
            unifiedSseService.sendUnreadCountUpdate(userId, 0, unreadCount);
        }

        return DetailResponse.from(notification);
    }

    /**
     * 최근 알림 조회 (상단 알림 팝업용)
     */
    public List<SummaryResponse> getRecentNotifications(Long userId, int limit) {
        return notificationRepository
                .findRecentByReceiverId(userId, org.springframework.data.domain.PageRequest.of(0, limit))
                .stream()
                .map(SummaryResponse::from)
                .collect(Collectors.toList());
    }

    // ========== 읽음 처리 ==========

    /**
     * 단건 읽음 처리
     */
    @Transactional
    public void markAsRead(Long notificationId, Long userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("알림을 찾을 수 없습니다."));

        if (!notification.getReceiver().getId().equals(userId)) {
            throw new IllegalArgumentException("접근 권한이 없습니다.");
        }

        if (notification.isUnread()) {
            notification.markAsRead();
            notificationRepository.save(notification);

            // 미확인 수 업데이트 전송
            long unreadCount = notificationRepository.countUnreadByReceiverId(userId);
            unifiedSseService.sendUnreadCountUpdate(userId, 0, unreadCount);
        }
    }

    /**
     * 전체 읽음 처리
     */
    @Transactional
    public void markAllAsRead(Long userId) {
        int updated = notificationRepository.markAllAsReadByReceiverId(userId, LocalDateTime.now());
        log.info("[NotificationService] 전체 읽음 처리. userId={}, 처리 건수={}", userId, updated);

        // 미확인 수 0으로 업데이트 전송
        unifiedSseService.sendUnreadCountUpdate(userId, 0, 0);
    }

    // ========== 통계 ==========

    /**
     * 사용자별 알림 통계
     */
    public StatsResponse getStats(Long userId) {
        long totalCount = notificationRepository.countUnreadByReceiverId(userId);
        long unreadCount = notificationRepository.countUnreadByReceiverId(userId);

        LocalDateTime todayStart = LocalDateTime.now().with(LocalTime.MIN);
        LocalDateTime todayEnd = LocalDateTime.now().with(LocalTime.MAX);
        long todayCount = notificationRepository.countByDateRange(todayStart, todayEnd);

        List<Object[]> typeCounts = notificationRepository.countByTypeForUser(userId);
        List<StatsResponse.TypeCount> countByType = typeCounts.stream()
                .map(row -> StatsResponse.TypeCount.builder()
                        .type((NotificationType) row[0])
                        .typeText(((NotificationType) row[0]).getDescription())
                        .count((Long) row[1])
                        .build())
                .collect(Collectors.toList());

        return StatsResponse.builder()
                .totalCount(totalCount)
                .unreadCount(unreadCount)
                .todayCount(todayCount)
                .countByType(countByType)
                .build();
    }

    // ========== 내부 메서드 ==========

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자를 찾을 수 없습니다. userId=" + userId));
    }

    private void sendRealtimeNotification(Long userId, Notification notification) {
        try {
            unifiedSseService.sendNotification(userId, notification);
        } catch (Exception e) {
            log.warn("[NotificationService] SSE 전송 실패. userId={}", userId, e);
        }
    }
}
