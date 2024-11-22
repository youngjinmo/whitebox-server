package io.andy.shorten_url.util.mapper;

import io.andy.shorten_url.exception.client.UnauthorizedException;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import java.net.MalformedURLException;
import java.net.URL;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

@Slf4j
public class ClientMapper {
    public static Map<ClientInfo, String> parseAccessInfo(HttpServletRequest request) {
        Map<ClientInfo, String> accessInfo = new HashMap<>();
        accessInfo.put(ClientInfo.IP_ADDRESS, parseClientIp(request));
        accessInfo.put(ClientInfo.USER_AGENT, parseUserAgent(request));
        accessInfo.put(ClientInfo.LOCALE, parseLocale(request));
        accessInfo.put(ClientInfo.REFERER, parseReferer(request));
        accessInfo.put(ClientInfo.TOKEN, parseAccessToken(request));
        return accessInfo;
    }

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

    public static String parseAuthToken(HttpServletRequest request) {
        String accessToken = parseAccessToken(request);
        if (accessToken== null) {
            log.warn("access token is null");
            throw new UnauthorizedException();
        }
        return accessToken;
    }

    private static String parseBrowser(String userAgent) {
        if (userAgent.contains("Edg")) {
            return "Edge";
        } else if (userAgent.contains("Firefox")) {
            return  "Firefox";
        } else if (userAgent.contains("OPR")) {
            return  "Opera";
        } else if (userAgent.contains("Safari")) {
            return  "Safari";
        } else if (userAgent.contains("Chrome") || userAgent.contains("CriOS")) {
            return "Chrome";
        } else if (userAgent.toLowerCase().contains("postman")) {
            return  "";
        } else {
            log.debug("failed to parse browser, userAgent = {}", userAgent);
            return "undefined";
        }
    }

    private static String parseOS(String userAgent) {
        if (userAgent.contains("Windows")) {
            return "Windows";
        } else if (userAgent.contains("Android")) {
            return  "Android";
        } else if (userAgent.contains("iPhone") || userAgent.contains("iPad")) {
            return "ios";
        } else if (userAgent.contains("Macintosh") || userAgent.contains("Mac OS") || userAgent.contains("OS X")) {
            return "Mac";
        } else if (userAgent.contains("Ubuntu") || userAgent.contains("Linux")) {
            return  "Linux";
        } else if (userAgent.toLowerCase().contains("postman")) {
            return  "Postman";
        }
        return userAgent;
    }

    private static String parseAccessToken(HttpServletRequest request) {
        String authHeader = request.getHeader("Authorization");
        if (Objects.isNull(authHeader) || authHeader.isBlank() || !authHeader.startsWith("Bearer")) {
            log.warn("access token is null");
            return null;
        }
        return authHeader.substring(7);
    }
}
