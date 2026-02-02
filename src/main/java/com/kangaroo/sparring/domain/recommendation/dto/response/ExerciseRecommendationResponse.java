package com.kangaroo.sparring.domain.recommendation.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "운동 추천 응답")
public class ExerciseRecommendationResponse {

    @Schema(description = "유산소 운동 목록")
    private List<CardiacExerciseDto> cardiacExercises;

    @Schema(description = "근력 운동 목록")
    private List<StrengthExerciseDto> strengthExercises;

    public static ExerciseRecommendationResponse of(List<CardiacExerciseDto> cardiacExercises,
                                                    List<StrengthExerciseDto> strengthExercises) {
        return ExerciseRecommendationResponse.builder()
                .cardiacExercises(cardiacExercises)
                .strengthExercises(strengthExercises)
                .build();
    }
}
