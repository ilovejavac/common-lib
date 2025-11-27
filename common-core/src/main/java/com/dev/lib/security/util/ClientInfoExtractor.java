package com.dev.lib.security.util;

import jakarta.servlet.http.HttpServletRequest;

public abstract class ClientInfoExtractor {
    private ClientInfoExtractor() {}

    public static String getClientIp(HttpServletRequest request) {
        String[] headers = {
                "X-Forwarded-For",
                "X-Real-IP",
                "Proxy-Client-IP",
                "WL-Proxy-Client-IP",
                "HTTP_X_FORWARDED_FOR",
                "HTTP_X_FORWARDED",
                "HTTP_X_CLUSTER_CLIENT_IP",
                "HTTP_CLIENT_IP",
                "HTTP_FORWARDED_FOR",
                "HTTP_FORWARDED",
                "HTTP_VIA",
                "REMOTE_ADDR"
        };

        for (String header : headers) {
            String ip = request.getHeader(header);
            if (isValidIp(ip)) {
                // 多级代理取第一个
                return ip.contains(",") ? ip.split(",")[0].trim() : ip;
            }
        }

        return request.getRemoteAddr();
    }

    private static boolean isValidIp(String ip) {
        return ip != null
                && !ip.isEmpty()
                && !"unknown".equalsIgnoreCase(ip)
                && !"0:0:0:0:0:0:0:1".equals(ip);
    }

    public static String getClientType(HttpServletRequest request) {
        String explicit = request.getHeader("X-Client-Type");
        if (explicit != null) {
            return explicit.toUpperCase();
        }

        String ua = request.getHeader("User-Agent");
        if (ua == null) return "UNKNOWN";

        ua = ua.toLowerCase();
        if (ua.contains("micromessenger")) return "MINI_PROGRAM";
        if (ua.contains("mobile") || ua.contains("android") || ua.contains("iphone")) return "APP";
        return "WEB";
    }
}