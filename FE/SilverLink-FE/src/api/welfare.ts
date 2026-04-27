import { apiClient } from './index';
import { WelfareListResponse, WelfareDetailResponse, WelfareSearchRequest, PageResponse } from '@/types/api';

/**
 * 복지 서비스 API
 */
export const welfareApi = {
    /**
     * 복지 서비스 검색 및 목록 조회
     * GET /api/welfare
     */
    searchWelfare: async (
        params: WelfareSearchRequest & { page?: number; size?: number }
    ): Promise<PageResponse<WelfareListResponse>> => {
        const response = await apiClient.get('/api/welfare', { params });
        return response.data;
    },

    /**
     * 복지 서비스 상세 조회
     * GET /api/welfare/{welfareId}
     */
    getWelfareDetail: async (welfareId: number): Promise<WelfareDetailResponse> => {
        const response = await apiClient.get(`/api/welfare/${welfareId}`);
        return response.data;
    },

    /**
     * 데이터 수동 동기화 (관리자)
     * POST /api/welfare/sync/manual
     */
    manualSync: async (): Promise<string> => {
        const response = await apiClient.post('/api/welfare/sync/manual');
        return response.data;
    },
};

export default welfareApi;
