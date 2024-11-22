package io.andy.shorten_url.util;

import io.andy.shorten_url.exception.client.UnauthorizedException;

import io.andy.shorten_url.util.mapper.ClientInfo;
import io.andy.shorten_url.util.mapper.ClientMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import org.springframework.mock.web.MockHttpServletRequest;

import java.util.Locale;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

class ClientMapperTest {
    MockHttpServletRequest request;

    @BeforeEach
    void init() {
        request = new MockHttpServletRequest();
    }

    @Test
    @DisplayName("접속 정보 파싱해서 map으로 반환")
    void parseAccessInfo() {
        // given
        String clientIp = "127.1.1.1";
        String userAgent = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/110.0.5481.100 Safari/537.36 Edg/110.0.1587.57";
        String token = "mock-access-token";

        request.addHeader("X-Forwarded-For", clientIp);
        request.addHeader("User-Agent", userAgent);
        request.addHeader("Authorization", "Bearer " + token);

        // when
        Map<ClientInfo, String> result = ClientMapper.parseAccessInfo(request);

        // then
        assertEquals(clientIp, result.get(ClientInfo.IP_ADDRESS));
        assertEquals("Windows Edge", result.get(ClientInfo.USER_AGENT));
        assertEquals(token, result.get(ClientInfo.TOKEN));
    }

    @ParameterizedTest
    @DisplayName("ip주소 파싱")
    @ValueSource(strings = {"0:0:0:0:0:0:0:1", "127.0.0.1,128.0.0.1,129.0.0.1"})
    void parseClientIp(String ip) {
        request.addHeader("X-Forwarded-For", ip);

        String parsedClientIp = ClientMapper.parseClientIp(request);

        assertNotNull(parsedClientIp);
        assertEquals("127.0.0.1", parsedClientIp);
    }

    @ParameterizedTest
    @DisplayName("user-agent 파싱")
    @CsvSource(value = {
            "Mozilla/5.0 (Macintosh; Intel Mac OS X 14_0) AppleWebKit/605.1.15, Mac undefined",
            "Mozilla/5.0 (Android 13; SM-F711N; Mobile; rv:117.0) Gecko/117.0 Firefox/117.0, Android Firefox"
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
    void parseLocale(String language, String country) {
        request.addPreferredLocale(new Locale(language, country));

        String locale = ClientMapper.parseLocale(request);

        assertNotNull(locale);
        assertEquals(country, locale);
    }

    @Test
    @DisplayName("referer 파싱")
    void parseReferer() {
        request.addHeader("Referer", "https://www.google.com");

        String referer = ClientMapper.parseReferer(request);

        assertNotNull(referer);
        assertEquals("https://www.google.com", referer);
    }

    @Test
    @DisplayName("인증 헤더 파싱")
    void parseAuthorization() {
        String mockAccessToken = "mockAccessToken";
        request.addHeader("Authorization", "Bearer "+mockAccessToken);

        String authToken = ClientMapper.parseAuthToken(request);

        assertEquals(mockAccessToken, authToken);
    }

    @Test
    @DisplayName("인증 헤더 파싱 예외")
    void throwExceptionWithWrongToken() {
        String mockAccessToken = "";
        request.addHeader("Authorization", mockAccessToken);

        assertThrows(UnauthorizedException.class, () -> ClientMapper.parseAuthToken(request));
    }
}