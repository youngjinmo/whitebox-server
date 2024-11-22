package io.andy.shorten_url.user.controller;

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

            UserResponseDto userDto = userService.createByEmail(CreateUserServiceDto.from(signUpDto));
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

            UserLoginResponseDto userDto = userService.login(UserLoginServiceDto.build(
                    loginDto,
                    accessInfo.get(ClientInfo.IP_ADDRESS),
                    accessInfo.get(ClientInfo.USER_AGENT))
            );
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
    public ResponseEntity<Void> sendEmailAuth(@RequestBody String recipient) {
        try {
            if (userService.isDuplicateUsername(recipient)) {
                throw new ForbiddenException("EMAIL DUPLICATED");
            }
            this.userService.sendEmailAuthCode(recipient);

            return ResponseEntity.ok().build();
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

            return ResponseEntity.ok("verified");
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        }
    }

    @DeleteMapping("/logout")
    public ResponseEntity<Void> logout(HttpServletRequest request) {
        try {
            // parse client access into
            Map<ClientInfo, String> accessInfo = ClientMapper.parseAccessInfo(request);

            // verify token
            UserLogoutResponseDto logoutResponseDto = userService.logout(UserLogoutRequestDto.build(accessInfo.get(ClientInfo.TOKEN)));
            userLogService.putUserAccessLog(AccessUserInfoDto.build(
                    logoutResponseDto,
                    UserLogMessage.LOGOUT,
                    accessInfo.get(ClientInfo.IP_ADDRESS),
                    accessInfo.get(ClientInfo.USER_AGENT))
            );

            return ResponseEntity.ok().build();
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
    public ResponseEntity<UserResponseDto> updateUsername(HttpServletRequest request, @RequestBody String givenUsername) {
        try {
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

            return ResponseEntity.ok(postUser);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/password")
    public ResponseEntity<UserResponseDto> updatePassword(HttpServletRequest request, String givenPassword) {
        try {
            // parse client access into
            Map<ClientInfo, String> clientInfo = ClientMapper.parseAccessInfo(request);

            long userId = userService.parseUserIdFromToken(clientInfo.get(ClientInfo.TOKEN));
            UserResponseDto userDto = userService.updatePasswordById(userId, givenPassword);

            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.build(
                    userDto,
                    UserLogMessage.DELETE_USER,
                    clientInfo.get(ClientInfo.IP_ADDRESS),
                    clientInfo.get(ClientInfo.USER_AGENT))
            );

            return ResponseEntity.ok(userDto);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/state")
    public ResponseEntity<UserResponseDto> updateState(HttpServletRequest request, @RequestBody UserState givenState) {
        try {
            String accessToken = ClientMapper.parseAuthToken(request);

            long userId = userService.parseUserIdFromToken(accessToken);
            UserResponseDto previousUserDto = userService.findById(userId);
            UserResponseDto userDto = userService.updateStateById(userId, givenState);

            userLogService.putUpdateInfoLog(UpdateUserInfoDto.build(
                    previousUserDto,
                    UserLogMessage.UPDATE_STATE,
                    previousUserDto.state().name(),
                    givenState.name())
            );

            return ResponseEntity.ok(userDto);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @DeleteMapping("/withdraw")
    public ResponseEntity<Void> withdrawUser(HttpServletRequest request) {
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

            return ResponseEntity.ok().build();
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }

    @PatchMapping("/reset-password")
    public ResponseEntity<String> resetPassword(HttpServletRequest request, @RequestBody String username) {
        try {
            // parse client access into
            Map<ClientInfo, String> clientInfo = ClientMapper.parseAccessInfo(request);

            UserResponseDto userDto = userService.findByUsername(username);
            String newPassword = userService.resetPassword(userDto.id());

            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.build(
                    userDto,
                    UserLogMessage.UPDATE_PASSWORD,
                    clientInfo.get(ClientInfo.IP_ADDRESS),
                    clientInfo.get(ClientInfo.USER_AGENT))
            );

            return ResponseEntity.ok(newPassword);
        } catch (UnauthorizedException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, e.getMessage());
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage());
        }
    }
}
