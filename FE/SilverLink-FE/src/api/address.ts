import apiClient from './index';
import { AddressResponse } from '@/types/api';

export const addressApi = {
    // 시/도 목록 조회
    getSido: async (): Promise<AddressResponse[]> => {
        const response = await apiClient.get('/api/address/sido');
        return response.data;
    },

    // 시/군/구 목록 조회
    getSigungu: async (sidoCode: string): Promise<AddressResponse[]> => {
        const response = await apiClient.get(`/api/address/sigungu?sidoCode=${sidoCode}`);
        return response.data;
    },

    // 읍/면/동 목록 조회
    getDong: async (sidoCode: string, sigunguCode: string): Promise<AddressResponse[]> => {
        const response = await apiClient.get(`/api/address/dong?sidoCode=${sidoCode}&sigunguCode=${sigunguCode}`);
        return response.data;
    },

    // 행정동 코드로 주소 조회 (필요 시)
    getAddress: async (admCode: number): Promise<AddressResponse> => {
        const response = await apiClient.get(`/api/address/${admCode}`);
        return response.data;
    }
};
