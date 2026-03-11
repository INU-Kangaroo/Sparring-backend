package com.kangaroo.sparring.domain.survey.dto.res;

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

    @Schema(description = "완료 여부", example = "true")
    private Boolean isCompleted;

    public static SurveySubmitResponse of() {
        return SurveySubmitResponse.builder()
                .isCompleted(true)
                .build();
    }
}
