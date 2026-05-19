package com.kangaroo.sparring.domain.recommendation.dto.req;

import com.kangaroo.sparring.domain.common.type.ExerciseDuration;
import com.kangaroo.sparring.domain.common.type.ExerciseIntensity;
import com.kangaroo.sparring.domain.common.type.ExerciseLocation;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

@Getter
@Schema(description = "운동 추천 요청")
public class ExerciseRecommendationRequest {

    @NotNull(message = "운동 시간은 필수입니다")
    @Schema(description = "운동 시간", example = "MEDIUM")
    private ExerciseDuration duration;

    @NotNull(message = "운동 강도는 필수입니다")
    @Schema(description = "운동 강도", example = "MODERATE")
    private ExerciseIntensity intensity;

    @NotNull(message = "운동 장소는 필수입니다")
    @Schema(description = "운동 장소", example = "INDOOR")
    private ExerciseLocation location;
}
