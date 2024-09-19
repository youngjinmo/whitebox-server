package io.andy.shorten_url.exception;

import io.andy.shorten_url.exception.client.BadRequestException;
import io.andy.shorten_url.exception.client.UnauthorizedException;
import io.andy.shorten_url.exception.server.InternalServerException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import static org.junit.jupiter.api.Assertions.*;

class CustomExceptionTest {

    @Test
    @DisplayName("UnauthorizedException")
    void throwUnauthorizedException() {
        String message = "인증 오류";
        boolean result = getAssertErrorMessage(new UnauthorizedException(message), HttpStatus.UNAUTHORIZED, message);
        assertTrue(result);
    }

    @Test
    @DisplayName("BadRequestException")
    void throwBadRequestException() {
        String message = "요청 오류";
        boolean result = getAssertErrorMessage(new BadRequestException(message), HttpStatus.BAD_REQUEST, message);
        assertTrue(result);
    }

    @Test
    @DisplayName("InternalServerException")
    void throwInternalServerException() {
        String message = "서버 오류";
        boolean result = getAssertErrorMessage(new InternalServerException(message), HttpStatus.INTERNAL_SERVER_ERROR, message);
        assertTrue(result);
    }

    private boolean getAssertErrorMessage(CustomException exception, HttpStatus status, String message) {
        return exception.getStatus().equals(status) && exception.getMessage().equals(message);
    }

}