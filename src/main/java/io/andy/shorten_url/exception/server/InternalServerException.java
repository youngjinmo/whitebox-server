package io.andy.shorten_url.exception.server;

import io.andy.shorten_url.exception.CustomException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class InternalServerException extends CustomException {
    public InternalServerException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "INTERNAL SERVER ERROR");
    }

    public InternalServerException(String message) {
       super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
