package io.andy.shorten_url.user.controller;

import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.exception.client.ForbiddenException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.session.InMemorySessionService;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.service.UserService;
import io.andy.shorten_url.util.ClientMapper;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@Slf4j
@RequestMapping("/${apiPrefix}/user")
@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final InMemorySessionService sessionService;

    @PostMapping("/create")
    public ResponseEntity<UserResponseDto> SignUp(
            HttpServletRequest request,
            @RequestBody Map<String, String> signupRequest
    ) {
        String username = signupRequest.get("username");
        String password = signupRequest.get("password");
        String ipAddress = request.getRemoteAddr();
        String userAgent = request.getHeader("User-Agent");

        UserResponseDto user = userService.signUpByUsername(new UserSignUpDto(username, ipAddress, userAgent), password);
        return new ResponseEntity<>(user, HttpStatus.CREATED);
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> Login(
            HttpServletRequest request,
            @RequestBody Map<String, String> signupRequest
    ) {
        String username = signupRequest.get("username");
        String password = signupRequest.get("password");
        String ip = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);

        try {
            UserLoginResponseDto result = userService.login(new UserLoginRequestDto(username, ip, userAgent), password);
            return new ResponseEntity<>(result, HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @PostMapping("/email-verification-code")
    public ResponseEntity<Void> sendEmailAuth(@RequestParam String recipient) {
        try {
            if (userService.isDuplicateUsername(recipient)) {
                throw new ForbiddenException("EMAIL DUPLICATED");
            }
            this.userService.sendEmailAuthCode(recipient);
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (ForbiddenException e) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/verify-email")
    public ResponseEntity<Boolean> verifyEmailAuth(@RequestParam String verificationCode, @RequestParam String recipient) {
        try {
            userService.verifyEmail(recipient, verificationCode);
            return new ResponseEntity<>(true, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "UNAUTHORIZED SESSION");
        }
    }

    @DeleteMapping("/logout/{id}")
    public ResponseEntity<Void> logout(HttpServletRequest request, @PathVariable("id") Long id) {
        String clientIp = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);
        TokenResponseDto tokenDto = ClientMapper.parseAuthToken(request);

        try {
            userService.logout(new UserLogOutDto(id, clientIp, userAgent, tokenDto));
            return new ResponseEntity<>(HttpStatus.OK);
        } catch (UnauthorizedException e) {
            log.error("error happened by logout, userId={}, message={}", id, e.getMessage());
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDto>> getAllUsers(@RequestParam(required = false) UserState[] states) {
        List<UserResponseDto> users = userService.findAllUsers(states == null ? new UserState[]{} : states);
        return new ResponseEntity<>(users, HttpStatus.OK);
    }

    @PreAuthorize("hasRole('USER')")
    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> findUserById(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails
    ) {
        UserResponseDto user = userService.findById(id);
        if (!userDetails.getUsername().equals(user.username())) {
            throw new UnauthorizedException();
        }
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

        userService.deleteById(new UserDeleteDto(id, clientIp, userAgent));

        return new ResponseEntity<>(HttpStatus.OK);
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<String> resetPassword(@PathVariable Long id) {
        String newPassword = userService.resetPassword(id);
        return new ResponseEntity<>(newPassword, HttpStatus.OK);
    }
}
