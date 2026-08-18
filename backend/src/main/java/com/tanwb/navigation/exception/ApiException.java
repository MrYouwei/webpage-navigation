package com.tanwb.navigation.exception;

import org.springframework.http.HttpStatus;

public class ApiException extends RuntimeException {
    private final HttpStatus status;
    private final int code;

    public ApiException(HttpStatus status, String message) {
        super(message);
        this.status = status;
        this.code = status.value();
    }

    public HttpStatus getStatus() {
        return status;
    }

    public int getCode() {
        return code;
    }
}
