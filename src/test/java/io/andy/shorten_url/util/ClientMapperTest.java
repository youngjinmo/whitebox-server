package io.andy.shorten_url.util;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;

import static org.junit.jupiter.api.Assertions.*;

class ClientMapperTest {
    MockHttpServletRequest request;

    @BeforeEach
    public void init() {
        request = new MockHttpServletRequest();
    }

    @Test
    void parseClientIp() {
        request.setRemoteAddr("0:0:0:0:0:0:0:1");

        String ip = ClientMapper.parseClientIp(request);

        assertNotNull(ip);
        assertEquals("127.0.0.1", ip);
    }

    @Test
    void parseUserAgent() {
        request.addHeader("User-Agent", "Mozilla/5.0");

        String userAgent = ClientMapper.parseUserAgent(request);

        assertNotNull(userAgent);
        assertEquals("firefox", userAgent);
    }
}