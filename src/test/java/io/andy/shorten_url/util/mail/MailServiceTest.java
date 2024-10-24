package io.andy.shorten_url.util.mail;

import io.andy.shorten_url.config.MailConfig;
import io.andy.shorten_url.util.random.SecretCodeGenerator;

import jakarta.mail.MessagingException;

import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static io.andy.shorten_url.util.mail.MailPolicy.SECRET_CODE_LENGTH;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MailServiceTest {
    @Mock private MailConfig mailConfig;
    @Mock private JavaMailSender mailSender;
    @Mock private SecretCodeGenerator secretCodeGenerator;
    @InjectMocks private MailService mailService;

    @Test
    @DisplayName("JavaMailSender의 send 호출 확인")
    void sendMail() throws MessagingException, MailException {
        // given
        String recipient = "dev.youngjinmo@gmail.com";
        String expectSecretCode = "secret";

        // when
        when(secretCodeGenerator.generate(SECRET_CODE_LENGTH)).thenReturn(expectSecretCode);
        when(mailSender.createMimeMessage()).thenReturn(mock(MimeMessage.class));
        when(mailConfig.getUsername()).thenReturn(recipient);
        doNothing().when(mailSender).send(any(MimeMessage.class));

        String code = mailService.sendMail(recipient);

        // then
        assertEquals(expectSecretCode, code);
        verify(mailSender, times(1)).createMimeMessage();
        verify(mailSender, times(1)).send(any(MimeMessage.class));
    }
}