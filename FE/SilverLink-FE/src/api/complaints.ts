import apiClient from './index';
import { PageResponse } from '@/types/api';

// 민원 관련 타입
export interface ComplaintResponse {
    id: number;
    title: string;
    content: string;
    category: string | null;
    status: 'WAITING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED';
    createdAt: string;
    response: string | null;
    respondedAt: string | null;
    respondedByName: string | null;
}

export interface ComplaintRequest {
    title: string;
    content: string;
    category?: string;
}

export interface ComplaintStats {
    pending: number;
    processing: number;
    resolved: number;
    total: number;
}

/**
 * 민원 등록
 * POST /api/complaints
 */
export const createComplaint = async (request: ComplaintRequest): Promise<ComplaintResponse> => {
    const response = await apiClient.post<ComplaintResponse>('/api/complaints', request);
    return response.data;
};

/**
 * 내 민원 목록 조회
 * GET /api/complaints/my
 */
export const getMyComplaints = async (): Promise<ComplaintResponse[]> => {
    const response = await apiClient.get<ComplaintResponse[]>('/api/complaints/my');
    return response.data;
};

/**
 * 민원 상세 조회
 * GET /api/complaints/{id}
 */
export const getComplaintDetail = async (id: number): Promise<ComplaintResponse> => {
    const response = await apiClient.get<ComplaintResponse>(`/api/complaints/${id}`);
    return response.data;
};

/**
 * 민원 통계 조회
 * GET /api/complaints/my/stats
 */
export const getMyComplaintStats = async (): Promise<ComplaintStats> => {
    const response = await apiClient.get<ComplaintStats>('/api/complaints/my/stats');
    return response.data;
};

// ==================
// 관리자용 API
// ==================

/**
 * 전체 민원 목록 조회 (관리자)
 * GET /api/admin/complaints
 */
export const getAllComplaints = async (params?: { page?: number; size?: number }): Promise<PageResponse<ComplaintResponse>> => {
    const response = await apiClient.get<PageResponse<ComplaintResponse>>('/api/admin/complaints', { params });
    return response.data;
};

/**
 * 상태별 민원 조회 (관리자)
 * GET /api/admin/complaints/status/{status}
 */
export const getComplaintsByStatus = async (
    status: 'WAITING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED',
    params?: { page?: number; size?: number }
): Promise<PageResponse<ComplaintResponse>> => {
    const response = await apiClient.get<PageResponse<ComplaintResponse>>(`/api/admin/complaints/status/${status}`, { params });
    return response.data;
};

/**
 * 민원 답변 (관리자)
 * POST /api/admin/complaints/{id}/reply
 */
export const replyToComplaint = async (id: number, replyContent: string): Promise<ComplaintResponse> => {
    const response = await apiClient.post<ComplaintResponse>(`/api/admin/complaints/${id}/reply`, { replyContent });
    return response.data;
};

/**
 * 민원 상태 변경 (관리자)
 * PATCH /api/admin/complaints/{id}/status
 */
export const updateComplaintStatus = async (
    id: number,
    status: 'WAITING' | 'PROCESSING' | 'RESOLVED' | 'REJECTED'
): Promise<ComplaintResponse> => {
    const response = await apiClient.patch<ComplaintResponse>(`/api/admin/complaints/${id}/status`, null, {
        params: { status }
    });
    return response.data;
};

export default {
    createComplaint,
    getMyComplaints,
    getComplaintDetail,
    getMyComplaintStats,
    getAllComplaints,
    getComplaintsByStatus,
    replyToComplaint,
    updateComplaintStatus,
};
