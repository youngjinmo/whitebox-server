package io.andy.shorten_url.util.mail;

import io.andy.shorten_url.config.MailConfig;
import io.andy.shorten_url.util.random.RandomUtility;
import io.andy.shorten_url.util.random.SecretCodeGenerator;

import jakarta.mail.Message.RecipientType;
import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import static io.andy.shorten_url.util.mail.MailPolicy.EMAIL_AUTH_SECRET_CODE_LENGTH;

@Slf4j
@Service
public class MailService {
    @Autowired private MailConfig mailConfig;
    @Autowired private JavaMailSender javaMailSender;
    private final RandomUtility randomUtility = new SecretCodeGenerator();

    // TODO refactor this service
    public MimeMessage createHTMLMailMessage(String title, String message, String recipient) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        mimeMessage.setFrom(mailConfig.getUsername());
        mimeMessage.setRecipients(RecipientType.TO, recipient);
        mimeMessage.setSubject(title);
        mimeMessage.setText(message, "UTF-8", "html");

        return mimeMessage;
    }

    public void sendEmail(MimeMessage message) throws MessagingException {
        javaMailSender.send(message);
        log.info("mail sent. subject={}, to={}", message.getSubject(), message.getRecipients(RecipientType.TO));
    }

    public String sendMail(String recipient) throws MessagingException {
        String secretCode = randomUtility.generate(EMAIL_AUTH_SECRET_CODE_LENGTH);
        log.debug("generated secret code={}", secretCode);

        MimeMessage mimeMessage = createEmailAuthMessage(secretCode, recipient);
        javaMailSender.send(mimeMessage);

        return secretCode;
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
