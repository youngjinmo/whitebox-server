package io.andy.shorten_url.user.service;

import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.session.SessionService;
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
import io.andy.shorten_url.util.mail.MailService;
import io.andy.shorten_url.util.random.RandomUtility;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@Transactional
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final UserLogService UserLogService;
    private final MailService mailService;
    private final SessionService sessionService;
    private final PasswordEncoder passwordEncoder;
    private final RandomUtility randomUtility;

    @Autowired
    UserServiceImpl(
            UserRepository userRepository,
            UserLogService UserLogService,
            MailService mailService,
            SessionService sessionService,
            PasswordEncoder passwordEncoder,
            RandomUtility randomUtility
    ) {
        this.userRepository = userRepository;
        this.UserLogService = UserLogService;
        this.mailService = mailService;
        this.sessionService = sessionService;
        this.passwordEncoder = passwordEncoder;
        this.randomUtility = randomUtility;
    }

    @Override
    public UserResponseDto createUserByUsername(UserSignUpDto userDto, String password) {
        if (isDuplicateUsername(userDto.username())) {
            log.debug("this username is already exists = {}", userDto.username());
            throw new BadRequestException("DUPLICATE USERNAME");
        }

        try {
            User user = userRepository.save(new User(
                    userDto.username(),
                    passwordEncoder.encode(password),
                    UserState.NEW,
                    UserRole.USER
            ));

            UserResponseDto userResponseDto = new UserResponseDto(user);
            log.debug("created user: {}", userResponseDto);
            UserLogService.putUserAccessLog(
                    new AccessInfoDto(
                            userResponseDto,
                            UserLogMessage.SIGNUP,
                            userDto.ipAddress(),
                            userDto.userAgent()
                    )
            );

            return userResponseDto;
        } catch (Exception e) {
            log.error("failed to create user={}. error message={}", userDto, e.getMessage());
            throw new InternalServerException("FAILED TO CREATE USER BY");
        }
    }

    @Override
    public UserResponseDto login(UserLoginDto userDto, String password) {
        Optional<User> optionalUser = userRepository.findByUsername(userDto.username());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (passwordEncoder.matches(password, user.getPassword())) {

                user.setState(UserState.NORMAL);
                user.setLastLoginAt(LocalDateTime.now());

                UserResponseDto userResponseDto = new UserResponseDto(user);
                // TODO save session

                log.info("user logined={}", userResponseDto.id());
                UserLogService.putUserAccessLog(
                        new AccessInfoDto(
                                userResponseDto,
                                UserLogMessage.LOGIN,
                                userDto.ipAddress(),
                                userDto.userAgent()
                        )
                );

                return userResponseDto;
            }
            log.debug("user failed to login by invalid password, id={}", user.getId());
            throw new UnauthorizedException("INVALID PASSWORD");
        }

        log.debug("failed to login by invalid username: {}", userDto.username());
        throw new UnauthorizedException("INVALID USERNAME");
    }

    @Override
    public void logout(UserLogOutDto userDto) {
        UserResponseDto userResponseDto = this.findById(userDto.id());
        // TODO remove session

        log.info("user logout, id={}", userResponseDto.id());
        UserLogService.putUserAccessLog(
                new AccessInfoDto(
                        userResponseDto,
                        UserLogMessage.LOGOUT,
                        userDto.ipAddress(),
                        userDto.userAgent()
                )
        );
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
            UserResponseDto userResponseDto = new UserResponseDto(user);
            UserLogService.putUpdateInfoLog(
                    new UpdateInfoDto(
                            userResponseDto,
                            UserLogMessage.UPDATE_USERNAME,
                            previousUsername,
                            username)
            );

            return userResponseDto;
        }
        log.debug("failed to update username by invalid id={}, username={}", id, username);
        throw new NotFoundException("FAILED TO UPDATE USERNAME BY INVALID ID");
    }

    @Override
    public UserResponseDto updatePasswordById(Long id, String password) {
        Optional<User> originUser = userRepository.findById(id);
        if (originUser.isPresent()) {
            User user = originUser.get();

            user.setPassword(passwordEncoder.encode(password));

            log.info("updated password by id={}", id);
            UserResponseDto userResponseDto = new UserResponseDto(user);
            UserLogService.putUpdateInfoLog(
                    new UpdatePrivacyInfoDto(
                            userResponseDto,
                            UserLogMessage.UPDATE_PASSWORD)
            );

            return userResponseDto;
        }
        log.debug("failed to update password by invalid id={}", id);
        throw new NotFoundException("FAILED TO UPDATE PASSWORD BY INVALID ID");
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
            UserResponseDto userResponseDto = new UserResponseDto(user);
            UserLogService.putUpdateInfoLog(
                    new UpdateInfoDto(
                            userResponseDto,
                            UserLogMessage.UPDATE_STATE,
                            String.valueOf(previousState),
                            String.valueOf(state)));

            return userResponseDto;
        }
        log.debug("failed to update state by invalid id={}", id);
        throw new NotFoundException("FAILED TO UPDATE STATE BY INVALID ID");
    }

    @Override
    public void deleteById(UserDeleteDto userDto) {
        Optional<User> originUser = userRepository.findById(userDto.id());
        if (originUser.isPresent()) {
            try {
                User user = originUser.get();
                user.setState(UserState.DELETED);
                user.setDeletedAt(LocalDateTime.now());
                // soft delete
                user.setUsername(EncodeUtil.encrypt(user.getUsername()));
                user.setPassword(EncodeUtil.encrypt(user.getPassword()));

                // TODO if session exists, remove it.

                log.info("user deleted. id={}, ip={}, user-agent={}", userDto.id(), userDto.ipAddress(), userDto.userAgent());
                UserResponseDto userResponseDto = new UserResponseDto(user);
                UserLogService.putUserAccessLog(
                        new AccessInfoDto(
                                userResponseDto,
                                UserLogMessage.DELETE_USER,
                                userDto.ipAddress(),
                                userDto.userAgent()
                        )
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
    public String resetPassword(String username) {
        // TODO create privacy url path
        String secretCode = randomUtility.generate(20);

        // TODO save it into session with ttl

        // TODO send it into email
        try {
            String title = "[Shorten-url] 비밀번호 찾기 링크 전송";
            String message = "비밀번호 찾기를 위한 링크입니다.<br>아래 링크로 접속하면 비밀번호가 초기화됩니다.";
            MimeMessage mail = mailService.createHTMLMailMessage(title, message, username);
            mailService.sendEmail(mail);
        } catch (MessagingException e) {
            log.error("failed to send email for find password, to={}", username);
            throw new InternalServerException("FAILED TO SEND MAIL");
        }

        // TODO return secret code
        return "";
    }
}
