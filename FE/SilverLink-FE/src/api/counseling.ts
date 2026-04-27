import apiClient from './index';
import { CounselingRecordResponse, CounselingRecordRequest } from '@/types/api';

/**
 * 상담 기록 생성
 * POST /api/counseling-records
 */
export const createRecord = async (data: CounselingRecordRequest): Promise<CounselingRecordResponse> => {
    const response = await apiClient.post<CounselingRecordResponse>('/api/counseling-records', data);
    return response.data;
};

/**
 * 내 상담 기록 조회
 * GET /api/counseling-records/me
 */
export const getMyRecords = async (): Promise<CounselingRecordResponse[]> => {
    const response = await apiClient.get<CounselingRecordResponse[]>('/api/counseling-records/me');
    return response.data;
};

/**
 * 어르신별 상담 기록 조회
 * GET /api/counseling-records/elderly/{elderlyId}
 */
export const getRecordsByElderly = async (elderlyId: number): Promise<CounselingRecordResponse[]> => {
    const response = await apiClient.get<CounselingRecordResponse[]>(`/api/counseling-records/elderly/${elderlyId}`);
    return response.data;
};

/**
 * 상담 기록 수정
 * PUT /api/counseling-records/{recordId}
 */
export const updateRecord = async (recordId: number, data: CounselingRecordRequest): Promise<CounselingRecordResponse> => {
    const response = await apiClient.put<CounselingRecordResponse>(`/api/counseling-records/${recordId}`, data);
    return response.data;
};

export default {
    createRecord,
    getMyRecords,
    getRecordsByElderly,
    updateRecord,
};
