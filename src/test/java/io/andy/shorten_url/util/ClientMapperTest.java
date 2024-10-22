package io.andy.shorten_url.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;

import static org.junit.jupiter.api.Assertions.*;

class ClientMapperTest {
    MockHttpServletRequest request;

    @BeforeEach
    public void init() {
        request = new MockHttpServletRequest();
    }

    @ParameterizedTest
    @DisplayName("ip주소 파싱")
    @ValueSource(strings = {"0:0:0:0:0:0:0:1", "[\"127.0.0.1\"]"})
    void parseClientIp(String ip) {
        request.setAttribute("X-Forwarded-For", ip);

        String parsedClientIp = ClientMapper.parseClientIp(request);

        assertNotNull(parsedClientIp);
        assertEquals("127.0.0.1", parsedClientIp);
    }

    @ParameterizedTest
    @DisplayName("user-agent 파싱")
    @CsvSource(value = {
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15, mac safari",
            "Mozilla/5.0 (Android 13; SM-F711N; Mobile; rv:117.0) Gecko/117.0 Firefox/117.0, android firefox"
    })
    void parseUserAgent(String userAgent, String expected) {
        request.addHeader("User-Agent", userAgent);

        String parsedUserAgent = ClientMapper.parseUserAgent(request);

        assertNotNull(parsedUserAgent);
        assertEquals(expected, parsedUserAgent);
    }

    @ParameterizedTest
    @DisplayName("locale 파싱")
    @CsvSource(value = {"KR, KO", "EN, US"})
    public void parseLocale(String language, String country) {
        request.addPreferredLocale(new Locale(language, country));

        String locale = ClientMapper.parseLocale(request);

        assertNotNull(locale);
        assertEquals(country, locale);
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