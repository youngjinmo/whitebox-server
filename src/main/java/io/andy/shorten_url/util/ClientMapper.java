package io.andy.shorten_url.util;

import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.Locale;
import java.util.Objects;

@Slf4j
public class ClientMapper {
    public static String parseClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty()) {
            ip = request.getRemoteAddr();
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("X-Real-IP");
        }
        if (ip == null || ip.isEmpty()) {
            ip = request.getHeader("HTTP_X_FORWARDED_FOR");
        }

        // localhost 에서 동작시 IPv6, IPv4로 변환
        if (ip != null && ip.equals("0:0:0:0:0:0:0:1")) {
            return "127.0.0.1";
        }

        if (ip != null && !ip.isEmpty()) {
            ip = ip.contains(",") ? ip.split(",")[0].trim() : ip;
        }

        return ip;
    }

    public static String parseUserAgent(HttpServletRequest request) {
        String userAgent = request.getHeader("User-Agent");
        String os = parseOS(userAgent);
        String browser = parseBrowser(userAgent);
        // 조건식에 의해 os, browser가 정상적으로 파싱되지 않은 경우
        if (os.equals(browser)) {
            return userAgent;
        }
        return String.format("%s %s", os, browser);
    }

    public static String parseLocale(HttpServletRequest request) {
        Locale locale = request.getLocale();
        if (Objects.isNull(locale)) {
            return "unknown";
        }
        return locale.getCountry().isEmpty() ? "unknown" : locale.getCountry();
    }

    public static String parseReferer(HttpServletRequest request) {
        String referer = request.getHeader("Referer");
        if (referer == null || referer.isEmpty()) {
            return "";
        }
        try {
           new URL(referer);
        } catch (MalformedURLException e) {
            log.error("Failed to parse referer by malformed, referer = {}, error message = {}", referer, e.getMessage());
        }
        return referer;
    }

    public static TokenResponseDto parseAuthToken(HttpServletRequest request) {
        String accessToken = parseAccessToken(request);
        if (accessToken== null) {
            log.warn("access token is null");
        }
        String refreshToken = request.getParameter("refresh_token");
        if (refreshToken == null) {
            log.warn("refresh token is null");
        }
        return new TokenResponseDto(accessToken, refreshToken);
    }

    private static String parseBrowser(String userAgent) {
        if (userAgent.contains("Chrome") || userAgent.contains("CriOS")) {
            return "chrome";
        } else if (userAgent.contains("Edge")) {
            return "edge";
        } else if (userAgent.contains("Whale")) {
            return  "whale";
        } else if (userAgent.contains("Firefox")) {
            return  "firefox";
        } else if (userAgent.contains("AppleWebKit") || userAgent.contains("Safari")) {
            return  "safari";
        } else if (userAgent.contains("Opera")) {
            return  "opera";
        } else if (userAgent.contains("Postman")) {
            return  "postman";
        }
        return userAgent;
    }

    private static String parseOS(String userAgent) {
        if (userAgent.contains("Windows")) {
            return "windows";
        } else if (userAgent.contains("Android")) {
            return  "android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "ios";
        } else if (userAgent.contains("Macintosh")) {
            return "mac";
        } else if (userAgent.contains("Ubuntu") || userAgent.contains("Linux")) {
            return  "linux";
        }
        return userAgent;
    }

    private static String parseAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (authHeader == null || authHeader.isEmpty() || !authHeader.startsWith("Bearer")) {
            log.warn("access token is null");
            return null;
        }
        return authHeader.substring(7);
    }
}
