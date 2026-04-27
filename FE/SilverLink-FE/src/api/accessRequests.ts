import apiClient from './index';

// 접근 권한 범위 타입
export type AccessScope = 'HEALTH_INFO' | 'MEDICATION' | 'CALL_RECORDS' | 'ALL';

// 요청 상태 타입
export type AccessRequestStatus = 'PENDING' | 'DOCUMENTS_VERIFIED' | 'APPROVED' | 'REJECTED' | 'REVOKED' | 'EXPIRED';

// 접근 요청 응답 타입
export interface AccessRequestResponse {
    id: number;
    requester: {
        userId: number;
        name: string;
        phone: string;
        email: string;
    };
    elderly: {
        userId: number;
        name: string;
        phone: string;
        admCode: number;
        sidoName: string;
        sigunguName: string;
        dongName: string;
        fullAddress: string;
    };
    scope: AccessScope;
    scopeDescription: string;
    status: AccessRequestStatus;
    statusDescription: string;
    documentVerified: boolean;
    reviewer: {
        userId: number;
        name: string;
    } | null;
    requestedAt: string;
    decidedAt: string | null;
    expiresAt: string | null;
    revokedAt: string | null;
    decisionNote: string | null;
    accessGranted: boolean;
}

export interface AccessRequestSummary {
    id: number;
    requesterName: string;
    elderlyName: string;
    scope: AccessScope;
    scopeDescription: string;
    status: AccessRequestStatus;
    statusDescription: string;
    documentVerified: boolean;
    requestedAt: string;
    decidedAt: string | null;
    decisionNote: string | null;
    reviewedBy: string | null;
    accessGranted: boolean;
}

// 요청 생성 타입
export interface CreateAccessRequest {
    elderlyUserId: number;
    scope: AccessScope;
    reason: string;
}

// 승인 요청 타입
export interface ApproveAccessRequest {
    accessRequestId: number;
    expiresAt?: string; // ISO DateTime string
    note?: string;
}

// 거절 요청 타입
export interface RejectAccessRequest {
    accessRequestId: number;
    reason: string;
}

// 철회 요청 타입
export interface RevokeAccessRequest {
    accessRequestId: number;
    reason?: string;
}

// 대기 통계 타입
export interface PendingStats {
    total: number;
    pending: number;
    documentsVerified: number;
}

/**
 * 내 요청 목록 조회 (보호자)
 * GET /api/access-requests/my
 */
export const getMyRequests = async (): Promise<AccessRequestSummary[]> => {
    const response = await apiClient.get<AccessRequestSummary[]>('/api/access-requests/my');
    return response.data;
};

/**
 * 접근 권한 요청 생성 (보호자)
 * POST /api/access-requests
 */
export const createRequest = async (request: CreateAccessRequest): Promise<AccessRequestResponse> => {
    const response = await apiClient.post<AccessRequestResponse>('/api/access-requests', request);
    return response.data;
};

/**
 * 요청 취소 (보호자)
 * DELETE /api/access-requests/{requestId}
 */
export const cancelRequest = async (requestId: number): Promise<void> => {
    await apiClient.delete(`/api/access-requests/${requestId}`);
};

// ==================
// 관리자용 API
// ==================

/**
 * 대기 중인 요청 목록 조회 (관리자)
 * GET /api/access-requests/pending
 */
export const getPendingRequests = async (): Promise<AccessRequestSummary[]> => {
    const response = await apiClient.get<AccessRequestSummary[]>('/api/access-requests/pending');
    return response.data;
};

/**
 * 서류 확인 완료된 대기 요청 목록 (관리자)
 * GET /api/access-requests/pending/verified
 */
export const getVerifiedPendingRequests = async (): Promise<AccessRequestSummary[]> => {
    const response = await apiClient.get<AccessRequestSummary[]>('/api/access-requests/pending/verified');
    return response.data;
};

/**
 * 대기 중인 요청 통계 (관리자)
 * GET /api/access-requests/pending/stats
 */
export const getPendingStats = async (): Promise<PendingStats> => {
    const response = await apiClient.get<PendingStats>('/api/access-requests/pending/stats');
    return response.data;
};

/**
 * 요청 상세 조회 (관리자)
 * GET /api/access-requests/{requestId}
 */
export const getRequestDetail = async (requestId: number): Promise<AccessRequestResponse> => {
    const response = await apiClient.get<AccessRequestResponse>(`/api/access-requests/${requestId}`);
    return response.data;
};

/**
 * 서류 확인 완료 처리 (관리자)
 * POST /api/access-requests/{requestId}/verify-documents
 */
export const verifyDocuments = async (requestId: number): Promise<AccessRequestResponse> => {
    const response = await apiClient.post<AccessRequestResponse>(`/api/access-requests/${requestId}/verify-documents`);
    return response.data;
};

/**
 * 접근 권한 승인 (관리자)
 * POST /api/access-requests/{requestId}/approve
 */
export const approveRequest = async (requestId: number, request?: ApproveAccessRequest): Promise<AccessRequestResponse> => {
    const response = await apiClient.post<AccessRequestResponse>(`/api/access-requests/${requestId}/approve`, request || {});
    return response.data;
};

/**
 * 접근 권한 거절 (관리자)
 * POST /api/access-requests/{requestId}/reject
 */
export const rejectRequest = async (requestId: number, request: RejectAccessRequest): Promise<AccessRequestResponse> => {
    const response = await apiClient.post<AccessRequestResponse>(`/api/access-requests/${requestId}/reject`, request);
    return response.data;
};

/**
 * 접근 권한 철회 (관리자)
 * POST /api/access-requests/{requestId}/revoke
 */
export const revokeAccess = async (requestId: number, request?: RevokeAccessRequest): Promise<AccessRequestResponse> => {
    const response = await apiClient.post<AccessRequestResponse>(`/api/access-requests/${requestId}/revoke`, request || {});
    return response.data;
};

// ==================
// 어르신용 API
// ==================

/**
 * 나에 대한 접근 요청 목록 조회 (어르신)
 * GET /api/access-requests/for-me
 */
export const getRequestsForMe = async (): Promise<AccessRequestSummary[]> => {
    const response = await apiClient.get<AccessRequestSummary[]>('/api/access-requests/for-me');
    return response.data;
};

/**
 * 접근 권한 철회 (어르신)
 * POST /api/access-requests/{requestId}/revoke-by-elderly
 */
export const revokeAccessByElderly = async (requestId: number, request?: RevokeAccessRequest): Promise<AccessRequestResponse> => {
    const response = await apiClient.post<AccessRequestResponse>(`/api/access-requests/${requestId}/revoke-by-elderly`, request || {});
    return response.data;
};

/**
 * 접근 권한 확인
 * GET /api/access-requests/check
 */
export const checkAccess = async (elderlyUserId: number, scope: AccessScope): Promise<boolean> => {
    const response = await apiClient.get<boolean>('/api/access-requests/check', {
        params: { elderlyUserId, scope }
    });
    return response.data;
};

export default {
    getMyRequests,
    createRequest,
    cancelRequest,
    // Admin APIs
    getPendingRequests,
    getVerifiedPendingRequests,
    getPendingStats,
    getRequestDetail,
    verifyDocuments,
    approveRequest,
    rejectRequest,
    revokeAccess,
    // Elderly APIs
    getRequestsForMe,
    revokeAccessByElderly,
    checkAccess,
};
