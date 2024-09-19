package io.andy.shorten_url.exception.client;

import io.andy.shorten_url.exception.CustomException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.FORBIDDEN)
public class ForbiddenException extends CustomException {
    public ForbiddenException() {
        super(HttpStatus.FORBIDDEN, "FORBIDDEN");
    }
    public ForbiddenException(String message) {
        super(HttpStatus.FORBIDDEN, message);
    }
}
