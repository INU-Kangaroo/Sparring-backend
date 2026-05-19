package com.kangaroo.sparring.domain.prediction.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "혈당 예측 요청")
public class GlucosePredictionRequest {

    @Valid
    @NotNull(message = "meal 값은 필수입니다")
    private Meal meal;

    @Getter
    @NoArgsConstructor
    @Schema(description = "식사 영양 정보")
    public static class Meal {

        @Schema(description = "탄수화물(g)", example = "42", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "탄수화물 값은 필수입니다")
        @PositiveOrZero(message = "탄수화물 값은 0 이상이어야 합니다")
        private Double carbs;

        @Schema(description = "단백질(g)", example = "24", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "단백질 값은 필수입니다")
        @PositiveOrZero(message = "단백질 값은 0 이상이어야 합니다")
        private Double protein;

        @Schema(description = "지방(g)", example = "10", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "지방 값은 필수입니다")
        @PositiveOrZero(message = "지방 값은 0 이상이어야 합니다")
        private Double fat;

        @Schema(description = "식이섬유(g)", example = "6", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "식이섬유 값은 필수입니다")
        @PositiveOrZero(message = "식이섬유 값은 0 이상이어야 합니다")
        private Double fiber;

        @Schema(description = "총 열량(kcal)", example = "410", requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "열량 값은 필수입니다")
        @PositiveOrZero(message = "열량 값은 0 이상이어야 합니다")
        private Double kcal;

        @Schema(description = "식사 유형", example = "lunch", allowableValues = {"breakfast", "lunch", "dinner"}, requiredMode = Schema.RequiredMode.REQUIRED)
        @NotNull(message = "식사 유형은 필수입니다")
        @Pattern(regexp = "breakfast|lunch|dinner", message = "식사 유형은 breakfast/lunch/dinner 중 하나여야 합니다")
        private String mealType;
    }
}
