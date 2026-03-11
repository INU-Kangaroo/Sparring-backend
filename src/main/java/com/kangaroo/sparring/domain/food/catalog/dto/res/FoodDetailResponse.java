package com.kangaroo.sparring.domain.food.catalog.dto.res;

import com.kangaroo.sparring.domain.food.catalog.entity.Food;
import com.kangaroo.sparring.domain.food.catalog.entity.MealNutrition;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "음식 상세 정보 응답 (영양 정보 포함)")
public class FoodDetailResponse {

    @Schema(description = "음식 ID", example = "1")
    private Long id;

    @Schema(description = "음식명", example = "닭가슴살")
    private String name;

    @Schema(description = "1회 제공량", example = "100.0")
    private Double servingSize;

    @Schema(description = "제공량 단위", example = "g")
    private String servingUnit;

    @Schema(description = "기준량 라벨", example = "1인분")
    private String portionLabel;

    @Schema(description = "기준량 표시값", example = "350g")
    private String portionAmount;

    @Schema(description = "칼로리 (kcal)", example = "165.0")
    private Double calories;

    @Schema(description = "탄수화물 (g)", example = "0.0")
    private Double carbs;

    @Schema(description = "단백질 (g)", example = "31.0")
    private Double protein;

    @Schema(description = "지방 (g)", example = "3.6")
    private Double fat;

    @Schema(description = "나트륨 (mg)", example = "74.0")
    private Double sodium;

    @Schema(description = "당류 (g)", example = "0.0")
    private Double sugar;

    @Schema(description = "콜레스테롤 (mg)", example = "85.0")
    private Double cholesterol;

    @Schema(description = "포화지방 (g)", example = "1.0")
    private Double saturatedFat;

    public static FoodDetailResponse from(Food food, MealNutrition nutrition) {
        return FoodDetailResponse.builder()
                .id(food.getId())
                .name(food.getName())
                .servingSize(food.getServingSize())
                .servingUnit(food.getServingUnit())
                .portionLabel(food.getPortionLabel())
                .portionAmount(food.getPortionAmount())
                .calories(nutrition != null ? nutrition.getCalories() : null)
                .carbs(nutrition != null ? nutrition.getCarbs() : null)
                .protein(nutrition != null ? nutrition.getProtein() : null)
                .fat(nutrition != null ? nutrition.getFat() : null)
                .sodium(nutrition != null ? nutrition.getSodium() : null)
                .sugar(nutrition != null ? nutrition.getSugar() : null)
                .cholesterol(nutrition != null ? nutrition.getCholesterol() : null)
                .saturatedFat(nutrition != null ? nutrition.getSaturatedFat() : null)
                .build();
    }
}
