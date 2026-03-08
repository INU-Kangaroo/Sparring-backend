package com.kangaroo.sparring.domain.meal.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.kangaroo.sparring.domain.meal.type.MealTime;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "식사 기록 등록 요청")
public class MealLogCreateRequest {

    @Schema(description = "음식 ID", example = "100", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "음식 ID는 필수입니다")
    private Long foodId;

    @Schema(description = "식사 구분", example = "아침", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "식사 구분은 필수입니다")
    private MealTime mealTime;

    @Schema(description = "섭취 시간 (KST, HH:mm)", example = "08:30", requiredMode = Schema.RequiredMode.REQUIRED)
    @JsonFormat(pattern = "HH:mm")
    @NotNull(message = "섭취 시간은 필수입니다")
    private LocalTime eatenTime;

    @Schema(description = "실제 섭취량 (g)", example = "180", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "섭취량은 필수입니다")
    @Positive(message = "섭취량은 0보다 커야 합니다")
    private Double eatenAmountGram;
}
