package io.andy.shorten_url.user.service;

import io.andy.shorten_url.auth.AuthService;
import io.andy.shorten_url.auth.token.dto.TokenRequestDto;
import io.andy.shorten_url.auth.token.dto.TokenResponseDto;
import io.andy.shorten_url.auth.token.dto.VerifyTokenDto;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.ForbiddenException;
import io.andy.shorten_url.exception.client.NotFoundException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;
import io.andy.shorten_url.user.constant.UserRole;
import io.andy.shorten_url.user.constant.UserState;
import io.andy.shorten_url.user.dto.*;
import io.andy.shorten_url.user.entity.User;
import io.andy.shorten_url.user.repository.UserRepository;
import io.andy.shorten_url.util.encrypt.EncodeUtil;
import io.andy.shorten_url.util.mail.MailService;
import io.andy.shorten_url.util.mail.dto.MailMessageDto;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.mail.MailException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {
    private final AuthService authService;
    private final MailService mailService;
    private final UserRepository userRepository;

    @Override
    @Transactional
    public UserResponseDto createByEmail(CreateUserServiceDto createUserDto) {
        if (isDuplicateUsername(createUserDto.username())) {
            log.debug("this username is already exists = {}", createUserDto.username());
            throw new BadRequestException("DUPLICATE USERNAME");
        }

        try {
            User user = userRepository.save(new User(
                    createUserDto.username(),
                    authService.encodePassword(createUserDto.password()),
                    UserState.NEW,
                    UserRole.USER
            ));

            UserResponseDto userResponseDto = UserResponseDto.from(user);
            log.debug("created user: {}", userResponseDto);

            return userResponseDto;
        } catch (Exception e) {
            log.error("failed to create user={}. error message={}", createUserDto, e.getMessage());
            throw new InternalServerException("FAILED TO CREATE USER BY");
        }
    }

    @Override
    @Transactional
    public UserLoginResponseDto login(UserLoginServiceDto userLoginDto) {
        Optional<User> optionalUser = userRepository.findByUsername(userLoginDto.username());
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            if (authService.matchPassword(userLoginDto.password(), user.getPassword())) {

                // 탈퇴중인 상태면 접속 불가
                if (List.of(UserState.WITHDRAWN, UserState.DELETED).contains(user.getState())) {
                    log.debug("failed to login by invalid user state, id={}, state={}", user.getId(), user.getState());
                    throw new ForbiddenException("INVALID STATE");
                }

                // 로그인으로 인한 정보 변경 (상태, 최근접속일)
                if (user.getState().equals(UserState.NEW)) {
                    // 첫 로그인이면 상태 변경
                    user.setState(UserState.NORMAL);
                }
                user.setLastLoginAt(LocalDateTime.now());

                // access token, refresh token 발행
                TokenResponseDto tokenResponseDto = authService.grantAuthToken(TokenRequestDto.build(user.getId(), userLoginDto.ipAddress(), userLoginDto.userAgent()));

                // integrate authentication into spring security
                Authentication authentication = new UsernamePasswordAuthenticationToken(userLoginDto.username(), userLoginDto.password());
                SecurityContextHolder.getContext().setAuthentication(authentication);

                log.info("user logined={}", user.getId());

                return UserLoginResponseDto.build(user, tokenResponseDto);
            }
            log.debug("user failed to login by invalid password, id={}", user.getId());
            throw new UnauthorizedException("INVALID PASSWORD");
        }

        log.debug("failed to login by invalid username: {}", userLoginDto.username());
        throw new UnauthorizedException("INVALID USERNAME");
    }

    @Override
    @Transactional
    public UserLogoutResponseDto logout(UserLogoutRequestDto userLogoutDto) {
        // parse claims from token
        VerifyTokenDto verifyTokenDto = authService.verifyAuthToken(userLogoutDto.accessToken());
        try {
            // user id 검증
            UserResponseDto userResponseDto = findById(verifyTokenDto.getUserId());

            // revoke token
            authService.revokeAuthToken(userLogoutDto.accessToken());

            log.info("user logout, id={}", userResponseDto.id());
            return UserLogoutResponseDto.from(userResponseDto);
        } catch (Exception e) {
            log.error("failed to logout userId={}. error message={}", verifyTokenDto.getUserId(), e.getMessage());
            throw new UnauthorizedException("FAILED TO LOGOUT");
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<UserResponseDto> findAllUsers(List<UserState> states) {
        return userRepository.findByStateIn(states)
                .stream()
                .map(UserResponseDto::from)
                .collect(Collectors.toList());
    }

    @Override
    public long parseUserIdFromToken(String token) {
        return authService.verifyAuthToken(token).getUserId();
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findById(Long id) {
        Optional<User> optionalUser = userRepository.findById(id);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();

            return UserResponseDto.from(user);
        }
        throw new NotFoundException("USER NOT FOUND");
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponseDto findByUsername(String username) {
        Optional<User> optionalUser = userRepository.findByUsername(username);
        if (optionalUser.isPresent()) {
            User user = optionalUser.get();
            return UserResponseDto.from(user);
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

            user.setUsername(username);
            user.setUpdatedAt(LocalDateTime.now());

            log.info("updated username by id={}", id);
            return UserResponseDto.from(user);
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
            return UserResponseDto.from(user);
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

            user.setState(state);
            user.setUpdatedAt(LocalDateTime.now());

            log.info("updated state into {} by id={}", state, id);
            return UserResponseDto.from(user);
        }
        log.debug("failed to update state by invalid id={}", id);
        throw new NotFoundException("FAILED TO UPDATE STATE BY INVALID ID");
    }

    @Override
    @Transactional
    public void deleteById(DeleteUserServiceDto deleteUserDto) {
        Optional<User> userEntity = userRepository.findById(deleteUserDto.id());
        if (userEntity.isPresent()) {
            try {
                User user = userEntity.get();
                user.setState(UserState.DELETED);
                user.setDeletedAt(LocalDateTime.now());

                // soft delete
                user.setUsername(EncodeUtil.encrypt(user.getUsername()));
                user.setPassword(EncodeUtil.encrypt(user.getPassword()));

                // TODO delete link

                log.info("user deleted. id={}, ip={}, user-agent={}", deleteUserDto.id(), deleteUserDto.ipAddress(), deleteUserDto.userAgent());
            } catch (Exception e) {
                log.error("failed to delete user by id={}. error message={}", deleteUserDto.id(), e.getMessage());
                throw new InternalServerException("FAILED TO DELETE USER BY ID");
            }
        }
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isDuplicateUsername(String username) {
        Optional<User> user = userRepository.findByUsername(username);
        return user.isPresent();
    }

    @Override
    public void findPassword(FindPasswordDto findPasswordDto) {
        try {
            // authenticate username
            Optional<User> userEntity = userRepository.findByUsername(findPasswordDto.username());
            if (userEntity.isEmpty()) {
                throw new NotFoundException("USER NOT FOUND");
            }

            // create verification code
            String sessionKey = authService.createVerificationResetPasswordKey(findPasswordDto.username());

            // set verification code into redis
            String verificationCode = authService.setEmailVerificationCode(findPasswordDto.username(), sessionKey);

            // create email text box
            String resetPasswordLink = String.format("%s:%s/reset-password?username=%s&verificationCode=%s",
                    findPasswordDto.serverDomain(), findPasswordDto.port(), findPasswordDto.username(), verificationCode);

            String subject = "[Shorten-url] 비밀번호 초기화 링크";
            String body = "아래의 링크를 접속하면 비밀번호가 초기화됩니다.<br>"+resetPasswordLink;
            MailMessageDto messageDto = new MailMessageDto(findPasswordDto.username(), subject, body);
            MimeMessage message = mailService.createMailMessage(messageDto);

            // send email
            mailService.sendMail(findPasswordDto.username(), message);
            log.info("send reset password link to={}", findPasswordDto.username());
        } catch (MessagingException e) {
            log.error("failed to send reset password link email, to={}", findPasswordDto.username());
            throw new InternalServerException("FAILED SEND MAIL");
        }
    }

    @Override
    @Transactional
    public String resetPassword(String username, String verificationCode) {
        // verify code
        if (!authService.verifyResetPasswordCode(username, verificationCode)) {
           throw new ForbiddenException("EXPIRED VERIFICATION CODE");
        }

        Optional<User> userEntity = userRepository.findByUsername(username);
        if (userEntity.isEmpty()) {
            log.debug("failed to reset password by invalid username={}", username);
            throw new UnauthorizedException("FAILED TO RESET PASSWORD BY DOES NOT EXIST USER");
        }
        User user = userEntity.get();
        String tempPassword = authService.generateResetPassword();

        // TODO 추후 메일 템플릿 서비스로 코드 분리
        try {
            String subject = "[Shorten-url] 비밀번호 초기화";
            String body = "비밀번호가 아래의 번호로 초기화되었습니다.<br><h3>"+tempPassword+"</h3>";
            MailMessageDto messageDto = new MailMessageDto(user.getUsername(), subject, body);
            MimeMessage message = mailService.createMailMessage(messageDto);

            user.setPassword(authService.encodePassword(tempPassword));
            log.info("success to reset password. username={}", username);

            mailService.sendMail(user.getUsername(), message);
            log.info("sent new password to={}", username);

            return tempPassword;
        } catch (MessagingException | MailException e) {
            log.error("failed to send email reset password, recipient = {}, error message = {}, stack trace = {}",
                    user.getUsername(), e.getMessage(), e.getStackTrace());
            throw new InternalServerException("FAILED TO SEND RESET PASSWORD BY EMAIL");
        }
    }

    @Override
    @Transactional
    public void sendEmailAuthCode(String recipient) {
        try {
            if (isDuplicateUsername(recipient)) {
                throw new ForbiddenException("EMAIL DUPLICATED");
            }

            String sessionKey = authService.createVerificationEmailKey(recipient);
            String verificationCode = authService.setEmailVerificationCode(recipient, sessionKey);

            // TODO 추후 메일 템플릿 서비스로 코드 분리
            String subject = "[Shorten-url] 이메일 인증";
            String body = "<h3>"+verificationCode+"</h3><br>요청하신 인증번호입니다.<br>15분 안에 입력해주시기 바랍니다.";
            MailMessageDto messageDto = new MailMessageDto(recipient, subject, body);
            MimeMessage message = mailService.createMailMessage(messageDto);

            mailService.sendMail(recipient, message);
            log.info("verification email is sent, recipient={}", recipient);
        } catch (MessagingException | MailException e) {
            log.error("failed to send email auth code, recipient = {}, error message = {}, stack trace = {}", recipient, e.getMessage(), e.getStackTrace());
            throw new InternalServerException("FAILED TO SEND AUTH CODE BY EMAIL");
        }
    }

    @Override
    public void verifyEmail(String recipient, String verificationCode) {
        authService.verifyEmail(recipient, verificationCode);
    }
}
