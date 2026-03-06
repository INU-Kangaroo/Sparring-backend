package com.kangaroo.sparring.domain.log.dto.res;

import com.kangaroo.sparring.domain.log.entity.MealLog;
import com.kangaroo.sparring.domain.log.type.MealTime;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "식사 기록 응답")
public class MealLogResponse {

    @Schema(description = "기록 ID", example = "1")
    private Long id;

    @Schema(description = "음식명", example = "닭가슴살 샐러드")
    private String foodName;

    @Schema(description = "매핑된 음식 ID", example = "100")
    private Long foodId;

    @Schema(description = "식사 구분", example = "아침")
    private MealTime mealTime;

    @Schema(description = "섭취 일시", example = "2026-03-06T08:30:00")
    private LocalDateTime eatenAt;

    @Schema(description = "칼로리 (kcal)", example = "210.0")
    private Double calories;

    @Schema(description = "탄수화물 (g)", example = "12.0")
    private Double carbs;

    @Schema(description = "단백질 (g)", example = "26.0")
    private Double protein;

    @Schema(description = "지방 (g)", example = "7.0")
    private Double fat;

    @Schema(description = "나트륨 (mg)", example = "280.0")
    private Double sodium;

    public static MealLogResponse from(MealLog log) {
        return MealLogResponse.builder()
                .id(log.getId())
                .foodName(log.getFoodName())
                .foodId(log.getFood() != null ? log.getFood().getId() : null)
                .mealTime(log.getMealTime())
                .eatenAt(log.getEatenAt())
                .calories(log.getCalories())
                .carbs(log.getCarbs())
                .protein(log.getProtein())
                .fat(log.getFat())
                .sodium(log.getSodium())
                .build();
    }
}
