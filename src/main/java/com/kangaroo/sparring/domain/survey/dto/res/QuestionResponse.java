package com.kangaroo.sparring.domain.survey.dto.res;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.survey.entity.Question;
import com.kangaroo.sparring.domain.survey.entity.QuestionType;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.io.IOException;
import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "설문 질문 응답")
public class QuestionResponse {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Schema(
            description = "질문 키 (예: HEIGHT, MEAL_FREQUENCY 등)",
            example = "HEIGHT"
    )
    private String questionKey;
    @Schema(description = "질문 타입", example = "NUMBER")
    private QuestionType questionType;
    @Schema(description = "질문 내용")
    private String questionText;
    @Schema(description = "필수 여부", example = "true")
    private Boolean isRequired;
    @Schema(description = "선택지")
    private List<OptionItem> options;

    public static QuestionResponse from(Question question) {
        return QuestionResponse.builder()
                .questionKey(question.getQuestionKey())
                .questionType(question.getQuestionType())
                .questionText(question.getQuestionText())
                .isRequired(question.getIsRequired())
                .options(parseOptions(question.getOptions(), question.getQuestionKey()))
                .build();
    }

    private static List<OptionItem> parseOptions(String options, String questionKey) {
        if (options == null || options.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(options, new TypeReference<List<OptionItem>>() {});
        } catch (IOException e) {
            throw new CustomException(
                    ErrorCode.INVALID_INPUT,
                    "질문 선택지 포맷 오류(questionKey=" + questionKey + "). [{code,label}] JSON 배열이어야 합니다."
            );
        }
    }
}
