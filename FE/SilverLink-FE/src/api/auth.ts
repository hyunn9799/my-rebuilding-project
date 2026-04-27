import apiClient, { setAccessToken, getAccessToken } from './index';
import { LoginRequest, TokenResponse, RefreshResponse, SignupRequest } from '@/types/api';

/**
 * 로그인 API
 * POST /api/auth/login
 */
export const signup = async (data: SignupRequest): Promise<number> => {
    const response = await apiClient.post<number>('/api/auth/signup', data);
    return response.data;
};

export const login = async (credentials: LoginRequest): Promise<TokenResponse> => {
    const response = await apiClient.post<TokenResponse>('/api/auth/login', credentials);

    // 토큰 저장
    setAccessToken(response.data.accessToken);

    return response.data;
};

/**
 * 로그인 확인 API (기존 세션 체크)
 * POST /api/auth/login/check
 */
export interface LoginCheckResponse {
    needsConfirmation: boolean;
    loginToken?: string;
    tokenResponse?: TokenResponse;
}

export const checkLogin = async (credentials: LoginRequest): Promise<LoginCheckResponse> => {
    const response = await apiClient.post<LoginCheckResponse>('/api/auth/login/check', credentials);

    // 바로 로그인된 경우 토큰 저장
    if (!response.data.needsConfirmation && response.data.tokenResponse) {
        setAccessToken(response.data.tokenResponse.accessToken);
    }

    return response.data;
};

/**
 * 강제 로그인 API (기존 세션 종료 후 로그인)
 * POST /api/auth/login/force
 */
export const forceLogin = async (loginToken: string): Promise<TokenResponse> => {
    const response = await apiClient.post<TokenResponse>('/api/auth/login/force', { loginToken });

    // 토큰 저장
    setAccessToken(response.data.accessToken);

    return response.data;
};

/**
 * 토큰 갱신 API
 * POST /api/auth/refresh
 */
export const refresh = async (): Promise<RefreshResponse> => {
    const response = await apiClient.post<RefreshResponse>('/api/auth/refresh');

    // 새 토큰 저장
    setAccessToken(response.data.accessToken);

    return response.data;
};

/**
 * 로그아웃 API
 * POST /api/auth/logout
 */
export const logout = async (): Promise<void> => {
    try {
        await apiClient.post('/api/auth/logout');
    } finally {
        // 토큰 삭제
        setAccessToken(null);
    }
};

/**
 * 현재 로그인 상태 확인
 */
export const isAuthenticated = (): boolean => {
    return getAccessToken() !== null;
};

/**
 * 비밀번호 재설정 API
 * POST /api/auth/reset-password
 */
export const resetPassword = async (
    loginId: string,
    proofToken: string,
    newPassword: string
): Promise<void> => {
    await apiClient.post('/api/auth/reset-password', {
        loginId,
        proofToken,
        newPassword,
    });
};

/**
 * 아이디 찾기 API
 * POST /api/auth/find-id
 */
export interface FindIdResponse {
    maskedLoginId: string;
}

export const findId = async (
    name: string,
    proofToken: string
): Promise<FindIdResponse> => {
    const response = await apiClient.post<FindIdResponse>('/api/auth/find-id', {
        name,
        proofToken,
    });
    return response.data;
};

/**
 * 세션 정보 조회 API
 * GET /api/auth/session/info
 */
export interface SessionInfoResponse {
    sid: string;
    lastSeen: number;
    expiresAt: number;
    remainingSeconds: number;
    idleTtl: number;
}

export const getSessionInfo = async (): Promise<SessionInfoResponse> => {
    const response = await apiClient.get<SessionInfoResponse>('/api/auth/session/info');
    return response.data;
};

export default {
    login,
    refresh,
    logout,
    isAuthenticated,
    resetPassword,
    findId,
    getSessionInfo,
};
