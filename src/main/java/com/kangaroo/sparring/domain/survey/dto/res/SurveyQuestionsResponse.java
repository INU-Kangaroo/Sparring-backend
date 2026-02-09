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
public class SurveyQuestionsResponse {

    private List<QuestionResponse> questions;

    public static SurveyQuestionsResponse from(List<QuestionResponse> questions) {
        return SurveyQuestionsResponse.builder()
                .questions(questions)
                .build();
    }
}
