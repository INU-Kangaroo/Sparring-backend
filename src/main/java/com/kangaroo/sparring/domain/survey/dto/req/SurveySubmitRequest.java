package com.kangaroo.sparring.domain.survey.dto.req;

import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.NotBlank;
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

    @Schema(description = "설문 타입", example = "BASIC")
    @NotNull(message = "설문 타입은 필수입니다.")
    private SurveyType surveyType;

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
                description = "질문 키 (규칙: {SURVEY_TYPE}_{FIELD})",
                example = "BASIC_HEIGHT"
        )
        @NotBlank(message = "질문 키는 필수입니다.")
        private String questionKey;

        @Schema(description = "답변 값 (문자열로 제출)", example = "180")
        @NotBlank(message = "답변 값은 필수입니다.")
        private String answerText;
    }
}
