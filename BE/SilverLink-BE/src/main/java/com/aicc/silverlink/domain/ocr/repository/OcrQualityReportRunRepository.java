package com.aicc.silverlink.domain.ocr.repository;

import com.aicc.silverlink.domain.ocr.entity.OcrQualityReportRun;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface OcrQualityReportRunRepository extends JpaRepository<OcrQualityReportRun, Long> {

    List<OcrQualityReportRun> findAllByOrderByCreatedAtDesc(Pageable pageable);

    List<OcrQualityReportRun> findByActionTypeOrderByCreatedAtDesc(String actionType, Pageable pageable);
}
