package com.example.lablink.global.exception;

import lombok.Getter;

@Getter
public class GlobalException extends RuntimeException {
    private final GlobalErrorCode errorCode;

    public GlobalException(GlobalErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public GlobalException(GlobalErrorCode errorCode, Throwable cause) {
        super(errorCode.getMessage(), cause);
        this.errorCode = errorCode;
    }
}
