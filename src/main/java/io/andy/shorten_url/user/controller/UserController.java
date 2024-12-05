package io.andy.shorten_url.user.controller;

import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.ForbiddenException;
import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.service.UserService;
import io.andy.shorten_url.user_log.constant.UserLogMessage;
import io.andy.shorten_url.user_log.dto.AccessUserInfoDto;
import io.andy.shorten_url.user_log.dto.UpdatePrivacyInfoDto;
import io.andy.shorten_url.user_log.dto.UpdateUserInfoDto;
import io.andy.shorten_url.user_log.service.UserLogService;
import io.andy.shorten_url.util.mapper.ClientInfo;
import io.andy.shorten_url.util.mapper.ClientMapper;

import jakarta.servlet.http.HttpServletRequest;

import lombok.AllArgsConstructor;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@RequestMapping("/api/user")
@RestController
@AllArgsConstructor
public class UserController {
    private final UserService userService;
    private final UserLogService userLogService;

    @PostMapping("/create")
    public ResponseEntity<UserResponseDto> SignUp(HttpServletRequest request, @RequestBody CreateUserRequestDto signUpDto) {
        try {
            // parse client access into
            Map<ClientInfo, String> accessInfo = ClientMapper.parseAccessInfo(request);

            // create user
            UserResponseDto userDto = userService.createByEmail(CreateUserServiceDto.from(signUpDto));

            // put log
            userLogService.putUserAccessLog(AccessUserInfoDto.build(
                    userDto,
                    UserLogMessage.SIGNUP,
                    accessInfo.get(ClientInfo.IP_ADDRESS),
                    accessInfo.get(ClientInfo.USER_AGENT))
            );

            return new ResponseEntity<>(userDto, HttpStatus.CREATED);
        } catch (BadRequestException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/login")
    public ResponseEntity<UserLoginResponseDto> Login(HttpServletRequest request, @RequestBody UserLoginRequestDto loginDto) {
        try {
            // parse client access into
            Map<ClientInfo, String> accessInfo = ClientMapper.parseAccessInfo(request);

            // grant token
            UserLoginResponseDto userDto = userService.login(UserLoginServiceDto.build(
                    loginDto,
                    accessInfo.get(ClientInfo.IP_ADDRESS),
                    accessInfo.get(ClientInfo.USER_AGENT))
            );

            // put log
            userLogService.putUserAccessLog(AccessUserInfoDto.build(
                    userDto,
                    UserLogMessage.LOGIN,
                    accessInfo.get(ClientInfo.IP_ADDRESS),
                    accessInfo.get(ClientInfo.USER_AGENT))
            );

            return ResponseEntity.ok(userDto);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/email-verification-code")
    public ResponseEntity<String> sendEmailAuth(@RequestBody String recipient) {
        try {
            this.userService.sendEmailAuthCode(recipient);

            return ResponseEntity.ok("success");
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PostMapping("/verify/email")
    public ResponseEntity<String> verifyEmailAuth(@RequestParam String verificationCode, @RequestParam String recipient) {
        try {
            userService.verifyEmail(recipient, verificationCode);

            return ResponseEntity.ok("verified");
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    @DeleteMapping("/logout")
    public ResponseEntity<String> logout(HttpServletRequest request) {
        try {
            // parse client access into
            Map<ClientInfo, String> accessInfo = ClientMapper.parseAccessInfo(request);

            // verify token
            UserLogoutResponseDto logoutResponseDto = userService.logout(UserLogoutRequestDto.build(accessInfo.get(ClientInfo.TOKEN)));

            // put log
            userLogService.putUserAccessLog(AccessUserInfoDto.build(
                    logoutResponseDto,
                    UserLogMessage.LOGOUT,
                    accessInfo.get(ClientInfo.IP_ADDRESS),
                    accessInfo.get(ClientInfo.USER_AGENT))
            );

            return ResponseEntity.ok("success");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/all")
    public ResponseEntity<List<UserResponseDto>> findAllUsers(@RequestParam(required = false) UserState[] states) {
        try {
            List<UserResponseDto> users = userService.findAllUsers(states == null ? new ArrayList<>() : List.of(states));

            return ResponseEntity.ok(users);
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @GetMapping("/{id}")
    public ResponseEntity<UserResponseDto> findUserById(@PathVariable("id") Long id) {
        try {
            UserResponseDto userDto = userService.findById(id);

            return ResponseEntity.ok(userDto);
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

            return ResponseEntity.ok(userDto);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/username")
    public ResponseEntity<String> updateUsername(HttpServletRequest request, @RequestBody String givenUsername) {
        try {
            // parse token from request
            String accessToken = ClientMapper.parseAuthToken(request);

            long userId = userService.parseUserIdFromToken(accessToken);
            UserResponseDto previousUser = userService.findById(userId);

            UserResponseDto postUser = userService.updateUsernameById(previousUser.id(), givenUsername);

            userLogService.putUpdateInfoLog(UpdateUserInfoDto.build(
                    postUser,
                    UserLogMessage.UPDATE_USERNAME,
                    previousUser.username(),
                    givenUsername)
            );

            return ResponseEntity.ok("success");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/password")
    public ResponseEntity<String> updatePassword(HttpServletRequest request, @RequestBody String givenPassword) {
        try {
            // parse client access into
            Map<ClientInfo, String> clientInfo = ClientMapper.parseAccessInfo(request);

            long userId = userService.parseUserIdFromToken(clientInfo.get(ClientInfo.TOKEN));
            UserResponseDto userDto = userService.updatePasswordById(userId, givenPassword);

            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.build(
                    userDto,
                    UserLogMessage.UPDATE_PASSWORD,
                    clientInfo.get(ClientInfo.IP_ADDRESS),
                    clientInfo.get(ClientInfo.USER_AGENT))
            );

            return ResponseEntity.ok("success");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/state")
    public ResponseEntity<String> updateState(HttpServletRequest request, @RequestBody UserState givenState) {
        try {
            String accessToken = ClientMapper.parseAuthToken(request);

            long userId = userService.parseUserIdFromToken(accessToken);
            UserResponseDto previousUserDto = userService.findById(userId);
            UserResponseDto userDto = userService.updateStateById(userId, givenState);

            userLogService.putUpdateInfoLog(UpdateUserInfoDto.build(
                    userDto,
                    UserLogMessage.UPDATE_STATE,
                    previousUserDto.state().name(),
                    givenState.name())
            );

            return ResponseEntity.ok("success");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<String> withdrawUser(HttpServletRequest request) {
        try {
            // parse client access into
            Map<ClientInfo, String> clientInfo = ClientMapper.parseAccessInfo(request);

            long userId = userService.parseUserIdFromToken(clientInfo.get(ClientInfo.TOKEN));
            UserResponseDto previousUserDto = userService.findById(userId);
            userService.updateStateById(userId, UserState.WITHDRAWN);

            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.build(
                    previousUserDto,
                    UserLogMessage.DELETE_USER,
                    clientInfo.get(ClientInfo.IP_ADDRESS),
                    clientInfo.get(ClientInfo.USER_AGENT))
            );

            return ResponseEntity.ok("success");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * 비밀번호 초기화 전 이메일 주소 인증
     *
     * @param request
     * @param username
     * @return
     */
    @PostMapping("/find-password")
    public ResponseEntity<String> findPassword(HttpServletRequest request, @RequestBody String username) {
        try {
            String serverAddress = ClientMapper.parseServerName(request);
            String port = ClientMapper.parseServerPort(request);

            // authenticate username and send verification code by mail
            userService.findPassword(FindPasswordDto
                    .build(username,serverAddress, port)
            );

            return ResponseEntity.ok("success");
        } catch (NotFoundException | UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        }catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    /**
     * verificationCode와 username 쿼리스트링으로 받아서 비밀번호 초기화 진행
     *
     * @param request
     * @param username
     * @param verificationCode
     * @return
     */
    @PatchMapping("/reset-password")
    public ResponseEntity<String> resetPassword(HttpServletRequest request, @RequestParam String username, @RequestParam String verificationCode) {
        try {
            // authenticate username and reset password
            userService.resetPassword(username, verificationCode);

            // parse client access into
            Map<ClientInfo, String> clientInfo = ClientMapper.parseAccessInfo(request);
            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.build(
                    userService.findByUsername(username),
                    UserLogMessage.UPDATE_PASSWORD,
                    clientInfo.get(ClientInfo.IP_ADDRESS),
                    clientInfo.get(ClientInfo.USER_AGENT))
            );

            return ResponseEntity.ok("success");
        } catch (UnauthorizedException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (ForbiddenException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
