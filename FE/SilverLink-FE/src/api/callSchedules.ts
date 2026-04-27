import apiClient from './index';

// ===== Types =====
export interface CallScheduleResponse {
    elderlyId: number;
    elderlyName: string;
    preferredCallTime: string | null;
    preferredCallDays: string[];
    callScheduleEnabled: boolean;
}

export interface CallScheduleUpdateRequest {
    preferredCallTime: string;
    preferredCallDays: string[];
    callScheduleEnabled: boolean;
}

export interface ScheduleChangeRequest {
    id: number;
    elderlyId: number;
    elderlyName: string;
    requestedCallTime: string;
    requestedCallDays: string[];
    status: 'PENDING' | 'APPROVED' | 'REJECTED';
    createdAt: string;
    processedAt: string | null;
    processedByName: string | null;
    rejectReason: string | null;
}

export interface CreateScheduleChangeRequest {
    preferredCallTime: string;
    preferredCallDays: string[];
}

// ===== Call Schedule API (어르신용) =====

/**
 * 본인 통화 스케줄 조회
 * GET /api/call-schedules/me
 */
export const getMySchedule = async (): Promise<CallScheduleResponse> => {
    const response = await apiClient.get<{ data: CallScheduleResponse }>('/api/call-schedules/me');
    return response.data.data;
};

/**
 * 본인 통화 스케줄 수정
 * PUT /api/call-schedules/me
 */
export const updateMySchedule = async (data: CallScheduleUpdateRequest): Promise<CallScheduleResponse> => {
    const response = await apiClient.put<{ data: CallScheduleResponse }>('/api/call-schedules/me', data);
    return response.data.data;
};

// ===== Schedule Change Request API (어르신용) =====

/**
 * 스케줄 변경 요청 생성
 * POST /api/schedule-change-requests
 */
export const createScheduleChangeRequest = async (data: CreateScheduleChangeRequest): Promise<ScheduleChangeRequest> => {
    const response = await apiClient.post<{ data: ScheduleChangeRequest }>('/api/schedule-change-requests', data);
    return response.data.data;
};

/**
 * 내 변경 요청 목록 조회
 * GET /api/schedule-change-requests/me
 */
export const getMyChangeRequests = async (): Promise<ScheduleChangeRequest[]> => {
    const response = await apiClient.get<{ data: ScheduleChangeRequest[] }>('/api/schedule-change-requests/me');
    return response.data.data;
};

// ===== Schedule Change Request API (상담사용) =====

/**
 * 대기 중 변경 요청 목록
 * GET /api/schedule-change-requests/pending
 */
export const getPendingRequests = async (): Promise<ScheduleChangeRequest[]> => {
    const response = await apiClient.get<{ data: ScheduleChangeRequest[] }>('/api/schedule-change-requests/pending');
    return response.data.data;
};

/**
 * 변경 요청 승인
 * PUT /api/schedule-change-requests/{requestId}/approve
 */
export const approveScheduleRequest = async (requestId: number): Promise<ScheduleChangeRequest> => {
    const response = await apiClient.put<{ data: ScheduleChangeRequest }>(`/api/schedule-change-requests/${requestId}/approve`);
    return response.data.data;
};

/**
 * 변경 요청 거절
 * PUT /api/schedule-change-requests/{requestId}/reject
 */
export const rejectScheduleRequest = async (requestId: number, reason?: string): Promise<ScheduleChangeRequest> => {
    const response = await apiClient.put<{ data: ScheduleChangeRequest }>(
        `/api/schedule-change-requests/${requestId}/reject`,
        { reason }
    );
    return response.data.data;
};

// ===== Admin Call Schedule API =====

/**
 * 특정 어르신 통화 스케줄 조회 (관리자용)
 * GET /api/call-schedules/elderly/{elderlyId}
 */
export const getElderlySchedule = async (elderlyId: number): Promise<CallScheduleResponse> => {
    const response = await apiClient.get<{ data: CallScheduleResponse }>(`/api/call-schedules/elderly/${elderlyId}`);
    return response.data.data;
};

/**
 * 특정 어르신 통화 스케줄 수정 (관리자용)
 * PUT /api/call-schedules/elderly/{elderlyId}
 */
export const updateElderlySchedule = async (elderlyId: number, data: CallScheduleUpdateRequest): Promise<CallScheduleResponse> => {
    const response = await apiClient.put<{ data: CallScheduleResponse }>(`/api/call-schedules/elderly/${elderlyId}`, data);
    return response.data.data;
};

export default {
    getMySchedule,
    updateMySchedule,
    createScheduleChangeRequest,
    getMyChangeRequests,
    getPendingRequests,
    approveScheduleRequest,
    rejectScheduleRequest,
    getElderlySchedule,
    updateElderlySchedule,
};
