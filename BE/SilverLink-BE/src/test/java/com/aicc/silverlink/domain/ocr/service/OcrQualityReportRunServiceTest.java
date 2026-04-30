package com.aicc.silverlink.domain.ocr.service;

import com.aicc.silverlink.domain.ocr.dto.QualityReportRunHistoryResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunRequest;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportUpsertRequest;
import com.aicc.silverlink.domain.ocr.dto.QualityReportUpsertResponse;
import com.aicc.silverlink.domain.ocr.entity.OcrQualityReportRun;
import com.aicc.silverlink.domain.ocr.repository.OcrQualityReportRunRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DataJpaTest
@Import(OcrQualityReportRunService.class)
class OcrQualityReportRunServiceTest {

    @Autowired
    private OcrQualityReportRunService service;

    @Autowired
    private OcrQualityReportRunRepository repository;

    @Test
    @DisplayName("quality report run records decision and candidate counts")
    void recordReportRunStoresSummaryCounts() {
        QualityReportRunResponse response = QualityReportRunResponse.builder()
                .success(true)
                .decisionCounts(List.of(
                        Map.of("decision_status", "MATCHED", "total", 7),
                        Map.of("decision_status", "LOW_CONFIDENCE", "total", 2),
                        Map.of("decision_status", "AMBIGUOUS", "total", "3"),
                        Map.of("decision_status", "NEED_USER_CONFIRMATION", "total", Map.of("value", 4))))
                .aliasCandidateCount(5)
                .manualReviewCount(6)
                .normalizationCandidateCount(1)
                .message("generated")
                .build();

        OcrQualityReportRun saved = service.recordReportRun(
                101L,
                QualityReportRunRequest.builder().limit(20).build(),
                response,
                true,
                "generated");

        OcrQualityReportRun found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getActionType()).isEqualTo(OcrQualityReportRunService.ACTION_REPORT_RUN);
        assertThat(found.getActorUserId()).isEqualTo(101L);
        assertThat(found.getLimit()).isEqualTo(20);
        assertThat(found.getSuccess()).isTrue();
        assertThat(found.getMatchedCount()).isEqualTo(7);
        assertThat(found.getPendingReviewCount()).isEqualTo(9);
        assertThat(found.getAliasCandidateCount()).isEqualTo(5);
        assertThat(found.getManualReviewCount()).isEqualTo(6);
        assertThat(found.getNormalizationCandidateCount()).isEqualTo(1);
        assertThat(found.getCreatedAt()).isNotNull();
    }

    @Test
    @DisplayName("alias upsert run records write counts")
    void recordUpsertRunStoresWriteCounts() {
        QualityReportUpsertResponse response = QualityReportUpsertResponse.builder()
                .success(true)
                .candidateCount(8)
                .upsertedCount(6)
                .skippedCount(2)
                .message("upserted")
                .build();

        OcrQualityReportRun saved = service.recordUpsertRun(
                102L,
                QualityReportUpsertRequest.builder().limit(30).confirmWrite(true).build(),
                response,
                true,
                "upserted");

        OcrQualityReportRun found = repository.findById(saved.getId()).orElseThrow();
        assertThat(found.getActionType()).isEqualTo(OcrQualityReportRunService.ACTION_ALIAS_UPSERT);
        assertThat(found.getActorUserId()).isEqualTo(102L);
        assertThat(found.getLimit()).isEqualTo(30);
        assertThat(found.getSuccess()).isTrue();
        assertThat(found.getCandidateCount()).isEqualTo(8);
        assertThat(found.getUpsertedCount()).isEqualTo(6);
        assertThat(found.getSkippedCount()).isEqualTo(2);
    }

    @Test
    @DisplayName("recent run response clamps limit and includes trend deltas from latest two report runs")
    void getRecentRunsClampsLimitAndBuildsTrend() {
        service.recordReportRun(
                101L,
                QualityReportRunRequest.builder().limit(10).build(),
                reportResponse(10, 4, 3, 2, 1),
                true,
                "previous");
        service.recordUpsertRun(
                101L,
                QualityReportUpsertRequest.builder().limit(10).confirmWrite(true).build(),
                QualityReportUpsertResponse.builder().candidateCount(3).upsertedCount(2).skippedCount(1).build(),
                true,
                "upsert");
        service.recordReportRun(
                101L,
                QualityReportRunRequest.builder().limit(10).build(),
                reportResponse(14, 2, 1, 1, 0),
                true,
                "current");

        QualityReportRunHistoryResponse oneItem = service.getRecentRuns(0);
        assertThat(oneItem.getItems()).hasSize(1);

        QualityReportRunHistoryResponse allItems = service.getRecentRuns(50);
        assertThat(allItems.getItems()).hasSize(3);
        assertThat(allItems.getItems().get(0).getMessage()).isEqualTo("current");
        assertThat(allItems.getTrend()).isNotNull();
        assertThat(allItems.getTrend().getMatchedDelta()).isEqualTo(4);
        assertThat(allItems.getTrend().getPendingReviewDelta()).isEqualTo(-2);
        assertThat(allItems.getTrend().getAliasCandidateDelta()).isEqualTo(-2);
        assertThat(allItems.getTrend().getManualReviewDelta()).isEqualTo(-1);
        assertThat(allItems.getTrend().getNormalizationCandidateDelta()).isEqualTo(-1);
    }

    @Test
    @DisplayName("recent run response caps large limit at fifty rows")
    void getRecentRunsCapsLargeLimit() {
        for (int i = 0; i < 55; i++) {
            service.recordReportRun(
                    101L,
                    QualityReportRunRequest.builder().limit(10).build(),
                    reportResponse(i, 0, 0, 0, 0),
                    true,
                    "run-" + i);
        }

        QualityReportRunHistoryResponse response = service.getRecentRuns(999);

        assertThat(response.getItems()).hasSize(50);
        assertThat(repository.findAllByOrderByCreatedAtDesc(Pageable.ofSize(100))).hasSize(55);
    }

    private QualityReportRunResponse reportResponse(
            int matched,
            int pending,
            int aliasCandidates,
            int manualReviews,
            int normalizationCandidates) {
        return QualityReportRunResponse.builder()
                .success(true)
                .decisionCounts(List.of(
                        Map.of("decision_status", "MATCHED", "total", matched),
                        Map.of("decision_status", "LOW_CONFIDENCE", "total", pending)))
                .aliasCandidateCount(aliasCandidates)
                .manualReviewCount(manualReviews)
                .normalizationCandidateCount(normalizationCandidates)
                .build();
    }
}
