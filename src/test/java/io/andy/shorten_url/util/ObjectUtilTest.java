package io.andy.shorten_url.util;

import io.andy.shorten_url.exception.server.ObjectUtilException;
import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.entity.User;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

class ObjectUtilTest {

    @Test
    @DisplayName("필드 하나 제외")
    public void fieldFilter() throws ObjectUtilException {
        User user = createUserDummy();

        User censored = ObjectUtil.fieldFilter(user, "password");

        assertEquals(User.class, censored.getClass());
        assertEquals(user.getUsername(), censored.getUsername());
        assertNull(censored.getPassword());
    }

    @Test
    @DisplayName("배열로 제외할 필드 필터")
    public void fieldFilters() throws ObjectUtilException {
        User user = createUserDummy();

        String[] excludes = {"username", "password"};
        User censored = ObjectUtil.fieldFilter(user, excludes);

        assertEquals(User.class, censored.getClass());
        assertNull(censored.getUsername());
        assertNull(censored.getPassword());
    }

    @Test
    @DisplayName("Optional 객체 필터 처리")
    public void optionalFilter() throws ObjectUtilException {
        Optional<User> user = Optional.of(createUserDummy());
        String[] excludes = {"username", "password"};
        User censored = ObjectUtil.fieldFilter(user, excludes);

        assertEquals(User.class, censored.getClass());
        assertNull(censored.getUsername());
        assertNull(censored.getPassword());
    }

    private User createUserDummy() {
        String username = "hello@gmail.com";
        String password = "12345678";
        return new User(username, password, UserState.NORMAL, UserRole.USER);
    }
}