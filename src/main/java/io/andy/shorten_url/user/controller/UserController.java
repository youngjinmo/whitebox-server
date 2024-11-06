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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

import static io.andy.shorten_url.auth.AuthPolicy.*;

@Slf4j
@RequestMapping("/${apiPrefix}/user")
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

    @Transactional
    public ResponseEntity<UserLoginResponseDto> Login(
            HttpServletRequest request,
            @RequestBody Map<String, String> signupRequest
    ) {
        String username = signupRequest.get("username");
        String password = signupRequest.get("password");
        String ip = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        try {
            UserLoginResponseDto result = userService.login(new UserLoginRequestDto(username, ip, userAgent), password);
            return new ResponseEntity<>(result, HttpStatus.OK);
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

    @Transactional
    @DeleteMapping("/logout/{id}")
    public ResponseEntity<Void> logout(HttpServletRequest request, @PathVariable("id") Long id) {
        String clientIp = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);
        String accessToken = ClientMapper.parseAuthorization(request);

        try {
            userService.logout(new UserLogOutDto(id, clientIp, userAgent, accessToken));
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

    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(HttpServletRequest request, @PathVariable("id") Long id) {
        String clientIp = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);

        userService.deleteById(new UserDeleteDto(id, clientIp, userAgent));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PostMapping("/{id}/reset-password")
    public ResponseEntity<String> resetPassword(@PathVariable Long id) {
        String newPassword = userService.resetPassword(id);
        return new ResponseEntity<>(newPassword, HttpStatus.OK);
    }
}
