package com.kangaroo.sparring.domain.survey.dto.res;

import com.kangaroo.sparring.domain.survey.entity.Answer;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "설문 응답 항목")
public class AnswerResponse {

    @Schema(description = "응답 ID")
    private Long answerId;
    @Schema(description = "질문 ID")
    private Long questionId;
    @Schema(
            description = "질문 키 (규칙: {SURVEY_TYPE}_{FIELD})",
            example = "BASIC_HEIGHT"
    )
    private String questionKey;
    @Schema(description = "질문 내용")
    private String questionText;
    @Schema(description = "응답 값")
    private String answerText;

    public static AnswerResponse from(Answer answer) {
        return AnswerResponse.builder()
                .answerId(answer.getId())
                .questionId(answer.getQuestion().getId())
                .questionKey(answer.getQuestion().getQuestionKey())
                .questionText(answer.getQuestion().getQuestionText())
                .answerText(answer.getAnswerText())
                .build();
    }
}
