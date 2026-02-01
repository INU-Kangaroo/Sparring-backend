package com.kangaroo.sparring.domain.survey.dto.req;

import jakarta.validation.constraints.NotBlank;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "설문 응답 수정 요청")
public class UpdateAnswerRequest {

    @Schema(
            description = "질문 키 (규칙: {SURVEY_TYPE}_{FIELD})",
            example = "BASIC_HEIGHT"
    )
    @NotBlank(message = "질문 키는 필수입니다.")
    private String questionKey;

    @Schema(description = "답변 값 (문자열로 제출)", example = "175")
    @NotBlank(message = "답변 값은 필수입니다.")
    private String answerText;
}
