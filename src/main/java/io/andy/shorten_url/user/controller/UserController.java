package io.andy.shorten_url.user.controller;

import io.andy.shorten_url.exception.client.ForbiddenException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.session.SessionService;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.service.UserService;
import io.andy.shorten_url.util.ClientMapper;
import io.andy.shorten_url.util.mail.MailService;

import jakarta.servlet.http.HttpServletRequest;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
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
    private final MailService mailService;

    @Autowired
    public UserController(
            UserService userService,
            SessionService sessionService,
            MailService mailService
    ) {
        this.userService = userService;
        this.sessionService = sessionService;
        this.mailService = mailService;
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
        sessionService.setAuthSession(request, user.id());

        return user;
    }

    @PostMapping("/send-email-auth")
    public ResponseEntity<Void> sendEmailAuth(HttpServletRequest request, @RequestParam String recipient) {
        if (userService.isDuplicateUsername(recipient)) {
            log.debug("[FORBIDDEN] Already existing user tried to send email authentication = {}", recipient);
            throw new ForbiddenException("EMAIL DUPLICATED");
        }
        try {
            String secretCode = this.mailService.sendMail(recipient);
            sessionService.setMailAuthSession(request, secretCode);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (Exception e) {
            e.printStackTrace();
            log.error("failed to sent mail = {}, error message={}", recipient, e.getMessage());
            throw new InternalServerException("FAILED TO SENT MAIL");
        }
    }

    @GetMapping("/verify-email-auth")
    public boolean verifyEmailAuth(HttpServletRequest request, @RequestParam String secretCode) {
        Object authSession = sessionService.getMailAuthSession(request);
        if (Objects.isNull(authSession)) return false;
        return authSession.equals(secretCode);
    }

    @DeleteMapping("/logout/{id}")
    public void logout(HttpServletRequest request, @PathVariable("id") Long id) {
        String clientIp = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);

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
        validateSession(request, id);
        return userService.updateUsernameById(id, username);
    }

    @PatchMapping("/{id}/password")
    public UserResponseDto UpdatePassword(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody String password) {
        validateSession(request, id);
        return userService.updatePasswordById(id, password);
    }

    @DeleteMapping("/{id}")
    public void deleteUser(HttpServletRequest request, @PathVariable("id") Long id) {
        validateSession(request, id);

        String clientIp = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);
        log.info("user try to delete id={}, clientIp={}, user-agent={}", id, clientIp,userAgent);

        userService.deleteById(new UserDeleteDto(id, clientIp, userAgent));
    }

    private void validateSession(HttpServletRequest request, Long id) {
        Object authSession = sessionService.getAuthSession(request);
        if (Objects.isNull(authSession)) throw new UnauthorizedException();
        if (authSession != id) throw new UnauthorizedException();
    }
}
