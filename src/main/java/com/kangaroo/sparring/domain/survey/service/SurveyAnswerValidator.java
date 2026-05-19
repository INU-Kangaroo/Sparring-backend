package com.kangaroo.sparring.domain.survey.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.healthprofile.HealthProfileFieldSupport;
import com.kangaroo.sparring.domain.survey.dto.req.SurveySubmitRequest;
import com.kangaroo.sparring.domain.survey.dto.res.OptionItem;
import com.kangaroo.sparring.domain.survey.entity.Question;
import com.kangaroo.sparring.domain.survey.entity.QuestionType;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Component
public class SurveyAnswerValidator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    public void validateHealthProfileFieldMappings(List<Question> questions) {
        List<String> invalidFields = new ArrayList<>();
        for (Question question : questions) {
            String fieldName = question.getHealthProfileField();
            if (fieldName == null || fieldName.isBlank()) {
                continue;
            }
            if (!HealthProfileFieldSupport.isSupportedField(fieldName)) {
                invalidFields.add(question.getQuestionKey() + ":" + fieldName);
            }
        }
        if (!invalidFields.isEmpty()) {
            log.error("Invalid healthProfileField mappings: {}", String.join(", ", invalidFields));
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    public void validateSubmittedAnswers(
            List<SurveySubmitRequest.AnswerItem> submittedAnswers,
            Map<String, Question> questionMap
    ) {
        Set<String> submittedQuestionKeys = new HashSet<>();
        for (SurveySubmitRequest.AnswerItem answerItem : submittedAnswers) {
            if (!submittedQuestionKeys.add(answerItem.getQuestionKey())) {
                throw new CustomException(ErrorCode.INVALID_INPUT, "중복된 questionKey: " + answerItem.getQuestionKey());
            }
        }

        Set<String> allowedQuestionKeys = questionMap.keySet();
        Set<String> unknownQuestionKeys = submittedQuestionKeys.stream()
                .filter(key -> !allowedQuestionKeys.contains(key))
                .collect(Collectors.toSet());
        if (!unknownQuestionKeys.isEmpty()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "유효하지 않은 questionKey: " + String.join(", ", unknownQuestionKeys)
            );
        }

        Set<String> requiredQuestionKeys = questionMap.values().stream()
                .filter(question -> Boolean.TRUE.equals(question.getIsRequired()))
                .map(Question::getQuestionKey)
                .collect(Collectors.toSet());
        Set<String> missingRequiredKeys = requiredQuestionKeys.stream()
                .filter(key -> !submittedQuestionKeys.contains(key))
                .collect(Collectors.toSet());
        if (!missingRequiredKeys.isEmpty()) {
            throw new CustomException(
                    ErrorCode.SURVEY_INCOMPLETE,
                    "필수 문항 누락: " + String.join(", ", missingRequiredKeys)
            );
        }

        for (SurveySubmitRequest.AnswerItem answerItem : submittedAnswers) {
            Question question = questionMap.get(answerItem.getQuestionKey());
            toStoredAnswer(question, answerItem.getValue());
        }
    }

    public String toStoredAnswer(Question question, JsonNode value) {
        if (value == null || value.isNull()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "답변 값은 null일 수 없습니다.");
        }

        QuestionType questionType = question.getQuestionType();
        OptionLookup optionLookup = parseOptionLookup(question);
        return switch (questionType) {
            case TEXT -> toScalarString(value, question.getQuestionKey(), questionType);
            case SINGLE_CHOICE -> toSingleChoiceCode(value, question.getQuestionKey(), optionLookup);
            case NUMBER -> toNumberLikeString(value, question.getQuestionKey());
            case MULTIPLE_CHOICE -> toJsonStringArrayOfCodes(value, question.getQuestionKey(), optionLookup);
        };
    }

    private OptionLookup parseOptionLookup(Question question) {
        String options = question.getOptions();
        if (options == null || options.isBlank()) {
            return OptionLookup.empty();
        }
        try {
            List<OptionItem> optionItems = OBJECT_MAPPER.readValue(options, new TypeReference<List<OptionItem>>() {});
            Set<String> allowedCodes = new LinkedHashSet<>();
            Map<String, String> labelToCode = new HashMap<>();
            for (OptionItem item : optionItems) {
                if (item.getCode() == null || item.getCode().isBlank()) {
                    continue;
                }
                String code = item.getCode().trim();
                allowedCodes.add(code);

                if (item.getLabel() != null && !item.getLabel().isBlank()) {
                    labelToCode.put(item.getLabel().trim(), code);
                }
            }
            return new OptionLookup(allowedCodes, labelToCode);
        } catch (IOException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "문항(" + question.getQuestionKey() + ") 옵션 포맷이 올바르지 않습니다.");
        }
    }

    private String toScalarString(JsonNode value, String questionKey, QuestionType questionType) {
        if (value.isArray() || value.isObject()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "문항(" + questionKey + ")의 " + questionType + " 답변은 단일 값이어야 합니다."
            );
        }
        String text = value.asText().trim();
        if (text.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "문항(" + questionKey + ") 답변 값이 비어있습니다.");
        }
        return text;
    }

    private String toSingleChoiceCode(JsonNode value, String questionKey, OptionLookup optionLookup) {
        String raw = toScalarString(value, questionKey, QuestionType.SINGLE_CHOICE);
        return normalizeOptionValue(raw, questionKey, "SINGLE_CHOICE", optionLookup);
    }

    private String toNumberLikeString(JsonNode value, String questionKey) {
        if (!(value.isNumber() || value.isTextual())) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "문항(" + questionKey + ")의 NUMBER 답변은 숫자 또는 숫자 문자열이어야 합니다."
            );
        }
        String text = value.asText().trim();
        if (text.isBlank()) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "문항(" + questionKey + ") 답변 값이 비어있습니다.");
        }
        return text;
    }

    private String toJsonStringArrayOfCodes(JsonNode value, String questionKey, OptionLookup optionLookup) {
        if (!value.isArray()) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "문항(" + questionKey + ")의 MULTIPLE_CHOICE 값은 code 문자열 배열이어야 합니다."
            );
        }

        List<String> codes = new ArrayList<>();
        for (JsonNode node : value) {
            if (!node.isTextual()) {
                throw new CustomException(
                        ErrorCode.INVALID_INPUT,
                        "문항(" + questionKey + ")의 MULTIPLE_CHOICE 항목은 문자열이어야 합니다."
                );
            }
            String raw = node.asText().trim();
            if (raw.isBlank()) {
                throw new CustomException(
                        ErrorCode.INVALID_INPUT,
                        "문항(" + questionKey + ")의 MULTIPLE_CHOICE 항목은 비어있을 수 없습니다."
                );
            }
            codes.add(normalizeOptionValue(raw, questionKey, "MULTIPLE_CHOICE", optionLookup));
        }
        try {
            return OBJECT_MAPPER.writeValueAsString(codes);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "문항(" + questionKey + ") 배열 직렬화에 실패했습니다.");
        }
    }

    private String normalizeOptionValue(
            String raw,
            String questionKey,
            String questionType,
            OptionLookup optionLookup
    ) {
        if (optionLookup.allowedCodes().isEmpty()) {
            return raw;
        }

        String matchedCode = optionLookup.findCode(raw);
        if (matchedCode != null) {
            return matchedCode;
        }

        throw new CustomException(
                ErrorCode.INVALID_INPUT,
                "문항(" + questionKey + ")의 " + questionType + " 값이 옵션에 없습니다: " + raw
        );
    }

    private record OptionLookup(Set<String> allowedCodes, Map<String, String> labelToCode) {
        static OptionLookup empty() {
            return new OptionLookup(Set.of(), Map.of());
        }

        String findCode(String raw) {
            if (raw == null) {
                return null;
            }
            String trimmed = raw.trim();
            if (trimmed.isBlank()) {
                return null;
            }

            if (allowedCodes.contains(trimmed)) {
                return trimmed;
            }

            String normalizedUpper = trimmed.toUpperCase(Locale.ROOT).replace(' ', '_');
            if (allowedCodes.contains(normalizedUpper)) {
                return normalizedUpper;
            }

            return labelToCode.get(trimmed);
        }
    }
}
