package com.aicc.silverlink.domain.emergency.event;

import com.aicc.silverlink.domain.notification.service.UnifiedSseService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * 긴급 알림 이벤트 리스너
 * 트랜잭션 커밋 후 SSE 알림을 발송하여, 프론트엔드가 DB 조회 시 데이터를 확인할 수 있도록 보장
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class EmergencyAlertEventListener {

    private final UnifiedSseService unifiedSseService;
    private final com.aicc.silverlink.domain.notification.service.NotificationService notificationService; // 주입 추가

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleEmergencyAlertCreated(EmergencyAlertCreatedEvent event) {
        try {
            // 1. 기존 긴급 알림 SSE 발송 (팝업용)
            unifiedSseService.sendEmergencyAlertToUsers(
                    event.getRecipientUserIds(),
                    event.getAlert());

            // 2. Notification 엔티티 생성 및 SSE 발송 (드롭다운 목록/배지용)
            // 주의: 여기서 NotificationService를 호출하면 별도 트랜잭션으로 실행됨
            for (Long userId : event.getRecipientUserIds()) {
                try {
                    // 링크 URL 결정 로직은 서비스나 엔티티에서 처리하는 것이 좋지만, 간단히 여기서 분기 처리하거나
                    // 수신자 역할에 따라 다를 수 있음.
                    // 일단 NotificationService에 넘기는 linkUrl은 수신자 역할별로 다를 수 있어서...
                    // 하지만 여기서는 userId만 알 수 있음.
                    // 역할별 링크를 정확히 하려면 수신자 정보를 더 조회해야 함.
                    // 그러나 Notification.createEmergencyNewNotification에서 linkUrl을 받도록 설계했음.
                    // 공통 URL 또는 역할별 URL 로직 필요.

                    // 간단히 역할 구분 없이 처리하거나, 역할별 처리가 필요하면 로직 추가.
                    // 현재 시스템에서 긴급 알림은 역할별로 URL이 다름 (/guardian/alerts, /counselor/alerts 등)
                    // 해결책: 수신자 역할을 알기 위해 AlertRecipient를 조회하거나,
                    // 단순히 여기서는 공통 처리를 하고 상세는 프론트가 처리?
                    // 아니면 알림 클릭시 리다이렉트 페이지로 이동?

                    // 우선 간단화를 위해 하드코딩된 역할별 URL 매핑이 어려우므로,
                    // NotificationService 내부나 여기서 DB 조회가 필요할 수 있음.
                    // 하지만 성능상 루프 돌며 조회는 부담.
                    // 대안: NotificationService에서 처리하지 않고, LinkUrl을 제네릭하게 하거나 프론트가 라우팅.

                    // 여기서는 단순히 "/guardian/alerts" 또는 유사 경로를 쓰되,
                    // 수신자가 누구냐에 따라 다르므로...
                    // Notification에 역할 정보가 없으므로 linkUrl이 중요함.

                    // EmergencyAlertService.createAndNotifyRecipients에서 recipients를 저장함.
                    // event에는 recipientUserIds만 있음.
                    // 하지만 event.getAlert()에는 정보가 있음.

                    // 일단 수신자별 주소 생성을 위해 SmsService의 로직을 참고해도 되지만,
                    // 여기서는 "알림 목록" 클릭 시 이동할 주소임.
                    // 프론트엔드에서 linkUrl이 있으면 거기로 이동함.
                    // 모든 유저에게 동일한 URL을 보내면 안됨 (보호자 vs 상담사).

                    // 해결: NotificationService.createEmergencyNewNotification을 호출할 때
                    // 수신자의 역할을 확인해서 URL을 생성해야 함.
                    // NotificationService.createEmergencyNewNotification 내부에서 User를 조회하므로,
                    // 거기서 User Role을 보고 URL을 결정하도록 수정하는 제안.

                    // 하지만 NotificationService는 도메인 서비스라 URL 생성 로직(역할별 경로)을 넣기 애매함.
                    // 일단 여기서는 null 또는 공통 경로를 넘기고, NotificationService를 수정하여 User Role에 따라 URL 할당하도록
                    // 변경하자.

                    notificationService.createEmergencyNewNotification(
                            userId,
                            event.getAlert().getId(),
                            event.getAlert().getElderly().getUser().getName(),
                            event.getAlert().getSeverity().getDescription(),
                            null // URL은 서비스 내부에서 결정하도록 위임 (아래 단계에서 Service 수정)
                    );
                } catch (Exception e) {
                    log.error("긴급 알림 Notification 생성 실패 userId={}", userId, e);
                }
            }

            log.info("[EmergencyAlertEventListener] 트랜잭션 커밋 후 알림 발송 완료. alertId={}, 수신자 수={}",
                    event.getAlert().getId(), event.getRecipientUserIds().size());
        } catch (Exception e) {
            log.error("[EmergencyAlertEventListener] 알림 발송 중 오류. alertId={}, error={}",
                    event.getAlert().getId(), e.getMessage(), e);
        }
    }
}
