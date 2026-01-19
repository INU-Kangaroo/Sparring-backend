package com.kangaroo.sparring.domain.survey.dto.res;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.survey.entity.Question;
import com.kangaroo.sparring.domain.survey.entity.QuestionType;
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

    @Schema(description = "질문 ID")
    private Long id;
    @Schema(
            description = "질문 키 (규칙: {SURVEY_TYPE}_{FIELD})",
            example = "BASIC_HEIGHT"
    )
    private String questionKey;
    @Schema(description = "질문 타입", example = "NUMBER")
    private QuestionType questionType;
    @Schema(description = "질문 내용")
    private String questionText;
    @Schema(description = "질문 순서", example = "1")
    private Integer questionOrder;
    @Schema(description = "필수 여부", example = "true")
    private Boolean isRequired;
    @Schema(description = "선택지")
    private List<String> options;

    public static QuestionResponse from(Question question) {
        return QuestionResponse.builder()
                .id(question.getId())
                .questionKey(question.getQuestionKey())
                .questionType(question.getQuestionType())
                .questionText(question.getQuestionText())
                .questionOrder(question.getQuestionOrder())
                .isRequired(question.getIsRequired())
                .options(parseOptions(question.getOptions()))
                .build();
    }

    private static List<String> parseOptions(String options) {
        if (options == null || options.isBlank()) {
            return null;
        }
        try {
            return OBJECT_MAPPER.readValue(options, new TypeReference<List<String>>() {});
        } catch (IOException e) {
            throw new IllegalArgumentException("Invalid options JSON", e);
        }
    }
}
