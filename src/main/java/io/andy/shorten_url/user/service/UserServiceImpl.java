package io.andy.shorten_url.user.service;

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
import io.andy.shorten_url.util.EncodeUtil;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@Transactional
public class UserServiceImpl implements UserService {
    @Autowired private final PasswordEncoder passwordEncoder;
    @Autowired private final UserLogService userLogService;
    @Autowired private final UserRepository userRepository;
    private final Logger logger;

    public UserServiceImpl(PasswordEncoder passwordEncoder, UserLogService userLogService, UserRepository userRepository) {
        this.passwordEncoder = passwordEncoder;
        this.userLogService = userLogService;
        this.userRepository = userRepository;
        this.logger = LoggerFactory.getLogger(UserServiceImpl.class);
    }

    @Override
    public UserResponseDto createUserByUsername(UserSignUpDto userDto) {
        if (isDuplicateUsername(userDto.getUsername())) {
            log.debug("this username is already exists = {}", userDto.getUsername());
            throw new IllegalStateException("DUPLICATE USERNAME");
        }

        try {
            User user = userRepository.save(new User(
                    userDto.username(),
                    passwordEncoder.encode(userDto.password()),
                    UserState.NEW,
                    UserRole.USER
            ));

            UserResponseDto userResponseDto = new UserResponseDto(user);
            log.debug("created user: {}", userResponseDto);
            putAccessLog(
                    userResponseDto,
                    UserLogMessage.SIGNUP,
                    userDto.getIp(),
                    userDto.getUserAgent()
            );

            return userResponseDto;
        } catch (Exception e) {
            log.error("failed to create user={}. error message={}", userDto, e.getMessage());
            throw new InternalServerException("FAILED TO DELETE USER BY ID");
        }
    }

    @Override
    public UserResponseDto login(UserLoginDto userDto) {
        Optional<User> optionalUser = userRepository.findByUsername(userDto.getUsername());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(userDto.getPassword(), user.getPassword())) {

                user.setState(UserState.NORMAL);
                user.setLastLoginAt(LocalDateTime.now());

                UserResponseDto userResponseDto = new UserResponseDto(user);

                log.info("user logined={}", userResponseDto.getId());
                putAccessLog(userResponseDto, UserLogMessage.LOGIN, userDto.getIp(), userDto.getUserAgent());

                return userResponseDto;
            }
            logger.debug("user failed to login by invalid password, id={}", user.getId());
            throw new UnauthorizedException("INVALID_PASSWORD");
        }

        log.debug("failed to login by invalid username: {}", userDto.getUsername());
        throw new UnauthorizedException("INVALID_USERNAME");
    }

    @Override
    public void logout(UserLogOutDto userDto) {
        UserResponseDto userResponseDto = this.findById(userDto.getId());
        log.info("user logout, id={}", userResponseDto.getId());
        this.putAccessLog(
                userResponseDto,
                UserLogMessage.LOGOUT,
                userDto.getClientIp(),
                userDto.getUserAgent()
        );
    }

    @Override
    public List<UserResponseDto> findAllUsers() {
        /*
            1. userRepository.findAll()로 반환된 List<User>를 스트림으로 변환합니다.
            2. map(UserResponseDto::new)을 통해 각 User 객체를 UserResponseDto 생성자로 전달하여 변환합니다.
            3. collect(Collectors.toList())를 사용해 스트림을 다시 List<UserResponseDto>로 변환하여 반환합니다.
         */
        List<User> users = userRepository.findAll();
        return users.stream().map(UserResponseDto::new).collect(Collectors.toList());
    }

    @Override
    public UserResponseDto findById(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return new UserResponseDto(user);
        }
        throw new NotFoundException("USER_NOT_FOUND");
    }

    @Override
    public UserResponseDto findByUsername(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return new UserResponseDto(user);
        }
        logger.debug("user failed to login by invalid username={}", username);
        throw new NotFoundException("USER NOT FOUND BY USERNAME");
    }

    @Override
    public UserResponseDto updateUsernameById(Long id, String username) {
        Optional<User> originUser = userRepository.findById(id);
        if (originUser.isPresent()) {
            if (isDuplicateUsername(username)) {
                log.debug("this username is already exists. userId={}, username={}", id, username);
                throw new BadRequestException("DUPLICATE USERNAME");
            }
            User user = originUser.get();
            String previousName = user.getUsername();

            user.setUsername(username);
            user.setUpdatedAt(LocalDateTime.now());

            log.info("updated username by id={}", id);
            UserResponseDto userResponseDto = new UserResponseDto(userRepository.findById(id).get());
            putInfoLog(
                    userResponseDto,
                    UserLogMessage.UPDATE_USERNAME,
                    previousName,
                    username
            );

            return userResponseDto;
        }
        logger.debug("failed to update username by invalid id={}, username={}", id, username);
        throw new BadRequestException("FAILED TO UPDATE USERNAME BY INVALID ID");
    }

    @Override
    public UserResponseDto updatePasswordById(Long id, String password) {
        Optional<User> originUser = userRepository.findById(id);
        if (originUser.isPresent()) {
            User user = originUser.get();

            user.setPassword(passwordEncoder.encode(password));
            user.setUpdatedAt(LocalDateTime.now());

            log.info("updated password by id={}", id);
            UserResponseDto userResponseDto = new UserResponseDto(userRepository.findById(id).get());
            putInfoLog(userResponseDto, UserLogMessage.UPDATE_PASSWORD);

            return userResponseDto;
        }
        logger.debug("failed to update password by invalid id={}", id);
        throw new BadRequestException("FAILED TO UPDATE PASSWORD BY INVALID ID");
    }

    @Override
    public UserResponseDto updateStateById(Long id, UserState state) {
        Optional<User> originUser = userRepository.findById(id);
        if (originUser.isPresent()) {
            User user = originUser.get();
            UserState previousState = user.getState();

            user.setState(state); // execution update by jpa
            user.setUpdatedAt(LocalDateTime.now());

            log.info("updated state into {} by id={}", state, id);
            UserResponseDto userResponseDto = new UserResponseDto(userRepository.findById(id).get());
            putInfoLog(
                    userResponseDto,
                    UserLogMessage.UPDATE_STATE,
                    String.valueOf(previousState),
                    String.valueOf(state)
            );

            return userResponseDto;
        }
        logger.debug("failed to update state by invalid id={}", id);
        throw new BadRequestException("FAILED TO UPDATE STATE BY INVALID ID");
    }

    @Override
    public void deleteById(UserDeleteDto userDto) {
        Optional<User> originUser = userRepository.findById(userDto.getId());
        if (originUser.isPresent()) {
            try {
                userRepository.deleteById(userDto.getId());
            } catch (Exception e) {

                log.error("failed to delete user by id={}. error message={}", userDto.getId(), e.getMessage());
                throw new InternalServerException("FAILED TO DELETE USER BY ID");
            }

                User user = originUser.get();
                user.setState(UserState.DELETED);
                user.setDeletedAt(LocalDateTime.now());
                encryptUser(user);

                log.info("user deleted. id={}, ip={}, user-agent={}", userDto.getId(), userDto.getClientIp(), userDto.getUserAgent());
                UserResponseDto userResponseDto = new UserResponseDto(user);
                putAccessLog(userResponseDto, UserLogMessage.DELETE_USER, userDto.getClientIp(), userDto.getUserAgent());
        }
    }

    private boolean isDuplicateUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.isPresent();
    }

    private void encryptUser(User user) {
        user.setUsername(EncodeUtil.encrypt(user.getUsername()));
        user.setPassword(EncodeUtil.encrypt(user.getPassword()));
    }

    private void putAccessLog(
            UserResponseDto userResponseDto,
            UserLogMessage logMessage,
            String ip,
            String userAgent
    ) {
        AccessInfoDto userLog = new AccessInfoDto(userResponseDto, logMessage, ip, userAgent);
        userLogService.putUserAccessLog(userLog);
    }

    private void putInfoLog(
            UserResponseDto userResponseDto,
            UserLogMessage logMessage,
            String previousValue,
            String postValue
    ) {
        UpdateInfoDto userLog = new UpdateInfoDto(userResponseDto, logMessage, previousValue, postValue);
        userLogService.putUpdateInfoLog(userLog);
    }

    private void putInfoLog(UserResponseDto userResponseDto, UserLogMessage logMessage) {
        UpdatePrivacyInfoDto userLog =
                new UpdatePrivacyInfoDto(userResponseDto, logMessage);
        userLogService.putUpdateInfoLog(userLog);
    }
}

