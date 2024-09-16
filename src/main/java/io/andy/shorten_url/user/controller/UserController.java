package io.andy.shorten_url.user.controller;

import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.session.SessionService;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.service.UserService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@Slf4j
@RequestMapping("/user")
@RestController
public class UserController {
    private final UserService userService;
    private final SessionService sessionService;

    @Autowired
    UserController(UserService userService, SessionService sessionService) {
        this.userService = userService;
        this.sessionService = sessionService;
    }

    @PostMapping("/create")
    public UserResponseDto SignUp(HttpServletRequest request, @RequestBody Map<String, String> signupRequest) {
        String username = signupRequest.get("username");
        String password = signupRequest.get("password");
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        return userService.createUserByUsername(new UserSignUpDto(username, ipAddress, userAgent), password);
    }

    @PostMapping("/login")
    public UserResponseDto Login(HttpServletRequest request, @RequestBody Map<String, String> signupRequest) {
        String username = signupRequest.get("username");
        String password = signupRequest.get("password");
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        UserResponseDto user = userService.login(new UserLoginDto(username, ip, userAgent), password);
        sessionService.setAttribute(request, user.id());

        return user;
    }

    @DeleteMapping("/logout/{id}")
    public void logout(HttpServletRequest request, @PathVariable("id") Long id) {
        String clientIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        userService.logout(new UserLogOutDto(id, clientIp, userAgent));
        sessionService.invalidateSession(request, id);
    }

    @GetMapping("/all")
    public List<UserResponseDto> getAllUsers() {
        return userService.findAllUsers();
    }

    @GetMapping("/{id}")
    public UserResponseDto findUserById(@PathVariable("id") Long id) {
       return userService.findById(id);
    }

    @GetMapping("/find")
    public UserResponseDto findUserByUsername(@RequestParam String username) {
        return userService.findByUsername(username);
    }

    @PatchMapping("/{id}/username")
    public UserResponseDto UpdateUsername(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody String username) {
        validateSession(request);
        return userService.updateUsernameById(id, username);
    }

    @PatchMapping("/{id}/password")
    public UserResponseDto UpdatePassword(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody String password) {
        validateSession(request);
        return userService.updatePasswordById(id, password);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(HttpServletRequest request, @PathVariable("id") Long id) {
        validateSession(request);

        String clientIp = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");
        log.info("user try to delete id={}, clientIp={}, user-agent={}", id, clientIp,userAgent);

        userService.deleteById(new UserDeleteDto(id, clientIp, userAgent));
    }

    private void validateSession(HttpServletRequest request) {
        if (Objects.isNull(sessionService.getAttribute(request))) {
            String clientIp = request.getRemoteAddr();
            String userAgent = request.getHeader("User-Agent");

            log.debug("invalidate session, clientIp={}, user-agent={}", clientIp, userAgent);
            throw new UnauthorizedException("INVALIDATE SESSION");
        }
    }
}
