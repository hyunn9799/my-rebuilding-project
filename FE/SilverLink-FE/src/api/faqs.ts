import { apiClient } from './index';
import { FaqResponse } from '@/types/api';

/**
 * FAQ API
 */
export const faqsApi = {
    /**
     * FAQ 목록 조회
     * GET /api/faqs
     * @param category - 카테고리 (선택)
     * @param keyword - 검색 키워드 (선택)
     */
    getFaqs: async (category?: string, keyword?: string): Promise<FaqResponse[]> => {
        const params: { category?: string; keyword?: string } = {};
        if (category) params.category = category;
        if (keyword) params.keyword = keyword;

        const response = await apiClient.get('/api/faqs', { params });
        return response.data;
    },
};

export default faqsApi;
