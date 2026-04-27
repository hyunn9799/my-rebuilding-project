package com.aicc.silverlink.global.common.dto;

public record CursorRequest(
        Long key,      // 마지막으로 조회된 ID (커서)
        Integer size   // 가져올 개수
) {
    public static final Long NONE_KEY = -1L;

    public CursorRequest {
        if (size == null) size = 10; // 기본 사이즈 설정
    }

    public boolean hasKey() {
        return key != null && !key.equals(NONE_KEY);
    }
}