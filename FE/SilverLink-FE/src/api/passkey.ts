import apiClient from './index';

/**
 * Passkey/WebAuthn 인증 API 모듈
 * 백엔드: PasskeyController (/api/auth/passkey)
 */

export interface StartRegResponse {
    requestId: string;
    creationOptionsJson: string;
}

export interface StartAuthResponse {
    requsetId: string;  // Note: typo in backend ('requsetId' instead of 'requestId')
    assertionRequestJson: string;
}

// Passkey 로그인 응답 - 사용자 프로필 포함
export interface PasskeyLoginResponse {
    accessToken: string;
    expiresInSeconds: number;
    user: {
        id: number;
        name: string;
        phone: string;
        role: string;
    };
}

// Legacy 호환용 (일반 로그인)
export interface TokenResponse {
    accessToken: string;
    expiresIn: number;
    role: string;
}

/**
 * Passkey 등록 시작 (옵션 가져오기)
 * ✅ 보안 강화: userId는 JWT에서 자동 추출되므로 body에 포함하지 않음
 */
export const startPasskeyRegistration = async (): Promise<StartRegResponse> => {
    try {
        console.log('[Passkey] Starting registration - requesting options from server');
        const response = await apiClient.post<StartRegResponse>('/api/auth/passkey/register/options', {});
        console.log('[Passkey] Registration options received:', {
            requestId: response.data.requestId,
            hasCreationOptions: !!response.data.creationOptionsJson
        });
        return response.data;
    } catch (error: any) {
        console.error('[Passkey] Failed to start registration:', {
            status: error.response?.status,
            statusText: error.response?.statusText,
            data: error.response?.data,
            message: error.message
        });
        throw error;
    }
};

/**
 * Passkey 등록 완료 (브라우저 인증 후 검증)
 * ✅ 보안 강화: userId는 JWT에서 자동 추출되므로 body에 포함하지 않음
 */
export const finishPasskeyRegistration = async (
    requestId: string,
    credentialJson: string
): Promise<void> => {
    await apiClient.post('/api/auth/passkey/register/verify', {
        requestId,
        credentialJson,
    });
};

/**
 * Passkey 로그인 시작 (옵션 가져오기)
 */
export const startPasskeyLogin = async (loginId?: string): Promise<StartAuthResponse> => {
    try {
        console.log('[Passkey] Starting login - requesting options from server');
        const response = await apiClient.post<StartAuthResponse>('/api/auth/passkey/login/options', {
            loginId: loginId || null,
        });
        console.log('[Passkey] Login options received:', {
            requestId: response.data.requsetId,
            hasAssertionRequest: !!response.data.assertionRequestJson
        });
        return response.data;
    } catch (error: any) {
        console.error('[Passkey] Failed to start login:', {
            status: error.response?.status,
            statusText: error.response?.statusText,
            data: error.response?.data,
            message: error.message
        });
        throw error;
    }
};

/**
 * Passkey 로그인 완료 (브라우저 인증 후 검증 및 토큰 + 사용자 프로필 발급)
 */
export const finishPasskeyLogin = async (
    requestId: string,
    credentialJson: string
): Promise<PasskeyLoginResponse> => {
    try {
        console.log('[Passkey] Finishing login - verifying credential');
        const response = await apiClient.post<PasskeyLoginResponse>('/api/auth/passkey/login/verify', {
            requestId,
            credentialJson,
        });
        console.log('[Passkey] Login successful:', {
            hasToken: !!response.data.accessToken,
            userId: response.data.user?.id,
            userName: response.data.user?.name
        });
        return response.data;
    } catch (error: any) {
        console.error('[Passkey] Failed to finish login:', {
            status: error.response?.status,
            statusText: error.response?.statusText,
            data: error.response?.data,
            message: error.message
        });
        throw error;
    }
};

export default {
    startPasskeyRegistration,
    finishPasskeyRegistration,
    startPasskeyLogin,
    finishPasskeyLogin,
};
