package io.andy.shorten_url.exception.client;

import io.andy.shorten_url.exception.CustomException;

import org.springframework.http.HttpStatus;

public class ForbiddenException extends CustomException {
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
