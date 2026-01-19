package com.kangaroo.sparring.domain.survey.dto.res;

import com.kangaroo.sparring.domain.survey.entity.SurveyType;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Schema(description = "설문 응답 제출 결과")
public class SurveySubmitResponse {

    @Schema(description = "설문 타입", example = "BASIC")
    private SurveyType surveyType;
    @Schema(description = "완료 여부", example = "true")
    private Boolean isCompleted;

    public static SurveySubmitResponse of(SurveyType surveyType) {
        return SurveySubmitResponse.builder()
                .surveyType(surveyType)
                .isCompleted(true)
                .build();
    }
}
