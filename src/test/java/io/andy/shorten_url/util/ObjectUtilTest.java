package io.andy.shorten_url.util;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilTest {
    private final String username = "hello@gmail.com";
    private final String password = "secure-password";
    private final String address = "South Korea, Seoul";

    @Test
    @DisplayName("필드 하나 제외")
    public void fieldFilter() {
        // given
        Temp targetClass = new Temp(username, password, address);
        String[] excludeFields = {"password"};

        // when
        Temp censored = ObjectUtil.fieldFilter(targetClass, excludeFields);

        // then
        assertEquals(Temp.class, censored.getClass());
        assertEquals(targetClass.getUsername(), censored.getUsername());
        assertNull(censored.getPassword());
    }

    @Test
    @DisplayName("배열로 제외할 필드 필터")
    public void fieldFilters() {
        // given
        Temp targetClass = new Temp(username, password, address);
        String[] excludes = {"username", "password"};

        // when
        Temp censored = ObjectUtil.fieldFilter(targetClass, excludes);

        // then
        assertEquals(Temp.class, censored.getClass());
        assertNull(censored.getUsername());
        assertNull(censored.getPassword());
        assertNotNull(censored.getAddress());
    }

    @Getter
    @AllArgsConstructor
    @NoArgsConstructor
    private class Temp {
        private String username;
        private String password;
        private String address;
    }
}