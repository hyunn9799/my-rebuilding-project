package com.aicc.silverlink.domain.counseling.repository;

import com.aicc.silverlink.domain.counseling.entity.CounselingRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CounselingRecordRepository extends JpaRepository<CounselingRecord, Long> {
    List<CounselingRecord> findByCounselorIdOrderByCounselingDateDescCounselingTimeDesc(Long counselorId);

    List<CounselingRecord> findByElderlyIdOrderByCounselingDateDescCounselingTimeDesc(Long elderlyId);
}
