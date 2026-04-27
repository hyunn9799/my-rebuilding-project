import { apiClient } from './index';
import { WelfareFacilityResponse, WelfareFacilityRequest } from '@/types/api';

/**
 * 사회복지시설(Map) API
 */
export const mapApi = {
    /**
     * 주변 시설 조회
     * GET /api/map/facilities/nearby
     */
    getNearbyFacilities: async (
        lat: number,
        lon: number,
        radius: number = 1
    ): Promise<WelfareFacilityResponse[]> => {
        const response = await apiClient.get('/api/map/facilities/nearby', {
            params: { lat, lon, radius }
        });
        return response.data;
    },

    /**
     * 시설 상세 조회
     * GET /api/map/facilities/{id}
     */
    getFacilityDetail: async (id: number): Promise<WelfareFacilityResponse> => {
        const response = await apiClient.get(`/api/map/facilities/${id}`);
        return response.data;
    },

    // --- 관리자 기능 ---

    /**
     * 전체 시설 조회 (관리자)
     * GET /api/map/facilities
     */
    getAllFacilities: async (): Promise<WelfareFacilityResponse[]> => {
        const response = await apiClient.get('/api/map/facilities');
        return response.data;
    },

    /**
     * 시설 등록 (관리자)
     * POST /api/map/facilities
     */
    createFacility: async (data: WelfareFacilityRequest): Promise<number> => {
        const response = await apiClient.post('/api/map/facilities', data);
        return response.data;
    },

    /**
     * 시설 수정 (관리자)
     * PUT /api/map/facilities/{id}
     */
    updateFacility: async (id: number, data: WelfareFacilityRequest): Promise<WelfareFacilityResponse> => {
        const response = await apiClient.put(`/api/map/facilities/${id}`, data);
        return response.data;
    },

    /**
     * 시설 삭제 (관리자)
     * DELETE /api/map/facilities/{id}
     */
    deleteFacility: async (id: number): Promise<void> => {
        await apiClient.delete(`/api/map/facilities/${id}`);
    },

    /**
     * 시설명 자동완성 검색
     * GET /api/map/facilities/search
     */
    searchFacilities: async (query: string): Promise<WelfareFacilityResponse[]> => {
        const response = await apiClient.get('/api/map/facilities/search', {
            params: { q: query, limit: 10 }
        });
        return response.data;
    }
};

export default mapApi;
