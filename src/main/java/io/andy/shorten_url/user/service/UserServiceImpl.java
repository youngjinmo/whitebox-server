package io.andy.shorten_url.user.service;

import io.andy.shorten_url.auth.AuthService;
import io.andy.shorten_url.auth.token.dto.CreateAuthTokenRequestDto;
import io.andy.shorten_url.auth.token.dto.VerifyAuthTokenDto;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.entity.User;
import io.andy.shorten_url.user.repository.UserRepository;
import io.andy.shorten_url.user_log.constant.UserLogMessage;
import io.andy.shorten_url.user_log.dto.AccessInfoDto;
import io.andy.shorten_url.user_log.dto.UpdateInfoDto;
import io.andy.shorten_url.user_log.dto.UpdatePrivacyInfoDto;
import io.andy.shorten_url.user_log.service.UserLogService;
import io.andy.shorten_url.util.encrypt.EncodeUtil;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import static io.andy.shorten_url.auth.AuthPolicy.RESET_PASSWORD_LENGTH;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final AuthService authService;
    private final UserLogService userLogService;
    private final UserRepository userRepository;

    @Override
    public UserResponseDto createUserByUsername(UserSignUpDto userDto, String password) {
        if (isDuplicateUsername(userDto.username())) {
            log.debug("this username is already exists = {}", userDto.username());
            throw new BadRequestException("DUPLICATE USERNAME");
        }

        try {
            User user = userRepository.save(new User(
                    userDto.username(),
                    authService.encodePassword(password),
                    UserState.NEW,
                    UserRole.USER
            ));

            UserResponseDto userResponseDto = new UserResponseDto(user);
            log.debug("created user: {}", userResponseDto);
            userLogService.putUserAccessLog(
                    AccessInfoDto.builder()
                            .userId(user.getId())
                            .role(user.getRole())
                            .state(user.getState())
                            .message(UserLogMessage.SIGNUP)
                            .ipAddress(userDto.ipAddress())
                            .userAgent(userDto.userAgent())
                            .build()
            );

            return userResponseDto;
        } catch (Exception e) {
            log.error("failed to create user={}. error message={}", userDto, e.getMessage());
            throw new InternalServerException("FAILED TO CREATE USER BY");
        }
    }

    @Override
    @Transactional
    public UserLoginResponseDto login(UserLoginRequestDto userDto, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(userDto.username());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (authService.matchPassword(password, user.getPassword())) {

                // 로그인으로 인한 정보 변경 (상태, 최근접속일)
                if (user.getState().equals(UserState.NEW)) {
                    // 첫 로그인이면 상태 변경
                    user.setState(UserState.NORMAL);
                }
                user.setLastLoginAt(LocalDateTime.now());

                // 액세스 토큰 발행
                String accessToken = authService.grantAccessToken(new CreateAuthTokenRequestDto(user.getId(), userDto.userAgent()));

                log.info("user logined={}", user.getId());
                userLogService.putUserAccessLog(
                        AccessInfoDto.builder()
                                .userId(user.getId())
                                .role(user.getRole())
                                .state(user.getState())
                                .message(UserLogMessage.LOGIN)
                                .ipAddress(userDto.ipAddress())
                                .userAgent(userDto.userAgent())
                                .build()
                );

                return new UserLoginResponseDto(user, accessToken);
            }
            log.debug("user failed to login by invalid password, id={}", user.getId());
            throw new UnauthorizedException("INVALID PASSWORD");
        }

        log.debug("failed to login by invalid username: {}", userDto.username());
        throw new UnauthorizedException("INVALID USERNAME");
    }

    @Override
    @Transactional
    public void logout(UserLogOutDto userDto) {
        try {
            // user id 검증
            UserResponseDto userResponseDto = this.findById(userDto.id());

            // access token 검증 및 revoke token
            authService.revokeAccessToken(
                    new VerifyAuthTokenDto(
                            userDto.id(),
                            userDto.userAgent(),
                            userDto.accessToken()
                    )
            );

            log.info("user logout, id={}", userResponseDto.id());
            userLogService.putUserAccessLog(
                    AccessInfoDto.builder()
                            .userId(userDto.id())
                            .state(userResponseDto.state())
                            .role(userResponseDto.role())
                            .message(UserLogMessage.LOGOUT)
                            .ipAddress(userDto.ipAddress())
                            .userAgent(userDto.userAgent())
                            .build()
            );
        } catch (NotFoundException e) {
            throw new UnauthorizedException(e.getMessage());
        }
    }

    @Override
    public List<UserResponseDto> findAllUsers() {
        return userRepository
                .findAll()
                .stream()
                .map(UserResponseDto::new)
                .collect(Collectors.toList());
    }

    @Override
    public UserResponseDto findById(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return new UserResponseDto(user);
        }
        throw new NotFoundException("USER NOT FOUND");
    }

    @Override
    public UserResponseDto findByUsername(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return new UserResponseDto(user);
        }
        log.debug("user failed to login by invalid username={}", username);
        throw new NotFoundException("USER NOT FOUND BY USERNAME");
    }

    @Override
    @Transactional
    public UserResponseDto updateUsernameById(Long id, String username) {
        Optional<User> originUser = userRepository.findById(id);
        if (originUser.isPresent()) {
            if (isDuplicateUsername(username)) {
                log.debug("this username is already exists. userId={}, username={}", id, username);
                throw new BadRequestException("DUPLICATE USERNAME");
            }
            User user = originUser.get();
            String previousUsername = user.getUsername();

            user.setUsername(username);
            user.setUpdatedAt(LocalDateTime.now());

            log.info("updated username by id={}", id);
            userLogService.putUpdateInfoLog(
                    UpdateInfoDto.builder()
                            .userId(id)
                            .state(user.getState())
                            .role(user.getRole())
                            .message(UserLogMessage.UPDATE_USERNAME)
                            .preValue(previousUsername)
                            .postValue(username)
                            .build()
            );

            return new UserResponseDto(user);
        }
        log.debug("failed to update username by invalid id={}, username={}", id, username);
        throw new NotFoundException("FAILED TO UPDATE USERNAME BY INVALID ID");
    }

    @Override
    @Transactional
    public UserResponseDto updatePasswordById(Long id, String password) {
        Optional<User> userEntity = userRepository.findById(id);
        if (userEntity.isPresent()) {
            User user = userEntity.get();

            user.setPassword(authService.encodePassword(password));
            user.setUpdatedAt(LocalDateTime.now());

            log.info("updated password by id={}", id);
            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.builder()
                            .userId(user.getId())
                            .role(user.getRole())
                            .state(UserState.NORMAL)
                            .message(UserLogMessage.UPDATE_PASSWORD)
                    .build());

            return new UserResponseDto(user);
        }
        log.debug("failed to update password by invalid id={}", id);
        throw new NotFoundException("FAILED TO UPDATE PASSWORD BY INVALID ID");
    }

    @Override
    @Transactional
    public UserResponseDto updateStateById(Long id, UserState state) {
        Optional<User> userEntity = userRepository.findById(id);
        if (userEntity.isPresent()) {
            User user = userEntity.get();
            UserState previousState = user.getState();

            user.setState(state);
            user.setUpdatedAt(LocalDateTime.now());

            log.info("updated state into {} by id={}", state, id);
            userLogService.putUpdateInfoLog(
                    UpdateInfoDto.builder()
                            .userId(user.getId())
                            .state(previousState)
                            .role(user.getRole())
                            .message(UserLogMessage.UPDATE_STATE)
                            .preValue(previousState.toString())
                            .postValue(state.toString())
                            .build());

            return new UserResponseDto(user);
        }
        log.debug("failed to update state by invalid id={}", id);
        throw new NotFoundException("FAILED TO UPDATE STATE BY INVALID ID");
    }

    @Override
    @Transactional
    public void deleteById(UserDeleteDto userDto) {
        Optional<User> userEntity = userRepository.findById(userDto.id());
        if (userEntity.isPresent()) {
            try {
                User user = userEntity.get();
                user.setState(UserState.DELETED);
                user.setDeletedAt(LocalDateTime.now());

                // soft delete
                user.setUsername(EncodeUtil.encrypt(user.getUsername()));
                user.setPassword(EncodeUtil.encrypt(user.getPassword()));

                authService.revokeAllTokensByUserId(userDto.id());

                log.info("user deleted. id={}, ip={}, user-agent={}", userDto.id(), userDto.ipAddress(), userDto.userAgent());
                userLogService.putUpdateInfoLog(
                        UpdatePrivacyInfoDto.builder()
                                .userId(userDto.id())
                                .role(user.getRole())
                                .state(user.getState())
                                .message(UserLogMessage.DELETE_USER)
                                .ipAddress(userDto.ipAddress())
                                .userAgent(userDto.userAgent())
                                .build()
                );
            } catch (Exception e) {
                log.error("failed to delete user by id={}. error message={}", userDto.id(), e.getMessage());
                throw new InternalServerException("FAILED TO DELETE USER BY ID");
            }
        }
    }

    @Override
    public boolean isDuplicateUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.isPresent();
    }

    @Override
    @Transactional
    public String resetPassword(Long id) {
        Optional<User> userEntity = userRepository.findById(id);
        if (userEntity.isPresent()) {
            User user = userEntity.get();
            String tempPassword = authService.generateResetPassword(RESET_PASSWORD_LENGTH);
            user.setPassword(authService.encodePassword(tempPassword));
            log.info("success to reset password. user id={}", id);
            userLogService.putUpdateInfoLog(UpdatePrivacyInfoDto.builder()
                    .userId(id)
                    .role(user.getRole())
                    .state(user.getState())
                    .message(UserLogMessage.UPDATE_PASSWORD)
                    .build());
            return tempPassword;
        }
        log.debug("failed to reset password by invalid id={}", id);
        throw new NotFoundException("FAILED TO RESET PASSWORD BY DOES NOT EXIST USER");
    }
}
