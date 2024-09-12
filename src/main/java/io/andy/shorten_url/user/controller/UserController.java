package io.andy.shorten_url.user.controller;

import io.andy.shorten_url.session.SessionService;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Objects;

@RequestMapping("/user")
@RestController
public class UserController {
    @Autowired private final UserService userService;
    @Autowired private final SessionService sessionService;
    private final Logger logger = LoggerFactory.getLogger(UserController.class);

    public UserController(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @ResponseBody
    @PostMapping("/create")
    public UserResponseDto SignUp(@RequestParam String username, @RequestParam String password) {
        return userService.createUserByUsername(new UserSignUpDto(username, password));
    }

    @PostMapping("/login")
    public UserResponseDto Login(HttpServletRequest request, @RequestParam String username, @RequestParam String password) {
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        UserResponseDto user = userService.login(new UserLoginDto(username, password, ip, userAgent));
        sessionService.setSessionById(request, user.getId());

        return user;
    }

    @DeleteMapping("/logout/{id}")
    public void logout(HttpServletRequest request, @PathVariable("id") Long id) {
        userService.logout(new UserLogOutDto(id));
        sessionService.removeSessionById(request, id);
    }

    @GetMapping("/all")
    public List<UserResponseDto> getAllUsers() {
        return userService.findAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponseDto findUserById(@PathVariable("id") Long id) {
       return userService.findById(id);
    }

    @GetMapping("/")
    public UserResponseDto findUserByUsername(@RequestParam String username) {
        return userService.findByUsername(username);
    }

    @PatchMapping("/{id}")
    public UserResponseDto UpdateUsername(HttpServletRequest request, @PathVariable("id") Long id, @RequestParam String username) {
        validateSession(request, String.format("id=%s failed to update username by invalid session", id));
        return userService.updateUsernameById(id, username);
    }

    @PatchMapping("/password/{id}")
    public UserResponseDto UpdatePassword(HttpServletRequest request, @PathVariable("id") Long id, @RequestParam String password) {
        validateSession(request, String.format("id=%s failed to update password by invalid session", id));
        return userService.updatePasswordById(id, password);
    }

    @DeleteMapping("/{id}")
    public void delete(HttpServletRequest request, @PathVariable("id") Long id) {
        validateSession(request, String.format("id=%s failed to delete by invalid session", id));

        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        logger.info("user try to delete id={}, ip={}, user-agent={}", id,ip,userAgent);

        userService.deleteById(new UserDeleteDto(id, ip, userAgent));
    }

    private void validateSession(HttpServletRequest request, String message) {
        if (Objects.isNull(sessionService.getSession(request))) {
            String ip = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");
            logger.debug("invalidate session, message={}, ip={}, user-agent={}", message, ip,userAgent);
            throw new IllegalStateException("INVALIDATE SESSION");
        }
    }
}
