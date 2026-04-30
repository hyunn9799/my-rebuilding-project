package com.aicc.silverlink.domain.ocr.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class QualityReportRunHistoryResponse {

    private List<QualityReportRunHistoryItem> items;

    private QualityReportTrendResponse trend;
}
