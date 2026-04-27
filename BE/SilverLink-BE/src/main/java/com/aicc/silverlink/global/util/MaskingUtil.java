package com.aicc.silverlink.global.util;

public class MaskingUtil {
    // 이름 마스킹 (2자: 홍*, 3자: 홍*동, 4자 이상: 홍**동)
    public static String maskName(String name) {
        if (name == null || name.length() < 2) return name;
        if (name.length() == 2) return name.substring(0, 1) + "*";
        return name.substring(0, 1) + "*".repeat(name.length() - 2) + name.substring(name.length() - 1);
    }

    // 전화번호 마스킹 (010-1234-5678 -> 010-****-5678)
    public static String maskPhone(String phone) {
        if (phone == null || !phone.contains("-")) return phone;
        String[] parts = phone.split("-");
        return parts[0] + "-****-" + parts[2];
    }
}