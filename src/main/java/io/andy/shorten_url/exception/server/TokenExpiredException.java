package io.andy.shorten_url.exception.server;

import io.andy.shorten_url.exception.CustomException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.UNAUTHORIZED)
public class TokenExpiredException extends CustomException {
    public TokenExpiredException() {
        super(HttpStatus.UNAUTHORIZED, "EXPIRED TOKEN");
    }

    public TokenExpiredException(String message) {
       super(HttpStatus.UNAUTHORIZED, message);
    }
}
