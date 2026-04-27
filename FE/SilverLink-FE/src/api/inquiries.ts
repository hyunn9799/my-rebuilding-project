import { apiClient } from './index';
import { InquiryResponse, InquiryRequest } from '@/types/api';

/**
 * 1:1 문의 API
 */
export const inquiriesApi = {
    /**
     * 문의 목록 조회
     * GET /api/inquiries
     */
    getInquiries: async (): Promise<InquiryResponse[]> => {
        const response = await apiClient.get('/api/inquiries');
        return response.data;
    },

    /**
     * 문의 상세 조회
     * GET /api/inquiries/{id}
     */
    getInquiry: async (id: number): Promise<InquiryResponse> => {
        const response = await apiClient.get(`/api/inquiries/${id}`);
        return response.data;
    },

    /**
     * 문의 등록 (보호자)
     * POST /api/inquiries
     */
    createInquiry: async (request: InquiryRequest): Promise<InquiryResponse> => {
        const response = await apiClient.post('/api/inquiries', request);
        return response.data;
    },

    /**
     * 답변 등록 (상담사)
     * POST /api/inquiries/{id}/answer
     */
    registerAnswer: async (id: number, request: InquiryRequest): Promise<void> => {
        await apiClient.post(`/api/inquiries/${id}/answer`, request);
    },
};

export default inquiriesApi;
