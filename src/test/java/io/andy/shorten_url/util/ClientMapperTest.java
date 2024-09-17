package io.andy.shorten_url.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ClientMapperTest {
    MockHttpServletRequest request;

    @BeforeEach
    public void init() {
        request = new MockHttpServletRequest();
    }

    @Test
    @DisplayName("ip주소 파싱")
    void parseClientIp() {
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        String ip = ClientMapper.parseClientIp(request);

        assertNotNull(ip);
        assertEquals("127.0.0.1", ip);
    }

    @Test
    @DisplayName("user-agent 파싱")
    void parseUserAgent() {
        request.addHeader("User-Agent", "Mozilla/5.0");

        String userAgent = ClientMapper.parseUserAgent(request);

        assertNotNull(userAgent);
        assertEquals("firefox", userAgent);
    }

    @Test
    @DisplayName("locale 파싱")
    public void parseLocale() {
        request.addPreferredLocale(new Locale("kr", "KO"));

        String locale = ClientMapper.parseLocale(request);

        assertNotNull(locale);
        assertEquals("KO", locale);
    }

    @Test
    @DisplayName("referer 파싱")
    public void parseReferer() {
        request.addHeader("Referer", "https://www.google.com");

        String referer = ClientMapper.parseReferer(request);

        assertNotNull(referer);
        assertEquals("https://www.google.com", referer);
    }
}