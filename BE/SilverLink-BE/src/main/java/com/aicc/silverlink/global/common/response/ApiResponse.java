package com.aicc.silverlink.global.common.response;

import lombok.Builder;

@Builder
public record ApiResponse<T>(
        String status,   // "success" 또는 "error"
        T data,          // 실제 반환할 데이터
        String message   // 에러 발생 시 메시지
) {
    public static <T> ApiResponse<T> success(T data) {
        return ApiResponse.<T>builder()
                .status("success")
                .data(data)
                .build();
    }

    public static ApiResponse<?> error(String message) {
        return ApiResponse.builder()
                .status("error")
                .message(message)
                .build();
    }
}