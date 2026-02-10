package com.kangaroo.sparring.global.exception;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(CustomException.class)
    public ResponseEntity<ErrorResponse> handleCustomException(CustomException e) {
        log.error("CustomException: {}", e.getMessage());
        ErrorCode errorCode = e.getErrorCode();
        return ResponseEntity
                .status(errorCode.getStatus())
                .body(new ErrorResponse(errorCode.getCode(), e.getMessage(), null));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException e) {
        Map<String, String> errors = new HashMap<>();
        e.getBindingResult().getAllErrors().forEach(error -> {
            String errorMessage = error.getDefaultMessage();
            if (error instanceof FieldError fieldError) {
                errors.put(fieldError.getField(), errorMessage);
            } else {
                errors.put(error.getObjectName(), errorMessage);
            }
        });

        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("VALIDATION_ERROR", "입력값이 올바르지 않습니다.", errors));
    }

    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ErrorResponse> handleMissingRequestHeaderException(MissingRequestHeaderException e) {
        log.warn("Missing request header: {}", e.getHeaderName());
        if ("X-Refresh-Token".equalsIgnoreCase(e.getHeaderName())) {
            return ResponseEntity
                    .badRequest()
                    .body(new ErrorResponse("MISSING_HEADER",
                            "리프레시 토큰 헤더가 누락되었습니다: X-Refresh-Token",
                            null));
        }
        return ResponseEntity
                .badRequest()
                .body(new ErrorResponse("MISSING_HEADER",
                        String.format("필수 헤더가 누락되었습니다: %s", e.getHeaderName()),
                        null));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleException(Exception e) {
        log.error("Exception: ", e);
        return ResponseEntity
                .internalServerError()
                .body(new ErrorResponse("INTERNAL_ERROR", "서버 오류가 발생했습니다", null));
    }

    record ErrorResponse(String code, String message, Map<String, String> errors) {}
}
