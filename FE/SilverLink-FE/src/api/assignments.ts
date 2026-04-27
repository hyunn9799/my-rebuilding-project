import apiClient from './index';
import { PageResponse } from '@/types/api';

// 배정 관련 타입 (백엔드 AssignmentResponse와 일치)
export interface AssignmentResponse {
    assignmentId: number;
    counselorId: number;
    counselorName: string;
    elderlyId: number;
    elderlyName: string;
    assignedByAdminName: string;
    status: 'ACTIVE' | 'ENDED';
    assignedAt: string;
    endedAt: string | null;
}

export interface AssignmentRequest {
    counselorId: number;
    elderlyId: number;
}

export interface CounselorCapacity {
    id: number;
    name: string;
    email: string;
    assignedCount: number;
    capacity: number;
    region: string;
}

/**
 * 상담사-어르신 배정
 * POST /api/assignments
 */
export const assignCounselor = async (request: AssignmentRequest): Promise<AssignmentResponse> => {
    const response = await apiClient.post<AssignmentResponse>('/api/assignments', request);
    return response.data;
};

/**
 * 배정 해제
 * POST /api/assignments/unassign
 */
export const unassignCounselor = async (counselorId: number, elderlyId: number): Promise<void> => {
    await apiClient.post('/api/assignments/unassign', null, {
        params: { counselorId, elderlyId }
    });
};

/**
 * 상담사 본인 배정 현황 조회
 * GET /api/assignments/counselor/me
 */
export const getMyAssignments = async (): Promise<AssignmentResponse[]> => {
    const response = await apiClient.get<AssignmentResponse[]>('/api/assignments/counselor/me');
    return response.data;
};

/**
 * 관리자: 특정 상담사의 배정 현황 조회
 * GET /api/assignments/admin/counselors/{counselorId}
 */
export const getCounselorAssignments = async (counselorId: number): Promise<AssignmentResponse[]> => {
    const response = await apiClient.get<AssignmentResponse[]>(`/api/assignments/admin/counselors/${counselorId}`);
    return response.data;
};

/**
 * 관리자: 특정 어르신의 배정 현황 조회
 * GET /api/assignments/admin/elderly/{elderlyId}
 */
export const getElderlyAssignment = async (elderlyId: number): Promise<AssignmentResponse> => {
    const response = await apiClient.get<AssignmentResponse>(`/api/assignments/admin/elderly/${elderlyId}`);
    return response.data;
};

export default {
    assignCounselor,
    unassignCounselor,
    getMyAssignments,
    getCounselorAssignments,
    getElderlyAssignment,
};
