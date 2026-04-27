package com.aicc.silverlink.domain.medication.repository;

import com.aicc.silverlink.domain.medication.entity.MedicationScheduleTime;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationScheduleTimeRepository extends JpaRepository<MedicationScheduleTime, Long> {

    // 특정 복약 일정의 시간 목록 조회
    List<MedicationScheduleTime> findByScheduleIdOrderByDoseSeq(Long scheduleId);

    // 여러 복약 일정의 시간 목록 조회
    List<MedicationScheduleTime> findByScheduleIdIn(List<Long> scheduleIds);

    // 특정 복약 일정의 시간 삭제
    void deleteByScheduleId(Long scheduleId);
}
