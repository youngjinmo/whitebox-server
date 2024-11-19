package io.andy.shorten_url.user.controller;

import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.ForbiddenException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.service.UserService;
import io.andy.shorten_url.user_log.constant.UserLogMessage;
import io.andy.shorten_url.user_log.dto.AccessUserInfoDto;
import io.andy.shorten_url.user_log.dto.UpdatePrivacyInfoDto;
import io.andy.shorten_url.user_log.dto.UpdateUserInfoDto;
import io.andy.shorten_url.user_log.service.UserLogService;
import io.andy.shorten_url.util.ClientMapper;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@RequestMapping("/api/user")
@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserLogService userLogService;

    @PostMapping("/create")
    public ResponseEntity<UserResponseDto> SignUp(HttpServletRequest request, @RequestBody CreateUserRequestDto signUpDto) {
        String ipAddress = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);

        try {
            UserResponseDto userDto = userService.createByEmail(CreateUserServiceDto.from(signUpDto));
            userLogService.putUserAccessLog(AccessUserInfoDto.build(userDto, UserLogMessage.SIGNUP, ipAddress, userAgent));

            return new ResponseEntity<>(userDto, HttpStatus.CREATED);
        } catch (BadRequestException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> Login(HttpServletRequest request, @RequestBody UserLoginRequestDto loginDto) {
        String ipAddress = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);

        try {
            UserLoginResponseDto userDto = userService.login(UserLoginServiceDto.build(loginDto, ipAddress, userAgent));
            userLogService.putUserAccessLog(AccessUserInfoDto.build(userDto, UserLogMessage.LOGIN, ipAddress, userAgent));

            return new ResponseEntity<>(userDto, HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/email-verification-code")
    public ResponseEntity<Void> sendEmailAuth(@RequestBody String recipient) {
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
    public ResponseEntity<String> verifyEmailAuth(@RequestParam String verificationCode, @RequestParam String recipient) {
        try {
            userService.verifyEmail(recipient, verificationCode);

            return new ResponseEntity<>("verified", HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @DeleteMapping("/{id}/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request, @PathVariable("id") Long id) {
        String ipAddress = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);
        TokenResponseDto tokenDto = ClientMapper.parseAuthToken(request);

        try {
            UserResponseDto userDto = userService.findById(id);
            userService.logout(UserLogoutServiceDto.build(id, tokenDto));
            userLogService.putUserAccessLog(AccessUserInfoDto.build(userDto, UserLogMessage.LOGOUT, ipAddress, userAgent));

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDto>> findAllUsers(@RequestParam(required = false) UserState[] states) {
        try {
            List<UserResponseDto> users = userService.findAllUsers(states == null ? new ArrayList<>() : List.of(states));

            return new ResponseEntity<>(users, HttpStatus.OK);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> findUserById(@PathVariable("id") Long id) {
        try {
            UserResponseDto userDto = userService.findById(id);

            return new ResponseEntity<>(userDto, HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/find")
    public ResponseEntity<UserResponseDto> findUserByUsername(@RequestParam String username) {
        try{
            UserResponseDto userDto = userService.findByUsername(username);

            return new ResponseEntity<>(userDto, HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/{id}/username")
    public ResponseEntity<UserResponseDto> updateUsername(@PathVariable("id") Long id, @RequestBody String username) {
        try {
            UserResponseDto previousUser = userService.findById(id);

            UserResponseDto userDto = userService.updateUsernameById(id, username);
            userLogService.putUpdateInfoLog(UpdateUserInfoDto.build(userDto, UserLogMessage.UPDATE_USERNAME, previousUser.username(), username));

            return new ResponseEntity<>(userDto, HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/{id}/password")
    public ResponseEntity<UserResponseDto> updatePassword(HttpServletRequest request, @PathVariable("id") Long id, @RequestBody String password) {
        String ipAddress = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);

        try {
            UserResponseDto userDto = userService.updatePasswordById(id, password);

            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.build(userDto, UserLogMessage.DELETE_USER, ipAddress, userAgent));

            return new ResponseEntity<>(userDto, HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/{id}/state")
    public ResponseEntity<UserResponseDto> updateState(@PathVariable("id") Long id, @RequestBody UserState state) {
        try {
            UserResponseDto previousUserDto = userService.findById(id);
            UserResponseDto userDto = userService.updateStateById(id, state);
            userLogService.putUpdateInfoLog(UpdateUserInfoDto.build(previousUserDto, UserLogMessage.UPDATE_STATE, previousUserDto.state().name(), state.name()));

            return new ResponseEntity<>(userDto, HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteUser(HttpServletRequest request, @PathVariable("id") Long id) {
        String ipAddress = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);
        try {
            UserResponseDto previousUserDto = userService.findById(id);
            userService.deleteById(DeleteUserServiceDto.of(id, ipAddress, userAgent));
            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.build(previousUserDto, UserLogMessage.DELETE_USER, ipAddress, userAgent));

            return new ResponseEntity<>(HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/{id}/reset-password")
    public ResponseEntity<String> resetPassword(HttpServletRequest request, @PathVariable Long id) {
        String ipAddress = ClientMapper.parseClientIp(request);
        String userAgent = ClientMapper.parseUserAgent(request);
        try {
            UserResponseDto userDto = userService.findById(id);
            String newPassword = userService.resetPassword(id);
            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.build(userDto, UserLogMessage.UPDATE_PASSWORD, ipAddress, userAgent));

            return new ResponseEntity<>(newPassword, HttpStatus.OK);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
