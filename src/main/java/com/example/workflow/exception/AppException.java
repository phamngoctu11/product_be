package com.example.workflow.exception;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class AppException extends RuntimeException {
    private final HttpStatus status;

    public AppException(HttpStatus status, ConstantErrorCode errorCode, Object... args) {
        super(errorCode.format(args));
        this.status = status;
    }
}
