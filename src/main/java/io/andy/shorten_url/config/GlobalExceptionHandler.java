package io.andy.shorten_url.config;

import io.andy.shorten_url.exception.CustomException;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;

import java.util.HashMap;
import java.util.Map;

@ControllerAdvice
public class GlobalExceptionHandler {
    @ExceptionHandler(CustomException.class)
    public ResponseEntity<Object> handleCustomException(CustomException exception) {
        Map<String, Object> responseBody = new HashMap<>();
        responseBody.put("error", exception.getStatus().getReasonPhrase());
        responseBody.put("status", exception.getStatus());
        responseBody.put("message", exception.getMessage());
        return new ResponseEntity<>(responseBody, exception.getStatus());
    }
}
