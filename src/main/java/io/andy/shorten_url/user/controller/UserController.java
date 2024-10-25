package io.andy.shorten_url.user.controller;

import io.andy.shorten_url.auth.AuthService;
import io.andy.shorten_url.exception.client.ForbiddenException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.session.InMemorySessionService;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.service.UserService;
import io.andy.shorten_url.util.ClientMapper;

import jakarta.mail.MessagingException;
import jakarta.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static io.andy.shorten_url.auth.AuthPolicy.*;

@Slf4j
@RequestMapping("/user")
@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final InMemorySessionService sessionService;
    private final AuthService authService;

    @PostMapping("/create")
    public ResponseEntity<UserResponseDto> SignUp(
            HttpServletRequest request,
            @RequestBody Map<String, String> signupRequest
    ) {
        String username = signupRequest.get("username");
        String password = signupRequest.get("password");
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        UserResponseDto user = userService.createUserByUsername(new UserSignUpDto(username, ipAddress, userAgent), password);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    public ResponseEntity<UserResponseDto> Login(
            HttpServletRequest request,
            @RequestBody Map<String, String> signupRequest
    ) {
        String username = signupRequest.get("username");
        String password = signupRequest.get("password");
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        try {
            UserResponseDto user = userService.login(new UserLoginDto(username, ip, userAgent), password);
            String sessionKey = LOGIN_SESSION_KEY.concat(":").concat(String.valueOf(user.id()));
            sessionService.setSession(request, sessionKey, "login", LOGIN_SESSION_ACTIVE_TIME);

            return new ResponseEntity<>(user, HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/send-email-auth")
    public ResponseEntity<Void> sendEmailAuth(HttpServletRequest request, @RequestParam String recipient) {
        try {
            if (userService.isDuplicateUsername(recipient)) {
                throw new ForbiddenException("EMAIL DUPLICATED");
            }

            String secretCode = this.authService.sendEmailAuthCode(recipient);
            String sessionKey = EMAIL_AUTH_SESSION_KEY.concat(":").concat(secretCode);
            sessionService.setSession(request, sessionKey, recipient, EMAIL_AUTH_SESSION_ACTIVE_TIME);

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (MessagingException e) {
            log.error("failed to sent mail = {}, error message={}", recipient, e.getMessage());
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/verify-email-auth")
    public ResponseEntity<Boolean> verifyEmailAuth(
            HttpServletRequest request,
            @RequestParam String secretCode,
            @RequestParam String recipient
    ) {
        String sessionKey = EMAIL_AUTH_SESSION_KEY.concat(":").concat(secretCode);
        if (sessionService.verifySession(request, sessionKey, recipient)) {
            return new ResponseEntity<>(HttpStatus.OK);
        }
        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED SESSION");
    }

    @DeleteMapping("/logout/{id}")
    public ResponseEntity<Void> logout(HttpServletRequest request, @PathVariable("id") Long id) {
        String clientIp = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);

        try {
            userService.logout(new UserLogOutDto(id, clientIp, userAgent));
            sessionService.invalidateSession(request, LOGIN_SESSION_KEY, id);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDto>> getAllUsers() {
        List<UserResponseDto> users = userService.findAllUsers();
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> findUserById(@PathVariable("id") Long id) {
        UserResponseDto user = userService.findById(id);
       return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @GetMapping("/find")
    public ResponseEntity<UserResponseDto> findUserByUsername(@RequestParam String username) {
        return new ResponseEntity<>(userService.findByUsername(username), HttpStatus.OK);
    }

    @PatchMapping("/{id}/username")
    public ResponseEntity<UserResponseDto> UpdateUsername(@PathVariable("id") Long id, @RequestBody String username) {
        UserResponseDto user = userService.updateUsernameById(id, username);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<UserResponseDto> UpdatePassword(@PathVariable("id") Long id, @RequestBody String password) {
        UserResponseDto user = userService.updatePasswordById(id, password);
        return new ResponseEntity<>(user, HttpStatus.OK);
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(HttpServletRequest request, @PathVariable("id") Long id) {
        String clientIp = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);
        log.info("user try to delete id={}, clientIp={}, user-agent={}", id, clientIp,userAgent);

        userService.deleteById(new UserDeleteDto(id, clientIp, userAgent));
        sessionService.invalidateSession(request, LOGIN_SESSION_KEY, id);

        return new ResponseEntity<>(HttpStatus.OK);
    }
}
