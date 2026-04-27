package com.aicc.silverlink.domain.call.entity;

/**
 * 통화 상태 Enum
 */
public enum CallState {
    REQUESTED("요청됨"),
    ANSWERED("응답됨"),
    FAILED("실패"),
    COMPLETED("완료"),
    CANCELLED("취소됨");

    private final String korean;

    CallState(String korean) {
        this.korean = korean;
    }

    public String getKorean() {
        return korean;
    }
}