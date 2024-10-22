package io.andy.shorten_url.util.ip;

import io.andy.shorten_url.exception.server.InternalServerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class IpLocationUtilsTest {

    @Test
    @DisplayName("ip-api get 테스트")
    public void ipApiTest() throws InternalServerException {
        String ip = "1.1.1.1";
        IpLocationUtils ipLocationUtils = new IpLocationUtils();
        IpApiResponse response = ipLocationUtils.getLocationByIp(ip);

        assertNotNull(response);
        assertEquals(response.getClass(), IpApiResponse.class);
        assertEquals("success", response.getStatus());
        assertEquals(ip, response.getQuery());
    }
}