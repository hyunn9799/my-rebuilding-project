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

export default {
    getAliasSuggestions,
    approveSuggestion,
    rejectSuggestion,
    reloadDictionary,
};
