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
@Schema(description = "음식 기본 정보 응답")
public class FoodResponse {

    @Schema(description = "음식 ID", example = "1")
    private Long id;

    @Schema(description = "음식명", example = "닭가슴살")
    private String name;

    @Schema(description = "1회 제공량", example = "100.0")
    private Double servingSize;

    @Schema(description = "제공량 단위", example = "g")
    private String servingUnit;

    @Schema(description = "칼로리 (kcal)", example = "165.0")
    private Double calories;

    @Schema(description = "제공량 라벨", example = "1인분")
    private String portionLabel;

    @Schema(description = "제공량 표시값", example = "350g")
    private String portionAmount;

    @Schema(description = "제조사명", example = "대한푸드텍(주)")
    private String manufacturer;

    public static FoodResponse from(Food food) {
        MealNutrition nutrition = food.getMealNutrition();
        return FoodResponse.builder()
                .id(food.getId())
                .name(food.getName())
                .servingSize(food.getServingSize())
                .servingUnit(food.getServingUnit())
                .calories(nutrition != null ? nutrition.getCalories() : null)
                .portionLabel(food.getPortionLabel())
                .portionAmount(food.getPortionAmount())
                .manufacturer(food.getManufacturer())
                .build();
    }
}
