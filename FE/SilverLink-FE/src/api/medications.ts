import apiClient from './index';

// 복약 관련 타입
export interface MedicationResponse {
    id: number;
    name: string;
    dosage: string | null;
    times: string[];
    reminder: boolean;
    startDate: string | null;
    endDate: string | null;
    instructions: string | null;
}

export interface MedicationRequest {
    medicationName: string;
    dosageText?: string;
    times: string[];  // "morning", "noon", "evening", "night"
    instructions?: string;
    startDate?: string;
    endDate?: string;
    reminder?: boolean;
}

/**
 * 복약 일정 등록
 * POST /api/medications
 */
export const createMedication = async (request: MedicationRequest): Promise<MedicationResponse> => {
    const response = await apiClient.post<MedicationResponse>('/api/medications', request);
    return response.data;
};

/**
 * 내 복약 일정 목록 조회
 * GET /api/medications/my
 */
export const getMyMedications = async (): Promise<MedicationResponse[]> => {
    const response = await apiClient.get<MedicationResponse[]>('/api/medications/my');
    return response.data;
};

/**
 * 복약 일정 상세 조회
 * GET /api/medications/{id}
 */
export const getMedicationDetail = async (id: number): Promise<MedicationResponse> => {
    const response = await apiClient.get<MedicationResponse>(`/api/medications/${id}`);
    return response.data;
};

/**
 * 복약 일정 삭제
 * DELETE /api/medications/{id}
 */
export const deleteMedication = async (id: number): Promise<void> => {
    await apiClient.delete(`/api/medications/${id}`);
};

/**
 * 알림 토글
 * PATCH /api/medications/{id}/reminder
 */
export const toggleReminder = async (id: number): Promise<MedicationResponse> => {
    const response = await apiClient.patch<MedicationResponse>(`/api/medications/${id}/reminder`);
    return response.data;
};

/**
 * 상담사: 어르신 복약 일정 조회
 * GET /api/medications/elderly/{elderlyId}
 */
export const getMedicationsByElderly = async (elderlyId: number): Promise<MedicationResponse[]> => {
    const response = await apiClient.get<MedicationResponse[]>(`/api/medications/elderly/${elderlyId}`);
    return response.data;
};

export default {
    createMedication,
    getMyMedications,
    getMedicationDetail,
    deleteMedication,
    toggleReminder,
    getMedicationsByElderly,
};
