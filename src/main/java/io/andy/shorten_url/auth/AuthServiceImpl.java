package io.andy.shorten_url.auth;

import io.andy.shorten_url.util.mail.MailService;
import io.andy.shorten_url.util.mail.dto.MailMessageDto;
import io.andy.shorten_url.util.random.RandomUtility;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.stereotype.Service;

import static io.andy.shorten_url.auth.AuthPolicy.SECRET_CODE_LENGTH;

@Slf4j
@Service
public class AuthServiceImpl implements AuthService {
    private final MailService mailService;
    private final RandomUtility randomUtility;

    public AuthServiceImpl(
            MailService mailService,
            @Qualifier("SecretCodeGenerator") RandomUtility randomUtility
    ) {
        this.mailService = mailService;
        this.randomUtility = randomUtility;
    }

    @Override
    public String sendEmailAuthCode(String recipient) throws MailException, MessagingException {
        try {
            String secretCode = randomUtility.generate(SECRET_CODE_LENGTH);
            String subject = "[Shorten-url] 이메일 인증";
            String body = "<h3>요청하신 인증 번호입니다.</h3><br>"+secretCode+"<br>";
            MailMessageDto messageDto = new MailMessageDto(recipient, subject, body);

            MimeMessage message = mailService.createMailMessage(messageDto);
            mailService.sendMail(recipient, message);

            return secretCode;
        } catch (MessagingException | MailException e) {
            log.error("failed to send email auth code, recipient = {}, error message = {}, stack trace = {}", recipient, e.getMessage(), e.getStackTrace());
            throw e;
        }
    }
}
