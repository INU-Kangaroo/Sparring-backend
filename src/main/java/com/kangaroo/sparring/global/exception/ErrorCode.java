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
    PASSWORD_CHANGE_NOT_ALLOWED(HttpStatus.BAD_REQUEST, "U005", "소셜 로그인 계정은 비밀번호 변경이 불가능합니다"),
    INVALID_PASSWORD_CONFIRM(HttpStatus.BAD_REQUEST, "U006", "새 비밀번호 확인이 일치하지 않습니다"),

    // JWT & Auth
    INVALID_TOKEN(HttpStatus.UNAUTHORIZED, "A001", "유효하지 않은 토큰입니다"),
    EXPIRED_TOKEN(HttpStatus.UNAUTHORIZED, "A002", "만료된 토큰입니다"),
    UNAUTHORIZED(HttpStatus.UNAUTHORIZED, "A003", "인증이 필요합니다"),
    OAUTH2_EMAIL_REQUIRED(HttpStatus.BAD_REQUEST, "A004", "소셜 로그인에는 이메일 동의가 필요합니다"),
    OAUTH2_PROVIDER_MISMATCH(HttpStatus.CONFLICT, "A005", "다른 소셜 계정으로 가입된 이메일입니다"),
    OAUTH2_MISSING_REDIRECT_URI(HttpStatus.BAD_REQUEST, "A006", "redirectUri가 필요합니다"),
    OAUTH2_MISSING_CODE_VERIFIER(HttpStatus.BAD_REQUEST, "A007", "codeVerifier가 필요합니다"),

    // Email Verification
    EMAIL_NOT_VERIFIED(HttpStatus.FORBIDDEN, "E001", "이메일 인증이 필요합니다"),
    INVALID_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "E002", "유효하지 않은 인증코드입니다"),
    EXPIRED_VERIFICATION_CODE(HttpStatus.BAD_REQUEST, "E003", "만료된 인증코드입니다"),
    VERIFICATION_NOT_FOUND(HttpStatus.BAD_REQUEST, "E004", "인증 요청이 존재하지 않습니다"),
    TOO_MANY_REQUESTS(HttpStatus.TOO_MANY_REQUESTS, "E005", "잠시 후 다시 시도해주세요."),

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
    INVALID_BLOOD_PRESSURE_RANGE(HttpStatus.BAD_REQUEST, "M006", "수축기 혈압은 이완기 혈압보다 높아야 합니다"),
    INVALID_SYSTOLIC_PRESSURE(HttpStatus.BAD_REQUEST, "M007", "수축기 혈압은 50~300 mmHg 범위여야 합니다"),
    INVALID_DIASTOLIC_PRESSURE(HttpStatus.BAD_REQUEST, "M008", "이완기 혈압은 30~200 mmHg 범위여야 합니다"),
    INVALID_HEART_RATE(HttpStatus.BAD_REQUEST, "M009", "심박수는 30~250 bpm 범위여야 합니다"),
    INVALID_GLUCOSE_LEVEL(HttpStatus.BAD_REQUEST, "M010", "혈당 수치는 20~600 mg/dL 범위여야 합니다"),
    MEASUREMENT_TIME_FUTURE(HttpStatus.BAD_REQUEST, "M011", "측정 시간은 미래일 수 없습니다"),
    INSUFFICIENT_DATA_FOR_PREDICTION(HttpStatus.BAD_REQUEST, "M012", "예측을 위한 충분한 데이터가 없습니다"),

    // Food
    FOOD_NOT_FOUND(HttpStatus.NOT_FOUND, "F001", "음식을 찾을 수 없습니다"),
    DUPLICATE_FOOD(HttpStatus.CONFLICT, "F002", "이미 등록된 음식입니다"),
    EXTERNAL_API_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "F003", "외부 API 호출에 실패했습니다"),

    // Recommendation
    RECOMMENDATION_AI_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "R003", "추천 AI 호출에 실패했습니다"),
    RECOMMENDATION_AI_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "R005", "추천 요청이 많습니다. 잠시 후 다시 시도해주세요."),

    // Chatbot
    CHATBOT_SESSION_NOT_FOUND(HttpStatus.NOT_FOUND, "CB001", "채팅 세션을 찾을 수 없습니다"),
    CHATBOT_AI_CALL_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CB002", "AI 응답 생성에 실패했습니다"),
    CHATBOT_AI_RATE_LIMIT(HttpStatus.TOO_MANY_REQUESTS, "CB003", "AI 요청 한도를 초과했습니다. 잠시 후 다시 시도해주세요"),
    CHATBOT_SESSION_SERIALIZE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CB004", "세션 저장 중 오류가 발생했습니다"),
    CHATBOT_SESSION_DESERIALIZE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "CB005", "세션 불러오기 중 오류가 발생했습니다"),

    // Home
    INSIGHT_GENERATE_FAILED(HttpStatus.INTERNAL_SERVER_ERROR, "H001", "인사이트 생성에 실패했습니다"),

    // Common
    INVALID_INPUT(HttpStatus.BAD_REQUEST, "C001", "잘못된 입력값입니다"),
    INTERNAL_SERVER_ERROR(HttpStatus.INTERNAL_SERVER_ERROR, "C002", "서버 오류가 발생했습니다");

    private final HttpStatus status;
    private final String code;
    private final String message;
}
