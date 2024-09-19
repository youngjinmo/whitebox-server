package io.andy.shorten_url.exception.client;

import io.andy.shorten_url.exception.CustomException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.NOT_FOUND)
public class NotFoundException extends CustomException  {
    public NotFoundException() {
        super(HttpStatus.NOT_FOUND, "NOT FOUND");
    }
    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
