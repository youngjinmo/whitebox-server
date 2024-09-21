package io.andy.shorten_url.config;

import io.andy.shorten_url.exception.CustomException;
import io.andy.shorten_url.exception.ErrorResponse;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException exception) {
        ErrorResponse response = new ErrorResponse.Builder()
                .httpStatus(exception.getStatus())
                .message(exception.getMessage())
                .build();
        return new ResponseEntity<>(response, exception.getStatus());
    }
}
