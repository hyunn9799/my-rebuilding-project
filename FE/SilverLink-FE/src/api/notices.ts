import apiClient from './index';
import { NoticeResponse, PageResponse } from '@/types/api';

interface NoticeListParams {
    keyword?: string;
    page?: number;
    size?: number;
}

// 공지사항 생성/수정 요청 타입 (백엔드 NoticeRequest DTO와 일치)
export interface NoticeRequest {
    title: string;
    content: string;
    category: 'NOTICE' | 'EVENT' | 'NEWS' | 'SYSTEM';
    targetMode: 'ALL' | 'ROLE_SET';
    targetRoles?: ('ADMIN' | 'COUNSELOR' | 'GUARDIAN' | 'ELDERLY')[];
    isPriority?: boolean;
    isPopup?: boolean;
    popupStartAt?: string;
    popupEndAt?: string;
    status?: 'DRAFT' | 'PUBLISHED' | 'ARCHIVED' | 'DELETED';
    attachments?: Array<{
        fileName: string;
        originalFileName: string;
        filePath: string;
        fileSize: number;
    }>;
}


/**
 * 공지사항 목록 조회
 * GET /api/notices
 */
export const getNotices = async (params?: NoticeListParams): Promise<PageResponse<NoticeResponse>> => {
    const response = await apiClient.get<PageResponse<NoticeResponse>>('/api/notices', { params });
    return response.data;
};

/**
 * 팝업 공지사항 조회
 * GET /api/notices/popups
 */
export const getPopups = async (): Promise<NoticeResponse[]> => {
    const response = await apiClient.get<NoticeResponse[]>('/api/notices/popups');
    return response.data;
};

/**
 * 공지사항 상세 조회
 * GET /api/notices/{id}
 */
export const getNoticeDetail = async (id: number): Promise<NoticeResponse> => {
    const response = await apiClient.get<NoticeResponse>(`/api/notices/${id}`);
    return response.data;
};

/**
 * 공지사항 읽음 처리
 * POST /api/notices/{id}/read
 */
export const markAsRead = async (id: number): Promise<void> => {
    await apiClient.post(`/api/notices/${id}/read`);
};

// ==================
// 관리자용 API
// ==================

/**
 * 관리자: 공지사항 목록 조회
 * GET /api/admin/notices
 */
export const getAdminNotices = async (params?: NoticeListParams): Promise<PageResponse<NoticeResponse>> => {
    const response = await apiClient.get<PageResponse<NoticeResponse>>('/api/admin/notices', { params });
    return response.data;
};

/**
 * 관리자: 공지사항 생성
 * POST /api/admin/notices
 */
export const createNotice = async (request: NoticeRequest): Promise<number> => {
    const response = await apiClient.post<number>('/api/admin/notices', request);
    return response.data;
};

/**
 * 관리자: 공지사항 상세 조회
 * GET /api/admin/notices/{id}
 */
export const getAdminNoticeDetail = async (id: number): Promise<NoticeResponse> => {
    const response = await apiClient.get<NoticeResponse>(`/api/admin/notices/${id}`);
    return response.data;
};

/**
 * 관리자: 공지사항 삭제
 * DELETE /api/admin/notices/{id}
 */
export const deleteNotice = async (id: number): Promise<void> => {
    await apiClient.delete(`/api/admin/notices/${id}`);
};

/**
 * 관리자: 공지사항 수정
 * PUT /api/admin/notices/{id}
 */
export const updateNotice = async (id: number, request: NoticeRequest): Promise<void> => {
    await apiClient.put(`/api/admin/notices/${id}`, request);
};

/**
 * 공지사항 필독 확인
 * POST /api/notices/{id}/confirm
 */
export const confirmNotice = async (id: number): Promise<void> => {
    await apiClient.post(`/api/notices/${id}/confirm`);
};

/**
 * 관리자: 공지사항 확인자 목록 조회
 * GET /api/admin/notices/{id}/read-status
 */
export interface NoticeConfirmUser {
    userId: number;
    name: string;
    confirmedAt: string;
}

export const getConfirmList = async (id: number): Promise<NoticeConfirmUser[]> => {
    const response = await apiClient.get<NoticeConfirmUser[]>(`/api/admin/notices/${id}/read-status`);
    return response.data;
};

/**
 * 관리자: 공지사항 읽음 통계 조회
 * GET /api/admin/notices/{id}/read-stats
 */
export interface NoticeReadStats {
    readCount: number;
    totalTargetCount: number;
    readPercentage: number;
}

export const getNoticeReadStats = async (id: number): Promise<NoticeReadStats> => {
    const response = await apiClient.get<NoticeReadStats>(`/api/admin/notices/${id}/read-stats`);
    return response.data;
};

/**
 * 관리자: 공지사항 복구
 * POST /api/admin/notices/{id}/restore
 */
export const restoreNotice = async (id: number): Promise<void> => {
    await apiClient.post(`/api/admin/notices/${id}/restore`);
};

export default {
    getNotices,
    getPopups,
    getNoticeDetail,
    markAsRead,
    confirmNotice,
    // Admin APIs
    getAdminNotices,
    createNotice,
    getAdminNoticeDetail,
    deleteNotice,
    updateNotice,
    getConfirmList,
    getNoticeReadStats,
    restoreNotice,
};

