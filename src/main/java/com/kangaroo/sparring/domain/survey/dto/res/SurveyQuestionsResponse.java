package com.kangaroo.sparring.domain.survey.dto.res;

import com.kangaroo.sparring.domain.survey.entity.Survey;
import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.stream.Collectors;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SurveyQuestionsResponse {

    private Long surveyId;
    private SurveyType surveyType;
    private String title;
    private String description;
    private List<QuestionResponse> questions;

    public static SurveyQuestionsResponse from(Survey survey) {
        return SurveyQuestionsResponse.builder()
                .surveyId(survey.getId())
                .surveyType(survey.getSurveyType())
                .title(survey.getTitle())
                .description(survey.getDescription())
                .questions(survey.getQuestions().stream()
                        .map(QuestionResponse::from)
                        .collect(Collectors.toList()))
                .build();
    }
}