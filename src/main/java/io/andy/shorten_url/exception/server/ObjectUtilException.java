package io.andy.shorten_url.exception.server;

import io.andy.shorten_url.exception.CustomException;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class ObjectUtilException extends CustomException  {
    public ObjectUtilException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "FAILED TO OBJECT UTIL EXECUTION");
    }

    public ObjectUtilException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
