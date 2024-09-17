package io.andy.shorten_url.util;

import jakarta.servlet.http.HttpServletRequest;

public class ClientMapper {
    public static String parseClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.length() == 0) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.length() == 0) {
            ip = request.getHeader("Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0) {
            ip = request.getHeader("WL-Proxy-Client-IP");
        }
        if (ip == null || ip.length() == 0) {
            ip = request.getHeader("HTTP_CLIENT_IP");
        }
        if (ip == null || ip.length() == 0) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }
        if (ip == null || ip.length() == 0) {
            ip = request.getRemoteAddr();
        }
        if (ip.equals("0:0:0:0:0:0:0:1")) {
            ip = "127.0.0.1";
        }
        return ip;
    }

    public static String parseUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        if (userAgent.contains("Chrome")) {
            return "chrome";
        } else if (userAgent.contains("Edge")) {
            return "edge";
        } else if (userAgent.contains("Whale")) {
            return "whale";
        } else if (userAgent.contains("Firefox") || userAgent.contains("Mozilla")) {
            return "firefox";
        } else if (userAgent.contains("Safari")) {
            return "safari";
        } else if (userAgent.contains("Opera")) {
            return "opera";
        } else if (userAgent.contains("Postman")) {
            return "postman";
        }
        return userAgent;
    }

    public static String parseLocale(HttpServletRequest request) {
        return request.getLocale().getCountry();
    }

    public static String parseReferer(HttpServletRequest request) {
        return request.getHeader("Referer");
    }
}
