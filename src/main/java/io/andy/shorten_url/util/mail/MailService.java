package io.andy.shorten_url.util.mail;

import io.andy.shorten_url.config.MailConfig;
import io.andy.shorten_url.util.random.RandomUtility;
import io.andy.shorten_url.util.random.SecretCodeGenerator;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import static io.andy.shorten_url.util.mail.MailPolicy.SECRET_CODE_LENGTH;

@Slf4j
@Service
public class MailService {
    private final MailConfig mailConfig;
    private final JavaMailSender javaMailSender;
    private final RandomUtility randomUtility;

    @Autowired
    public MailService(MailConfig mailConfig, JavaMailSender javaMailSender) {
        this.mailConfig = mailConfig;
        this.javaMailSender = javaMailSender;
        this.randomUtility = new SecretCodeGenerator();
    }

    public String sendMail(String recipient) throws MessagingException {
        String secretCode = randomUtility.generate(SECRET_CODE_LENGTH);
        log.debug("generated secret code "+secretCode);

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
