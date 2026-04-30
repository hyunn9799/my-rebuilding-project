package com.aicc.silverlink.domain.ocr.service;

import com.aicc.silverlink.domain.ocr.dto.QualityReportRunHistoryItem;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunHistoryResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunRequest;
import com.aicc.silverlink.domain.ocr.dto.QualityReportRunResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportTrendResponse;
import com.aicc.silverlink.domain.ocr.dto.QualityReportUpsertRequest;
import com.aicc.silverlink.domain.ocr.dto.QualityReportUpsertResponse;
import com.aicc.silverlink.domain.ocr.entity.OcrQualityReportRun;
import com.aicc.silverlink.domain.ocr.repository.OcrQualityReportRunRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class OcrQualityReportRunService {

    public static final String ACTION_REPORT_RUN = "QUALITY_REPORT_RUN";
    public static final String ACTION_ALIAS_UPSERT = "QUALITY_ALIAS_UPSERT";

    private static final List<String> PENDING_STATUSES = List.of(
            "LOW_CONFIDENCE",
            "AMBIGUOUS",
            "NEED_USER_CONFIRMATION");

    private final OcrQualityReportRunRepository repository;

    @Transactional
    public OcrQualityReportRun recordReportRun(
            Long actorUserId,
            QualityReportRunRequest request,
            QualityReportRunResponse response,
            boolean success,
            String message) {
        OcrQualityReportRun run = OcrQualityReportRun.builder()
                .actionType(ACTION_REPORT_RUN)
                .actorUserId(actorUserId)
                .limit(request != null ? request.getLimit() : null)
                .success(success)
                .matchedCount(countDecisionStatus(response, "MATCHED"))
                .pendingReviewCount(countPendingReviews(response))
                .aliasCandidateCount(valueOrZero(response != null ? response.getAliasCandidateCount() : null))
                .manualReviewCount(valueOrZero(response != null ? response.getManualReviewCount() : null))
                .normalizationCandidateCount(valueOrZero(response != null ? response.getNormalizationCandidateCount() : null))
                .message(message)
                .build();
        return repository.save(run);
    }

    @Transactional
    public OcrQualityReportRun recordUpsertRun(
            Long actorUserId,
            QualityReportUpsertRequest request,
            QualityReportUpsertResponse response,
            boolean success,
            String message) {
        OcrQualityReportRun run = OcrQualityReportRun.builder()
                .actionType(ACTION_ALIAS_UPSERT)
                .actorUserId(actorUserId)
                .limit(request != null ? request.getLimit() : null)
                .success(success)
                .candidateCount(valueOrZero(response != null ? response.getCandidateCount() : null))
                .upsertedCount(valueOrZero(response != null ? response.getUpsertedCount() : null))
                .skippedCount(valueOrZero(response != null ? response.getSkippedCount() : null))
                .message(message)
                .build();
        return repository.save(run);
    }

    @Transactional(readOnly = true)
    public QualityReportRunHistoryResponse getRecentRuns(int limit) {
        int safeLimit = Math.max(1, Math.min(limit, 50));
        List<QualityReportRunHistoryItem> items = repository
                .findAllByOrderByCreatedAtDesc(PageRequest.of(0, safeLimit))
                .stream()
                .map(this::toItem)
                .toList();
        return QualityReportRunHistoryResponse.builder()
                .items(items)
                .trend(buildTrend())
                .build();
    }

    private QualityReportTrendResponse buildTrend() {
        List<OcrQualityReportRun> runs = repository.findByActionTypeOrderByCreatedAtDesc(
                ACTION_REPORT_RUN,
                PageRequest.of(0, 2));
        if (runs.size() < 2) {
            return null;
        }
        OcrQualityReportRun current = runs.get(0);
        OcrQualityReportRun previous = runs.get(1);
        return QualityReportTrendResponse.builder()
                .currentRunId(current.getId())
                .previousRunId(previous.getId())
                .matchedDelta(delta(current.getMatchedCount(), previous.getMatchedCount()))
                .pendingReviewDelta(delta(current.getPendingReviewCount(), previous.getPendingReviewCount()))
                .aliasCandidateDelta(delta(current.getAliasCandidateCount(), previous.getAliasCandidateCount()))
                .manualReviewDelta(delta(current.getManualReviewCount(), previous.getManualReviewCount()))
                .normalizationCandidateDelta(delta(current.getNormalizationCandidateCount(), previous.getNormalizationCandidateCount()))
                .build();
    }

    private QualityReportRunHistoryItem toItem(OcrQualityReportRun run) {
        return QualityReportRunHistoryItem.builder()
                .id(run.getId())
                .actionType(run.getActionType())
                .actorUserId(run.getActorUserId())
                .limit(run.getLimit())
                .success(run.getSuccess())
                .matchedCount(run.getMatchedCount())
                .pendingReviewCount(run.getPendingReviewCount())
                .aliasCandidateCount(run.getAliasCandidateCount())
                .manualReviewCount(run.getManualReviewCount())
                .normalizationCandidateCount(run.getNormalizationCandidateCount())
                .candidateCount(run.getCandidateCount())
                .upsertedCount(run.getUpsertedCount())
                .skippedCount(run.getSkippedCount())
                .message(run.getMessage())
                .createdAt(run.getCreatedAt() != null ? run.getCreatedAt().toString() : null)
                .build();
    }

    private int countPendingReviews(QualityReportRunResponse response) {
        return PENDING_STATUSES.stream()
                .mapToInt(status -> countDecisionStatus(response, status))
                .sum();
    }

    private int countDecisionStatus(QualityReportRunResponse response, String status) {
        if (response == null || response.getDecisionCounts() == null) {
            return 0;
        }
        return response.getDecisionCounts().stream()
                .filter(row -> Objects.equals(status, String.valueOf(row.get("decision_status"))))
                .map(row -> row.get("total"))
                .mapToInt(this::toInt)
                .sum();
    }

    private int toInt(Object value) {
        if (value instanceof Number number) {
            return number.intValue();
        }
        if (value instanceof String string) {
            try {
                return Integer.parseInt(string);
            } catch (NumberFormatException e) {
                log.debug("Unable to parse count value: {}", string);
            }
        }
        if (value instanceof Map<?, ?> map && map.containsKey("value")) {
            return toInt(map.get("value"));
        }
        return 0;
    }

    private int valueOrZero(Integer value) {
        return value != null ? value : 0;
    }

    private int delta(Integer current, Integer previous) {
        return valueOrZero(current) - valueOrZero(previous);
    }
}
