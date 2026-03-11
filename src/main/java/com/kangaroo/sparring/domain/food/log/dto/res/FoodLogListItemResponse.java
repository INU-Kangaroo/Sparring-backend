package com.kangaroo.sparring.domain.food.log.dto.res;

import com.kangaroo.sparring.domain.food.log.entity.FoodLog;
import com.kangaroo.sparring.domain.common.type.MealTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "식사 기록 목록 응답")
public class FoodLogListItemResponse {

    @Schema(description = "기록 ID", example = "1")
    private Long id;

    @Schema(description = "음식명", example = "닭가슴살 샐러드")
    private String foodName;

    @Schema(description = "식사 구분", example = "아침")
    private MealTime mealTime;

    @Schema(description = "섭취 일시", example = "2026-03-06T08:30:00")
    private LocalDateTime eatenAt;

    @Schema(description = "실제 섭취량 (g)", example = "180.0")
    private Double eatenAmountGram;

    @Schema(description = "칼로리 (kcal)", example = "210.0")
    private Double calories;

    public static FoodLogListItemResponse from(FoodLog log) {
        return FoodLogListItemResponse.builder()
                .id(log.getId())
                .foodName(log.getFoodName())
                .mealTime(log.getMealTime())
                .eatenAt(log.getEatenAt())
                .eatenAmountGram(log.getEatenAmountGram())
                .calories(log.getCalories())
                .build();
    }
}
