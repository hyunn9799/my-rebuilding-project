package com.aicc.silverlink.global.util;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class DateTimeUtil {

    // 실무 표준 포맷 정의
    public static final String DATE_PATTERN = "yyyy-MM-dd";
    public static final String DATE_TIME_PATTERN = "yyyy-MM-dd HH:mm:ss";
    public static final String KOREAN_DATE_PATTERN = "yyyy년 MM월 dd일";

    /**
     * LocalDateTime -> String (기본: yyyy-MM-dd HH:mm:ss)
     */
    public static String format(LocalDateTime localDateTime) {
        if (localDateTime == null) return "";
        return localDateTime.format(DateTimeFormatter.ofPattern(DATE_TIME_PATTERN));
    }

    /**
     * LocalDateTime -> String (커스텀 패턴)
     */
    public static String format(LocalDateTime localDateTime, String pattern) {
        if (localDateTime == null) return "";
        return localDateTime.format(DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * String -> LocalDateTime
     */
    public static LocalDateTime parse(String dateTimeString, String pattern) {
        if (dateTimeString == null || dateTimeString.isBlank()) return null;
        return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern(pattern));
    }

    /**
     * 상대적 시간 계산 (요구사항 14: "방금 전", "3일 전" 등)
     */
    public static String calculateRelativeTime(LocalDateTime targetTime) {
        if (targetTime == null) return "";

        LocalDateTime now = LocalDateTime.now();
        Duration duration = Duration.between(targetTime, now);
        long seconds = duration.getSeconds();

        if (seconds < 60) return "방금 전";
        if (seconds < 3600) return (seconds / 60) + "분 전";
        if (seconds < 86400) return (seconds / 3600) + "시간 전";
        if (seconds < 2592000) return (seconds / 86400) + "일 전";

        return format(targetTime, DATE_PATTERN); // 한 달 이상은 날짜 표시
    }

}