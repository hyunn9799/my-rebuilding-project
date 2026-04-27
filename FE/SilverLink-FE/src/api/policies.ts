import apiClient from './index';

// ==================
// Policy 타입 정의
// ==================

export type PolicyType =
    | 'TERMS_OF_SERVICE'
    | 'PRIVACY_POLICY'
    | 'SENSITIVE_INFO_CONSENT'
    | 'THIRD_PARTY_PROVISION_CONSENT'
    | 'LOCATION_BASED_SERVICE'
    | 'WELFARE_BENEFITS_NOTIFICATION';

export interface PolicyResponse {
    id: number;
    policyType: PolicyType;
    policyName: string;
    version: string;
    content: string;
    isMandatory: boolean;
    description?: string;
    createdAt: string;
    updatedAt: string;
}

export interface PolicyRequest {
    policyType: PolicyType;
    version: string;
    content: string;
    isMandatory: boolean;
    description?: string;
}

// ==================
// API 함수
// ==================

/**
 * 관리자: 전체 정책 목록 조회
 * GET /api/policies
 */
export const getAllPolicies = async (): Promise<PolicyResponse[]> => {
    const response = await apiClient.get<PolicyResponse[]>('/api/policies');
    return response.data;
};

/**
 * 최신 정책 조회 (로그인 불필요)
 * GET /api/policies/latest/{type}
 */
export const getLatestPolicy = async (type: PolicyType): Promise<PolicyResponse> => {
    const response = await apiClient.get<PolicyResponse>(`/api/policies/latest/${type}`);
    return response.data;
};

/**
 * 관리자: 정책 생성
 * POST /api/policies
 */
export const createPolicy = async (request: PolicyRequest): Promise<PolicyResponse> => {
    const response = await apiClient.post<PolicyResponse>('/api/policies', request);
    return response.data;
};

export default {
    getAllPolicies,
    getLatestPolicy,
    createPolicy,
};

