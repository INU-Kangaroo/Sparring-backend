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

    @Schema(
            description = "질문 키 (예: HEIGHT, MEAL_FREQUENCY 등)",
            example = "HEIGHT"
    )
    private String questionKey;
    @Schema(description = "응답 값")
    private String value;

    public static AnswerResponse from(Answer answer) {
        return AnswerResponse.builder()
                .questionKey(answer.getQuestion().getQuestionKey())
                .value(answer.getAnswerText())
                .build();
    }
}
