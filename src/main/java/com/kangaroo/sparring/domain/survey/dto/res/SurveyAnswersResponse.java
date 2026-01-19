package com.kangaroo.sparring.domain.survey.dto.res;

import com.kangaroo.sparring.domain.survey.entity.SurveyType;
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

    private SurveyType surveyType;
    private List<AnswerResponse> answers;

    public static SurveyAnswersResponse of(SurveyType surveyType, List<AnswerResponse> answers) {
        return SurveyAnswersResponse.builder()
                .surveyType(surveyType)
                .answers(answers)
                .build();
    }
}