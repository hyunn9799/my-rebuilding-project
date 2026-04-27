package com.aicc.silverlink.domain.medication.service;

import com.aicc.silverlink.domain.elderly.entity.Elderly;
import com.aicc.silverlink.domain.elderly.repository.ElderlyRepository;
import com.aicc.silverlink.domain.medication.dto.MedicationRequest;
import com.aicc.silverlink.domain.medication.dto.MedicationResponse;
import com.aicc.silverlink.domain.medication.entity.MedicationSchedule;
import com.aicc.silverlink.domain.medication.entity.MedicationScheduleTime;
import com.aicc.silverlink.domain.medication.repository.MedicationScheduleRepository;
import com.aicc.silverlink.domain.medication.repository.MedicationScheduleTimeRepository;
import com.aicc.silverlink.domain.user.entity.User;
import com.aicc.silverlink.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class MedicationService {

    private final MedicationScheduleRepository scheduleRepository;
    private final MedicationScheduleTimeRepository scheduleTimeRepository;
    private final ElderlyRepository elderlyRepository;
    private final UserRepository userRepository;

    // 시간 매핑
    private static final Map<String, LocalTime> TIME_MAP = Map.of(
            "morning", LocalTime.of(8, 0),
            "noon", LocalTime.of(12, 0),
            "evening", LocalTime.of(18, 0),
            "night", LocalTime.of(22, 0));

    /**
     * 복약 일정 등록 (어르신)
     */
    @Transactional
    public MedicationResponse createMedication(Long elderlyUserId, MedicationRequest request) {
        Elderly elderly = elderlyRepository.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("어르신 정보를 찾을 수 없습니다."));

        User user = userRepository.findById(elderlyUserId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));

        MedicationSchedule schedule = MedicationSchedule.builder()
                .elderly(elderly)
                .medicationName(request.getMedicationName())
                .dosageText(request.getDosageText())
                .startDate(request.getStartDate() != null ? request.getStartDate() : LocalDate.now())
                .endDate(request.getEndDate())
                .createdBy(user)
                .build();

        MedicationSchedule savedSchedule = scheduleRepository.save(schedule);

        // 복용 시간 저장
        List<MedicationScheduleTime> times = createScheduleTimes(savedSchedule, request.getTimes());
        scheduleTimeRepository.saveAll(times);

        return MedicationResponse.from(savedSchedule, times);
    }

    /**
     * 내 복약 일정 목록 조회 (어르신)
     */
    public List<MedicationResponse> getMyMedications(Long elderlyUserId) {
        List<MedicationSchedule> schedules = scheduleRepository
                .findByElderlyUserIdAndIsActiveTrueOrderByCreatedAtDesc(elderlyUserId);

        if (schedules.isEmpty()) {
            return List.of();
        }

        // 모든 시간 정보 조회
        List<Long> scheduleIds = schedules.stream()
                .map(MedicationSchedule::getId)
                .collect(Collectors.toList());

        List<MedicationScheduleTime> allTimes = scheduleTimeRepository.findByScheduleIdIn(scheduleIds);

        Map<Long, List<MedicationScheduleTime>> timesByScheduleId = allTimes.stream()
                .collect(Collectors.groupingBy(t -> t.getSchedule().getId()));

        return schedules.stream()
                .map(s -> MedicationResponse.from(s, timesByScheduleId.getOrDefault(s.getId(), List.of())))
                .collect(Collectors.toList());
    }

    /**
     * 복약 일정 상세 조회
     */
    public MedicationResponse getMedicationDetail(Long scheduleId, Long elderlyUserId) {
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("복약 일정을 찾을 수 없습니다."));

        if (!schedule.getElderly().getId().equals(elderlyUserId)) {
            throw new IllegalArgumentException("조회 권한이 없습니다.");
        }

        List<MedicationScheduleTime> times = scheduleTimeRepository.findByScheduleIdOrderByDoseSeq(scheduleId);
        return MedicationResponse.from(schedule, times);
    }

    /**
     * 복약 일정 삭제 (비활성화)
     */
    @Transactional
    public void deleteMedication(Long scheduleId, Long elderlyUserId) {
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("복약 일정을 찾을 수 없습니다."));

        if (!schedule.getElderly().getId().equals(elderlyUserId)) {
            throw new IllegalArgumentException("삭제 권한이 없습니다.");
        }

        schedule.deactivate();
    }

    /**
     * 알림 토글
     */
    @Transactional
    public MedicationResponse toggleReminder(Long scheduleId, Long elderlyUserId) {
        MedicationSchedule schedule = scheduleRepository.findById(scheduleId)
                .orElseThrow(() -> new IllegalArgumentException("복약 일정을 찾을 수 없습니다."));

        if (!schedule.getElderly().getId().equals(elderlyUserId)) {
            throw new IllegalArgumentException("수정 권한이 없습니다.");
        }

        schedule.toggleActive();
        List<MedicationScheduleTime> times = scheduleTimeRepository.findByScheduleIdOrderByDoseSeq(scheduleId);
        return MedicationResponse.from(schedule, times);
    }

    /**
     * 상담사용: 특정 어르신의 복약 일정 조회
     */
    public List<MedicationResponse> getMedicationsByElderly(Long elderlyUserId) {
        return getMyMedications(elderlyUserId);
    }

    private List<MedicationScheduleTime> createScheduleTimes(MedicationSchedule schedule, List<String> times) {
        short seq = 1;
        List<MedicationScheduleTime> scheduleTimes = new java.util.ArrayList<>();
        
        for (String timeStr : times) {
            MedicationScheduleTime scheduleTime = MedicationScheduleTime.builder()
                    .schedule(schedule)
                    .doseSeq(seq++)  // seq를 증가시킴
                    .intakeTime(TIME_MAP.getOrDefault(timeStr, LocalTime.of(8, 0)))
                    .build();
            scheduleTimes.add(scheduleTime);
        }
        
        return scheduleTimes;
    }
}
