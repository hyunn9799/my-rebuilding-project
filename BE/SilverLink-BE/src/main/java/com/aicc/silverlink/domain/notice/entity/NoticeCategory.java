package com.aicc.silverlink.domain.notice.entity;

public enum NoticeCategory {
    NOTICE("공지"),
    EVENT("이벤트"),
    NEWS("뉴스"),
    SYSTEM("시스템");

    private final String description;

    NoticeCategory(String description) {
        this.description = description;
    }

    public String getDescription() {
        return description;
    }
}
