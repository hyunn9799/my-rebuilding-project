package com.aicc.silverlink.domain.medication.repository;

import com.aicc.silverlink.domain.medication.entity.MedicationSchedule;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MedicationScheduleRepository extends JpaRepository<MedicationSchedule, Long> {

    // 어르신별 활성 복약 일정 조회
    List<MedicationSchedule> findByElderlyUserIdAndIsActiveTrueOrderByCreatedAtDesc(Long elderlyUserId);

    // 어르신별 전체 복약 일정 조회
    List<MedicationSchedule> findByElderlyUserIdOrderByCreatedAtDesc(Long elderlyUserId);

    // 특정 어르신의 복약 일정 수
    long countByElderlyUserIdAndIsActiveTrue(Long elderlyUserId);
}
