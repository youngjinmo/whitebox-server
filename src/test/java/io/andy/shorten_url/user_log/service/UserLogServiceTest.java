package io.andy.shorten_url.user_log.service;

import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.UserResponseDto;
import io.andy.shorten_url.user.entity.User;
import io.andy.shorten_url.user.repository.UserRepository;
import io.andy.shorten_url.user_log.constant.UserLogMessage;
import io.andy.shorten_url.user_log.dto.AccessInfoDto;
import io.andy.shorten_url.user_log.dto.UpdateInfoDto;
import io.andy.shorten_url.user_log.entity.UserLog;
import io.andy.shorten_url.user_log.repository.UserLogRepository;

import org.junit.jupiter.api.*;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@Transactional
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.NONE)
@ActiveProfiles("test")
class UserLogServiceTest {
    @Autowired private UserLogRepository userLogRepository;
    @Autowired private UserRepository userRepository;
    @Autowired private UserLogService userLogService;

    private User user;

    @BeforeEach
    public void flushBefore() {
        userLogRepository.deleteAll();
        user = setInitiateUser();
    }

    @AfterEach
    public void flushAfter() {
        userLogRepository.deleteAll();
        userRepository.deleteById(user.getId());
    }

    @Test
    void putUpdateInfoLog() {
        String newUsername = "new@gmail.com";
        userLogService.putUpdateInfoLog(new UpdateInfoDto(
                new UserResponseDto(user),
                UserLogMessage.UPDATE_USERNAME,
                user.getUsername(),
                newUsername
        ));

        List<UserLog> result = userLogRepository.findByUserId(user.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(newUsername, result.get(0).getPostValue());
    }

    @Test
    void putUserAccessLog() {
        String localhost = "127.0.0.1";
        userLogService.putUserAccessLog(new AccessInfoDto(
                new UserResponseDto(user),
                UserLogMessage.SIGNUP,
                localhost,
                "firfox"
        ));

        List<UserLog> result = userLogRepository.findByUserId(user.getId()).orElse(null);
        assertNotNull(result);
        assertEquals(localhost, result.get(0).getIpAddress());
    }

    @Test
    void getUserLogsByUserId() {
        userLogService.putUserAccessLog(new AccessInfoDto(
                new UserResponseDto(user),
                UserLogMessage.LOGIN,
                "127.0.0.1",
                "firefox"
        ));
        userLogService.putUserAccessLog(new AccessInfoDto(
                new UserResponseDto(user),
                UserLogMessage.LOGOUT,
                "127.0.0.1",
                "firefox"
        ));

        List<UserLog> result = userLogService.getUserLogsByUserId(user.getId());
        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void getUserLogsByLogMessage() {
        UserLogMessage message = UserLogMessage.LOGIN;
        userLogService.putUserAccessLog(new AccessInfoDto(
                new UserResponseDto(user),
                message,
                "127.0.0.1",
                "firefox"
        ));
        userLogService.putUserAccessLog(new AccessInfoDto(
                new UserResponseDto(user),
                message,
                "128.2.2.2",
                "chrome"
        ));

        List<UserLog> result = userLogService.getUserLogsByMessage(message);

        assertNotNull(result);
        assertEquals(2, result.size());
        assertEquals(message, result.get(0).getMessage());
    }

    @Test
    public void findAllUserLogs() {
        userLogService.putUserAccessLog(new AccessInfoDto(
                new UserResponseDto(user),
                UserLogMessage.LOGIN,
                "128.2.2.2",
                "chrome"
        ));
        userLogService.putUpdateInfoLog(new UpdateInfoDto(
                new UserResponseDto(user),
                UserLogMessage.UPDATE_USERNAME,
                user.getUsername(),
               "new@gmail.com"
        ));

        List<UserLog> result = userLogService.findAllUserLogs();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    public void findLatestUserLogs() {
        for (int i = 0; i < 10; i++) {
            userLogService.putUserAccessLog(new AccessInfoDto(
                    new UserResponseDto(user),
                    UserLogMessage.LOGIN,
                    "127.0.0."+i,
                    "chrome+"+i
            ));
        }

        List<UserLog> result = userLogService.findLatestUserLogs(3);
        assertNotNull(result);
        assertEquals(3, result.size());
    }

    public User setInitiateUser() {
        String username = "hello@gmail.com";
        String password = "1234@5678";
        UserState state = UserState.NORMAL;
        UserRole role = UserRole.USER;
        return userRepository.save(new User(username, password, state, role));
    }
}