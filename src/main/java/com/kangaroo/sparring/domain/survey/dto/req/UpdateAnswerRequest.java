package com.kangaroo.sparring.domain.survey.dto.req;

import com.fasterxml.jackson.databind.JsonNode;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "설문 응답 수정 요청")
public class UpdateAnswerRequest {

    @Schema(
            description = "질문 키 (예: HEIGHT, MEAL_FREQUENCY 등)",
            example = "EXERCISE_PLACE"
    )
    @NotBlank(message = "질문 키는 필수입니다.")
    private String questionKey;

    @Schema(
            description = "답변 값 (TEXT: 문자열, NUMBER: 숫자 또는 숫자 문자열, SINGLE_CHOICE: code 문자열, MULTIPLE_CHOICE: code 문자열 배열)",
            example = "[\"GYM_FACILITY\",\"OUTDOOR\"]"
    )
    @NotNull(message = "답변 값은 필수입니다.")
    private JsonNode value;
}
