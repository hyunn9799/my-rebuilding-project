import apiClient from './index';

/**
 * 휴대폰 인증 API 모듈
 * 백엔드: PhoneVerificationController (/api/auth/phone)
 */

export type PhoneVerificationPurpose = 'SIGNUP' | 'DEVICE_REGISTRATION' | 'PASSWORD_RESET';

export interface RequestCodeRequest {
    phone: string;
    purpose: PhoneVerificationPurpose;
    userId?: number;
}

export interface RequestCodeResponse {
    verificationId: number;
    expireAt: string; // ISO DateTime
    expiresInSeconds?: number; // 서버에서 계산한 남은 시간(초)
    debugCode?: string; // 개발용
}

export interface VerifyCodeRequest {
    verificationId: number;
    code: string;
}

export interface VerifyCodeResponse {
    verified: boolean;
    proofToken: string;
}

/**
 * 인증번호 요청 (SMS 발송)
 * POST /api/auth/phone/request
 */
export const requestVerificationCode = async (
    phone: string,
    purpose: PhoneVerificationPurpose = 'SIGNUP',
    userId?: number
): Promise<RequestCodeResponse> => {
    const response = await apiClient.post<RequestCodeResponse>('/api/auth/phone/request', {
        phone,
        purpose,
        userId,
    });
    return response.data;
};

/**
 * 인증번호 확인
 * POST /api/auth/phone/verify
 */
export const verifyCode = async (
    verificationId: number,
    code: string
): Promise<VerifyCodeResponse> => {
    const response = await apiClient.post<VerifyCodeResponse>('/api/auth/phone/verify', {
        verificationId,
        code,
    });
    return response.data;
};

export default {
    requestVerificationCode,
    verifyCode,
};
