package com.aicc.silverlink.domain.elderly.scheduler;

import com.aicc.silverlink.domain.elderly.dto.CallScheduleDto.StartCallRequest;
import com.aicc.silverlink.domain.elderly.service.CallScheduleService;
import com.aicc.silverlink.infrastructure.callbot.CallBotClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 통화 스케줄러
 * 
 * 매분 정각에 실행되어 예정된 통화를 Python CallBot에 요청합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class CallScheduler {

    private final CallScheduleService callScheduleService;
    private final CallBotClient callBotClient;

    /**
     * 매분 정각에 실행 - 예정된 통화 발신
     * 
     * cron 표현식: 초 분 시 일 월 요일
     * "0 * * * * *" = 매분 0초에 실행
     */
    @Scheduled(cron = "0 * * * * *")
    public void triggerScheduledCalls() {
        log.debug("[CallScheduler] 스케줄 체크 시작");

        List<StartCallRequest> dueList = callScheduleService.getDueForCall();

        if (dueList.isEmpty()) {
            log.debug("[CallScheduler] 현재 시간에 예정된 통화 없음");
            return;
        }

        log.info("[CallScheduler] {}명에게 전화 발신 예정", dueList.size());

        int successCount = 0;
        int failCount = 0;

        for (StartCallRequest request : dueList) {
            boolean success = callBotClient.startCall(request);
            if (success) {
                successCount++;
            } else {
                failCount++;
            }
        }

        log.info("[CallScheduler] 전화 발신 완료: 성공={}, 실패={}", successCount, failCount);
    }
}
