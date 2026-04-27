import { AxiosError } from 'axios';

/**
 * API 에러 응답에서 사용자 친화적인 메시지를 추출합니다.
 * 백엔드 GlobalExceptionHandler에서 반환하는 형식을 파싱합니다.
 */
export interface ApiErrorResponse {
    error?: string;
    message?: string;
    code?: string;
}

/**
 * Axios 에러에서 사용자에게 표시할 메시지를 추출합니다.
 */
export const getErrorMessage = (error: unknown, defaultMessage: string = '오류가 발생했습니다.'): string => {
    if (!error) return defaultMessage;

    // Axios 에러인 경우
    if (isAxiosError(error)) {
        const data = error.response?.data as ApiErrorResponse;

        // 백엔드에서 message 필드로 한글 메시지를 반환하는 경우
        if (data?.message) {
            return data.message;
        }

        // error 필드가 있는 경우 (코드일 수 있음)
        if (data?.error && typeof data.error === 'string') {
            // 영문 코드인 경우 기본 메시지 반환
            if (data.error === data.error.toUpperCase() && data.error.includes('_')) {
                return defaultMessage;
            }
            return data.error;
        }

        // 응답이 문자열인 경우
        if (typeof data === 'string') {
            return data;
        }

        // HTTP 상태 코드에 따른 기본 메시지
        const status = error.response?.status;
        if (status) {
            return getStatusMessage(status);
        }
    }

    // 일반 Error 객체인 경우
    if (error instanceof Error) {
        return error.message || defaultMessage;
    }

    return defaultMessage;
};

/**
 * HTTP 상태 코드에 따른 기본 메시지 반환
 */
export const getStatusMessage = (status: number): string => {
    switch (status) {
        case 400:
            return '잘못된 요청입니다. 입력 값을 확인해주세요.';
        case 401:
            return '로그인이 필요합니다.';
        case 403:
            return '접근 권한이 없습니다.';
        case 404:
            return '요청한 정보를 찾을 수 없습니다.';
        case 409:
            return '중복된 데이터가 존재합니다.';
        case 422:
            return '입력 값이 유효하지 않습니다.';
        case 429:
            return '요청이 너무 많습니다. 잠시 후 다시 시도해주세요.';
        case 500:
            return '서버 오류가 발생했습니다. 잠시 후 다시 시도해주세요.';
        case 502:
        case 503:
        case 504:
            return '서버에 연결할 수 없습니다. 잠시 후 다시 시도해주세요.';
        default:
            return '오류가 발생했습니다.';
    }
};

/**
 * Axios 에러인지 확인하는 타입 가드
 */
export const isAxiosError = (error: unknown): error is AxiosError => {
    return (
        error !== null &&
        typeof error === 'object' &&
        'isAxiosError' in error &&
        (error as AxiosError).isAxiosError === true
    );
};

/**
 * 에러 코드에 따른 특정 동작이 필요한지 확인
 */
export const isAuthError = (error: unknown): boolean => {
    if (!isAxiosError(error)) return false;

    const status = error.response?.status;
    if (status === 401) return true;

    const data = error.response?.data as ApiErrorResponse;
    const errorCode = data?.error;

    return errorCode === 'SESSION_EXPIRED' ||
        errorCode === 'REFRESH_REUSED' ||
        errorCode === 'INVALID_TOKEN' ||
        errorCode === 'TOKEN_EXPIRED';
};

/**
 * 네트워크 에러인지 확인
 */
export const isNetworkError = (error: unknown): boolean => {
    if (!isAxiosError(error)) return false;
    return !error.response && error.code === 'ERR_NETWORK';
};

export default {
    getErrorMessage,
    getStatusMessage,
    isAxiosError,
    isAuthError,
    isNetworkError,
};
