package com.kangaroo.sparring.global.exception;

import lombok.Getter;

@Getter
public class CustomException extends RuntimeException {
    
    private final ErrorCode errorCode;

    public CustomException(ErrorCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public CustomException(ErrorCode errorCode, String detailMessage) {
        super(detailMessage == null || detailMessage.isBlank()
                ? errorCode.getMessage()
                : errorCode.getMessage() + " - " + detailMessage);
        this.errorCode = errorCode;
    }
}
