package io.andy.shorten_url.util.mail;

import io.andy.shorten_url.config.MailConfig;
import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.util.mail.dto.MailMessageDto;

import jakarta.mail.MessagingException;
import jakarta.mail.internet.MimeMessage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import org.springframework.mail.MailException;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.ActiveProfiles;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ActiveProfiles("test")
@ExtendWith(MockitoExtension.class)
class MailServiceTest {
    @Mock private MailConfig mailConfig;
    @Mock private JavaMailSender javaMailSender;
    @InjectMocks private MailService mailService;

    @Test
    @DisplayName("JavaMailSender.send() 호출 확인")
    void sendMail() throws MailException {
        // given
        String recipient = "dev.youngjinmo@gmail.com";
        MimeMessage mimeMessage = mock(MimeMessage.class);

        // when
        doNothing().when(javaMailSender).send(mimeMessage);

        mailService.sendMail(recipient, mimeMessage);

        // then
        verify(javaMailSender, times(1)).send(any(MimeMessage.class));
    }

    @ParameterizedTest
    @ValueSource(strings = {"yj.com", "hello@"})
    @DisplayName("잘못된 메일로 발송 시도시 예외")
    void sendMailFailure(String recipient) {
        // given
        MimeMessage mimeMessage = mock(MimeMessage.class);

        // when
        BadRequestException exception = assertThrows(BadRequestException.class, () -> mailService.sendMail(recipient, mimeMessage));

        // then
        assertEquals("INVALID EMAIL FORMAT", exception.getMessage());
    }

    @Test
    @DisplayName("MailMessage 생성 확인")
    void createMailMessage() throws MessagingException {
        // Given
        String mailHost = "admin@gmail.com";
        String recipient = "dev.youngjinmo@gmail.com";
        String subject = "[TEST] Sending mail";
        String messageBody = "This is test";
        MimeMessage mockMessage = mock(MimeMessage.class);

        // When
        when(javaMailSender.createMimeMessage()).thenReturn(mockMessage);
        when(mailConfig.getUsername()).thenReturn(mailHost);

        MailMessageDto messageDto = new MailMessageDto(recipient, subject, messageBody);
        MimeMessage result = mailService.createMailMessage(messageDto);

        // Then
        verify(result).setFrom(mailHost);
        verify(result).setRecipients(MimeMessage.RecipientType.TO, recipient);
        verify(result).setSubject(subject);
        verify(result).setText(messageBody, "UTF-8", "html");
        assertEquals(mockMessage, result);
    }
}