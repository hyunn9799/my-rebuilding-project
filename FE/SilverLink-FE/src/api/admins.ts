import apiClient from './index';
import {
    AdminResponse,
    AdminCreateRequest,
    AdminUpdateRequest,
    AuditLogResponse,
    RegisterElderlyRequest,
    RegisterGuardianRequest
} from '@/types/api';

/**
 * 관리자 생성
 * POST /api/admins
 */
export const createAdmin = async (data: AdminCreateRequest): Promise<AdminResponse> => {
    const response = await apiClient.post<AdminResponse>('/api/admins', data);
    return response.data;
};

// ... (omitted unchanged parts)

/**
 * 어르신 오프라인 등록
 * POST /api/admin/members/elderly
 */
export const registerElderly = async (data: RegisterElderlyRequest): Promise<number> => {
    const response = await apiClient.post<number>('/api/admin/members/elderly', data);
    return response.data;
};

/**
 * 보호자 오프라인 등록
 * POST /api/admin/members/guardian
 */
export const registerGuardian = async (data: RegisterGuardianRequest): Promise<number> => {
    const response = await apiClient.post<number>('/api/admin/members/guardian', data);
    return response.data;
};

/**
 * 관리자 정보 조회
 * GET /api/admins/{userId}
 */
export const getAdmin = async (userId: number): Promise<AdminResponse> => {
    const response = await apiClient.get<AdminResponse>(`/api/admins/${userId}`);
    return response.data;
};

/**
 * 관리자 목록 조회
 * GET /api/admins
 */
export const getAdmins = async (params?: {
    admCode?: number;
    level?: 'NATIONAL' | 'PROVINCIAL' | 'CITY' | 'DISTRICT';
}): Promise<AdminResponse[]> => {
    const response = await apiClient.get<AdminResponse[]>('/api/admins', { params });
    return response.data;
};

/**
 * 상위 관리자 조회
 * GET /api/admins/supervisors
 */
export const getSupervisors = async (admCode: number): Promise<AdminResponse[]> => {
    const response = await apiClient.get<AdminResponse[]>('/api/admins/supervisors', {
        params: { admCode },
    });
    return response.data;
};

/**
 * 하위 관리자 조회
 * GET /api/admins/{userId}/subordinates
 */
export const getSubordinates = async (userId: number): Promise<AdminResponse[]> => {
    const response = await apiClient.get<AdminResponse[]>(`/api/admins/${userId}/subordinates`);
    return response.data;
};

/**
 * 관리자 권한 확인
 * GET /api/admins/{userId}/jurisdiction
 */
export const checkJurisdiction = async (userId: number, targetCode: number): Promise<boolean> => {
    const response = await apiClient.get<boolean>(`/api/admins/${userId}/jurisdiction`, {
        params: { targetCode },
    });
    return response.data;
};

/**
 * 관리자 정보 수정
 * PUT /api/admins/{userId}
 */
export const updateAdmin = async (userId: number, data: AdminUpdateRequest): Promise<AdminResponse> => {
    const response = await apiClient.put<AdminResponse>(`/api/admins/${userId}`, data);
    return response.data;
};

/**
 * 관리자 삭제
 * DELETE /api/admins/{userId}
 */
export const deleteAdmin = async (userId: number): Promise<void> => {
    await apiClient.delete(`/api/admins/${userId}`);
};

/**
// (Moved to top with proper types)

/**
 * 내 정보 조회 (관리자)
 * GET /api/admins/me
 */
export const getMyInfo = async (): Promise<AdminResponse> => {
    const response = await apiClient.get<AdminResponse>('/api/admins/me');
    return response.data;
};

/**
 * 감사 로그 조회 (관리자)
 * GET /api/admin/audit-logs
 */
export const getAuditLogs = async (params?: {
    action?: string;
    actorId?: number;
    size?: number;
}): Promise<{ content: AuditLogResponse[] }> => {
    const response = await apiClient.get<{ content: AuditLogResponse[] }>('/api/admin/audit-logs', { params });
    return response.data;
};

export default {
    createAdmin,
    getAdmin,
    getAdmins,
    getSupervisors,
    getSubordinates,
    checkJurisdiction,
    updateAdmin,
    deleteAdmin,
    registerElderly,
    registerGuardian,
    getMyInfo,
    getAuditLogs,
};

