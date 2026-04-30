import apiClient from './index';

// ── 타입 ──

export interface AliasSuggestionItem {
    id: number;
    item_seq: string;
    item_name?: string;
    alias_name: string;
    alias_normalized: string;
    suggestion_type: string;
    source: string;
    source_request_id?: string;
    review_status: string;
    frequency: number;
    priority_score?: number;
    priority_reason?: string;
    is_active: number;
    reviewed_by?: string;
    reviewed_at?: string;
    created_at?: string;
    updated_at?: string;
}

export interface AliasSuggestionPageResponse {
    items: AliasSuggestionItem[];
    total: number;
    page: number;
    size: number;
}

export interface AliasSuggestionActionResponse {
    success: boolean;
    message: string;
    target_table?: string;
    reload_success?: boolean;
    reload_warning?: string;
}

export interface QualityReportRunRequest {
    limit?: number;
    include_candidates?: boolean;
    persist_files?: boolean;
}

export interface QualityReportRunResponse {
    success: boolean;
    generated_at?: string;
    decision_counts?: Array<Record<string, unknown>>;
    suggestion_counts?: Array<Record<string, unknown>>;
    match_method_counts?: unknown[];
    recommended_action_counts?: Record<string, number>;
    alias_candidate_count?: number;
    manual_review_count?: number;
    normalization_candidate_count?: number;
    report_markdown?: string;
    alias_candidates?: Array<Record<string, unknown>>;
    message?: string;
}

export interface QualityReportUpsertRequest {
    limit?: number;
    confirm_write: boolean;
}

export interface QualityReportUpsertResponse {
    success: boolean;
    upserted_count?: number;
    candidate_count?: number;
    skipped_count?: number;
    message?: string;
    generated_at?: string;
}

export interface QualityReportRunHistoryItem {
    id: number;
    action_type: string;
    actor_user_id?: number;
    limit?: number;
    success?: boolean;
    matched_count?: number;
    pending_review_count?: number;
    alias_candidate_count?: number;
    manual_review_count?: number;
    normalization_candidate_count?: number;
    candidate_count?: number;
    upserted_count?: number;
    skipped_count?: number;
    message?: string;
    created_at?: string;
}

export interface QualityReportTrendResponse {
    matched_delta?: number;
    pending_review_delta?: number;
    alias_candidate_delta?: number;
    manual_review_delta?: number;
    normalization_candidate_delta?: number;
    current_run_id?: number;
    previous_run_id?: number;
}

export interface QualityReportRunHistoryResponse {
    items: QualityReportRunHistoryItem[];
    trend?: QualityReportTrendResponse | null;
}

// ── API ──

export const getAliasSuggestions = async (
    page: number = 1,
    size: number = 20,
    reviewStatus: string = 'PENDING',
): Promise<AliasSuggestionPageResponse> => {
    const response = await apiClient.get<AliasSuggestionPageResponse>(
        '/api/admin/alias-suggestions',
        { params: { page, size, reviewStatus } },
    );
    return response.data;
};

export const approveSuggestion = async (
    suggestionId: number,
    reviewedBy: string = 'admin',
): Promise<AliasSuggestionActionResponse> => {
    const response = await apiClient.put<AliasSuggestionActionResponse>(
        `/api/admin/alias-suggestions/${suggestionId}/approve`,
        null,
        { params: { reviewedBy } },
    );
    return response.data;
};

export const rejectSuggestion = async (
    suggestionId: number,
    reviewedBy: string = 'admin',
): Promise<AliasSuggestionActionResponse> => {
    const response = await apiClient.put<AliasSuggestionActionResponse>(
        `/api/admin/alias-suggestions/${suggestionId}/reject`,
        null,
        { params: { reviewedBy } },
    );
    return response.data;
};

export const reloadDictionary = async (): Promise<AliasSuggestionActionResponse> => {
    const response = await apiClient.post<AliasSuggestionActionResponse>(
        '/api/admin/alias-suggestions/reload-dictionary',
    );
    return response.data;
};

export const runQualityReport = async (
    request: QualityReportRunRequest = {},
): Promise<QualityReportRunResponse> => {
    const response = await apiClient.post<QualityReportRunResponse>(
        '/api/admin/ocr/quality-report/run',
        {
            limit: request.limit ?? 20,
            include_candidates: request.include_candidates ?? true,
            persist_files: request.persist_files ?? false,
        },
    );
    return response.data;
};

export const upsertQualityReportAliasCandidates = async (
    request: QualityReportUpsertRequest,
): Promise<QualityReportUpsertResponse> => {
    const response = await apiClient.post<QualityReportUpsertResponse>(
        '/api/admin/ocr/quality-report/upsert-alias-candidates',
        request,
    );
    return response.data;
};

export const getQualityReportRuns = async (
    limit: number = 10,
): Promise<QualityReportRunHistoryResponse> => {
    const response = await apiClient.get<QualityReportRunHistoryResponse>(
        '/api/admin/ocr/quality-report/runs',
        { params: { limit } },
    );
    return response.data;
};

export default {
    getAliasSuggestions,
    approveSuggestion,
    rejectSuggestion,
    reloadDictionary,
    runQualityReport,
    upsertQualityReportAliasCandidates,
    getQualityReportRuns,
};
