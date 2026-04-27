package com.aicc.silverlink;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * 임시 테스트용 스케줄 러너
 * 앱 실행 시 첫 번째 어르신의 통화 스케줄을 현재 시간 +1분으로 설정합니다.
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class TemporaryScheduleRunner implements CommandLineRunner {

    private final ElderlyRepository elderlyRepository;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        log.info("[TemporaryScheduleRunner] 임시 스케줄 설정 시작...");

        List<Elderly> elderlyList = elderlyRepository.findAll();
        if (elderlyList.isEmpty()) {
            log.warn("[TemporaryScheduleRunner] 등록된 어르신이 없어 테스트 설정을 건너뜁니다.");
            return;
        }

        Elderly target = elderlyList.get(0);

        // 현재 시간 + 1분 (스케줄러가 다음 분 00초에 실행되므로 맞춤)
        LocalTime nextMinute = LocalTime.now().plusMinutes(1);
        String timeStr = nextMinute.format(DateTimeFormatter.ofPattern("HH:mm"));

        // 모든 요일 (오늘이 무슨 요일이든 실행되도록)
        String allDays = "MON,TUE,WED,THU,FRI,SAT,SUN";

        // 기존 값 백업 로그
        log.info(">> 변경 전: ID={}, Time={}, Days={}, Enabled={}",
                target.getId(), target.getPreferredCallTime(), target.getPreferredCallDays(),
                target.getCallScheduleEnabled());

        // 업데이트
        target.updateCallSchedule(timeStr, allDays, true);
        elderlyRepository.save(target);

        log.info(">> 변경 후: ID={}, Time={}, Days={}, Enabled={}",
                target.getId(), timeStr, allDays, true);

        log.info("[TemporaryScheduleRunner] 설정 완료! {}에 CallScheduler가 실행될 예정입니다. (대상 ID: {})", timeStr, target.getId());
    }
}
