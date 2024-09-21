package io.andy.shorten_url.exception;

import org.springframework.http.HttpStatus;

import java.util.Objects;

public class ErrorResponse {
    private final HttpStatus httpStatus;
    private final String message;

    private ErrorResponse(HttpStatus httpStatus, String message) {
        this.httpStatus = httpStatus;
        this.message = message;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "ErrorResponse [httpStatus=" + httpStatus + ", message=" + message + "]";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        ErrorResponse that = (ErrorResponse) o;
        return this.httpStatus == that.httpStatus &&
                this.message.equals(that.message);
    }

    public static class Builder {
        private HttpStatus httpStatus;
        private String message;

        public static Builder builder() {
            return new Builder();
        }

        public Builder httpStatus(HttpStatus httpStatus) {
            this.httpStatus = Objects.requireNonNull(httpStatus, "httpStatus must not be null");
            return this;
        }

        public Builder message(String message) {
            this.message = Objects.requireNonNull(message, "message must not be null");
            return this;
        }

        public ErrorResponse build() {
            return new ErrorResponse(httpStatus, message);
        }
    }
}
