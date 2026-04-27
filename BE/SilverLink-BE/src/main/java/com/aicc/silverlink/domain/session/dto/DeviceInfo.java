package com.aicc.silverlink.domain.session.dto;

import jakarta.servlet.http.HttpServletRequest;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

/**
 * 디바이스 식별 정보.
 * HttpServletRequest에서 IP, User-Agent를 추출하고
 * SHA-256 해싱으로 고유한 deviceId를 생성합니다.
 */
public record DeviceInfo(
        String ipAddress,
        String userAgent,
        String deviceId) {
    /**
     * HttpServletRequest에서 DeviceInfo를 추출합니다.
     * X-Forwarded-For 헤더를 우선 확인하여 프록시 뒤의 실제 IP를 가져옵니다.
     */
    public static DeviceInfo from(HttpServletRequest request) {
        String ip = resolveClientIp(request);
        String ua = request.getHeader("User-Agent");
        if (ua == null)
            ua = "unknown";

        String deviceId = sha256(ip + "|" + ua);
        return new DeviceInfo(ip, ua, deviceId);
    }

    /**
     * IP 주소를 마스킹합니다 (보안 목적, 응답용).
     * 예: 192.168.1.100 → 192.168.1.***
     */
    public String maskedIp() {
        if (ipAddress == null)
            return "unknown";
        int lastDot = ipAddress.lastIndexOf('.');
        if (lastDot < 0)
            return ipAddress;
        return ipAddress.substring(0, lastDot) + ".***";
    }

    /**
     * User-Agent를 요약합니다 (응답용).
     * 예: "Mozilla/5.0 (Windows NT 10.0; Win64; x64) Chrome/120.0" → "Chrome/120.0
     * (Windows)"
     */
    public String deviceSummary() {
        if (userAgent == null || userAgent.equals("unknown"))
            return "알 수 없는 기기";

        String browser = "Unknown Browser";
        String os = "Unknown OS";

        if (userAgent.contains("Chrome") && !userAgent.contains("Edg")) {
            browser = extractToken(userAgent, "Chrome/");
        } else if (userAgent.contains("Edg")) {
            browser = extractToken(userAgent, "Edg/");
        } else if (userAgent.contains("Firefox")) {
            browser = extractToken(userAgent, "Firefox/");
        } else if (userAgent.contains("Safari") && !userAgent.contains("Chrome")) {
            browser = extractToken(userAgent, "Safari/");
        }

        if (userAgent.contains("Windows"))
            os = "Windows";
        else if (userAgent.contains("Mac OS"))
            os = "Mac";
        else if (userAgent.contains("Linux"))
            os = "Linux";
        else if (userAgent.contains("Android"))
            os = "Android";
        else if (userAgent.contains("iPhone") || userAgent.contains("iPad"))
            os = "iOS";

        return browser + " (" + os + ")";
    }

    private static String resolveClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private static String extractToken(String ua, String prefix) {
        int idx = ua.indexOf(prefix);
        if (idx < 0)
            return prefix.replace("/", "");
        String after = ua.substring(idx);
        int space = after.indexOf(' ');
        return space > 0 ? after.substring(0, space) : after;
    }

    private static String sha256(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(raw.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder();
            for (byte b : digest)
                sb.append(String.format("%02x", b));
            return sb.toString();
        } catch (Exception e) {
            throw new RuntimeException("Hashing failed", e);
        }
    }
}
