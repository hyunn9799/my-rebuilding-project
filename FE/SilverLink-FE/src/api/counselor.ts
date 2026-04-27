import apiClient from './index';
import { CounselorRequest, CounselorResponse } from '@/types/api';

export const counselorApi = {
    // 상담사 등록
    registerCounselor: async (data: CounselorRequest): Promise<CounselorResponse> => {
        const response = await apiClient.post('/api/counselors', data);
        return response.data;
    },

    // 상담사 목록 조회 (관리자용)
    getAllCounselors: async (): Promise<CounselorResponse[]> => {
        const response = await apiClient.get('/api/counselors');
        return response.data;
    },

    // 상담사 상세 조회 (관리자용)
    getCounselor: async (id: number): Promise<CounselorResponse> => {
        const response = await apiClient.get(`/api/counselors/admin/${id}`);
        return response.data;
    }
};
