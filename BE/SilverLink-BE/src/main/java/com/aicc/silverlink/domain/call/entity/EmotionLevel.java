package com.aicc.silverlink.domain.call.entity;

/**
 * 감정 레벨 Enum
 */
public enum EmotionLevel {
    GOOD("좋음"),
    NORMAL("보통"),
    BAD("나쁨"),
    DEPRESSED("우울");

    private final String korean;

    EmotionLevel(String korean) {
        this.korean = korean;
    }

    public String getKorean() {
        return korean;
    }
}