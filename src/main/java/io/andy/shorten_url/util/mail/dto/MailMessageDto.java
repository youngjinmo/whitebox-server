package io.andy.shorten_url.util.mail.dto;

public record MailMessageDto (
        String recipient,
        String subject,
        String messageBody
) {}
