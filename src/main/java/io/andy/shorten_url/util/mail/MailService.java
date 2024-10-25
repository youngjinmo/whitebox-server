package io.andy.shorten_url.util.mail;

import io.andy.shorten_url.config.MailConfig;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.util.mail.dto.MailMessageDto;
import io.andy.shorten_url.util.validation.Validator;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import lombok.extern.slf4j.Slf4j;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class MailService {
    private final MailConfig mailConfig;
    private final JavaMailSender javaMailSender;

    public MailService(MailConfig mailConfig, JavaMailSender javaMailSender) {
        this.mailConfig = mailConfig;
        this.javaMailSender = javaMailSender;
    }

    public void sendMail(String recipient, MimeMessage message) throws MailException {
        if (!Validator.validateEmail(recipient)) {
            throw new BadRequestException("INVALID EMAIL FORMAT");
        }
        try {
            javaMailSender.send(message);
        } catch (MailException e) {
            log.error("failed to send email, to = {}, error = {}", recipient, e.getMessage());
            throw e;
        }
    }

    public MimeMessage createMailMessage(MailMessageDto messageDto) throws MessagingException {
        MimeMessage mimeMessage = javaMailSender.createMimeMessage();
        mimeMessage.setFrom(mailConfig.getUsername());
        mimeMessage.setRecipients(MimeMessage.RecipientType.TO, messageDto.recipient());
        mimeMessage.setSubject(messageDto.subject());
        mimeMessage.setText(messageDto.messageBody(), "UTF-8", "html");

        return mimeMessage;
    }
}
