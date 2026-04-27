import apiClient from './index';
import { CounselorResponse, CounselorRequest } from '@/types/api';

/**
 * 상담사 등록 (관리자 전용)
 * POST /api/counselors
 */
export const register = async (data: CounselorRequest): Promise<CounselorResponse> => {
    const response = await apiClient.post<CounselorResponse>('/api/counselors', data);
    return response.data;
};

/**
 * 내 정보 조회 (상담사)
 * GET /api/counselors/me
 */
export const getMyInfo = async (): Promise<CounselorResponse> => {
    const response = await apiClient.get<CounselorResponse>('/api/counselors/me');
    return response.data;
};

/**
 * 상담사 정보 조회 (관리자용)
 * GET /api/counselors/admin/{id}
 */
export const getCounselorByAdmin = async (id: number): Promise<CounselorResponse> => {
    const response = await apiClient.get<CounselorResponse>(`/api/counselors/admin/${id}`);
    return response.data;
};

/**
 * 모든 상담사 목록 조회 (관리자용)
 * GET /api/counselors
 */
export const getAllCounselors = async (): Promise<CounselorResponse[]> => {
    const response = await apiClient.get<CounselorResponse[]>('/api/counselors');
    return response.data;
};

export default {
    register,
    getMyInfo,
    getCounselorByAdmin,
    getAllCounselors,
};
