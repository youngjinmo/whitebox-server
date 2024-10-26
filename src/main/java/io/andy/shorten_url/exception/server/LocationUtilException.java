package io.andy.shorten_url.exception.server;

import io.andy.shorten_url.exception.CustomException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
public class LocationUtilException extends CustomException  {
    public LocationUtilException() {
        super(HttpStatus.INTERNAL_SERVER_ERROR, "FAILED TO PARSE LOCATION INFO BY IP");
    }

    public LocationUtilException(String message) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, message);
    }
}
