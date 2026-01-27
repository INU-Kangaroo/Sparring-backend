package com.kangaroo.sparring.global.exception;

import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;

@Getter
@RequiredArgsConstructor
public enum ErrorCode {

    // User
    USER_NOT_FOUND(HttpStatus.NOT_FOUND, "U001", "사용자를 찾을 수 없습니다"),
    DUPLICATE_EMAIL(HttpStatus.CONFLICT, "U002", "이미 사용중인 이메일입니다"),
    INVALID_PASSWORD(HttpStatus.UNAUTHORIZED, "U003", "비밀번호가 일치하지 않습니다"),
    INACTIVE_USER(HttpStatus.FORBIDDEN, "U004", "비활성화된 사용자입니다"),

    // JWT & Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A003", "인증이 필요합니다"),

    // Email Verification
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "E001", "이메일 인증이 필요합니다"),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "E002", "유효하지 않은 인증코드입니다"),
    EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "E003", "만료된 인증코드입니다"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "E004", "잠시 후 다시 시도해주세요."),

    // Survey
    SURVEY_NOT_FOUND(HttpStatus.NOT_FOUND, "S001", "설문을 찾을 수 없습니다"),
    QUESTION_NOT_FOUND(HttpStatus.NOT_FOUND, "S002", "질문을 찾을 수 없습니다"),
    ANSWER_NOT_FOUND(HttpStatus.NOT_FOUND, "S003", "응답을 찾을 수 없습니다"),
    SURVEY_ALREADY_COMPLETED(HttpStatus.CONFLICT, "S004", "이미 완료한 설문입니다"),
    SURVEY_NOT_COMPLETED(HttpStatus.BAD_REQUEST, "S005", "아직 설문을 완료하지 않았습니다"),
    HEALTH_PROFILE_NOT_FOUND(HttpStatus.NOT_FOUND, "S006", "건강 프로필을 찾을 수 없습니다"),
    SURVEY_INCOMPLETE(HttpStatus.BAD_REQUEST, "S007", "모든 질문에 답변해야 합니다"),

    // Measurement
    BLOOD_SUGAR_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "M001", "혈당 측정 기록을 찾을 수 없습니다"),
    BLOOD_PRESSURE_LOG_NOT_FOUND(HttpStatus.NOT_FOUND, "M002", "혈압 측정 기록을 찾을 수 없습니다"),
    INVALID_MEASUREMENT_TYPE(HttpStatus.BAD_REQUEST, "M003", "유효하지 않은 측정 타입입니다"),
    INVALID_DATE_RANGE(HttpStatus.BAD_REQUEST, "M004", "유효하지 않은 날짜 범위입니다"),
    AI_PREDICTION_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "M005", "AI 예측에 실패했습니다"),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
