import { apiClient } from "./index";
import { MyProfileResponse, UpdateMyProfileRequest } from "@/types/api";

export const userApi = {
    // 내 정보 조회
    getUserProfile: async (): Promise<MyProfileResponse> => {
        const response = await apiClient.get<MyProfileResponse>('/api/users/me');
        return response.data;
    },

    // 내 정보 수정
    updateUserProfile: async (data: UpdateMyProfileRequest): Promise<MyProfileResponse> => {
        const response = await apiClient.patch<MyProfileResponse>('/api/users/me', data);
        return response.data;
    }
};
