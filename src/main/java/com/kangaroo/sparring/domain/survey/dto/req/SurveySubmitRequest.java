package com.kangaroo.sparring.domain.survey.dto.req;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "설문 응답 제출 요청")
public class SurveySubmitRequest {

    @Schema(description = "질문-응답 목록")
    @NotEmpty(message = "답변은 최소 1개 이상이어야 합니다.")
    @Valid
    private List<AnswerItem> answers;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "질문-응답 항목")
    public static class AnswerItem {

        @Schema(
                description = "질문 키 (예: HEIGHT, MEAL_FREQUENCY 등 등록된 questionKey)",
                example = "HEIGHT"
        )
        @NotBlank(message = "질문 키는 필수입니다.")
        private String questionKey;

        @Schema(
                description = "답변 값 (TEXT: 문자열, NUMBER: 숫자 또는 숫자 문자열, SINGLE_CHOICE: code 문자열, MULTIPLE_CHOICE: code 문자열 배열)",
                example = "180"
        )
        @NotNull(message = "답변 값은 필수입니다.")
        private JsonNode value;
    }
}
