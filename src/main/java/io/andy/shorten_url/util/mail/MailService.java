package io.andy.shorten_url.util.mail;

import io.andy.shorten_url.config.MailConfig;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.util.random.RandomUtility;
import io.andy.shorten_url.util.validation.Validator;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import static io.andy.shorten_url.util.mail.MailPolicy.SECRET_CODE_LENGTH;

@Slf4j
@Service
public class MailService {
    private final MailConfig mailConfig;
    private final JavaMailSender javaMailSender;
    private final RandomUtility codeGenerator;

    public MailService(
            MailConfig mailConfig,
            JavaMailSender javaMailSender,
            @Qualifier("SecretCodeGenerator") RandomUtility codeGenerator
    ) {
        this.mailConfig = mailConfig;
        this.javaMailSender = javaMailSender;
        this.codeGenerator = codeGenerator;
    }

    public String sendMail(String recipient) throws MessagingException, MailException {
        if (!Validator.validateEmail(recipient)) {
            throw new BadRequestException("Invalid email address");
        }

        try {
            String secretCode = codeGenerator.generate(SECRET_CODE_LENGTH);
            log.debug("generated secret code = {}", secretCode);

            MimeMessage mimeMessage = createEmailAuthMessage(secretCode, recipient);
            javaMailSender.send(mimeMessage);

            return secretCode;
        } catch (MailException | MessagingException e) {
            log.error("failed to send email, to = {}, error = {}", recipient, e.getMessage());
            throw e;
        }
    }

    private MimeMessage createEmailAuthMessage(String secretCode, String recipient) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        mimeMessage.setFrom(mailConfig.getUsername());
        mimeMessage.setRecipients(MimeMessage.RecipientType.TO, recipient);
        mimeMessage.setSubject("[Shorten-url] 이메일 인증");
        String body = "<h3>요청하신 인증 번호입니다.</h3><br>"+secretCode+"<br>";
        mimeMessage.setText(body, "UTF-8", "html");

        return mimeMessage;
    }
}
