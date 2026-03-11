package com.kangaroo.sparring.domain.survey.dto.res;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyAnswersResponse {

    private List<AnswerResponse> answers;

    public static SurveyAnswersResponse of(List<AnswerResponse> answers) {
        return SurveyAnswersResponse.builder()
                .answers(answers)
                .build();
    }
}
