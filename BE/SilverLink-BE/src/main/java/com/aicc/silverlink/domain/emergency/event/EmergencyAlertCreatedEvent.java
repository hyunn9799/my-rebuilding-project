package com.aicc.silverlink.domain.emergency.event;

import com.aicc.silverlink.domain.emergency.entity.EmergencyAlert;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

import java.util.List;

/**
 * 긴급 알림 생성 이벤트
 * 트랜잭션 커밋 후 SSE 실시간 알림을 발송하기 위해 사용
 */
@Getter
public class EmergencyAlertCreatedEvent extends ApplicationEvent {

    private final EmergencyAlert alert;
    private final List<Long> recipientUserIds;

    public EmergencyAlertCreatedEvent(Object source, EmergencyAlert alert, List<Long> recipientUserIds) {
        super(source);
        this.alert = alert;
        this.recipientUserIds = recipientUserIds;
    }
}
