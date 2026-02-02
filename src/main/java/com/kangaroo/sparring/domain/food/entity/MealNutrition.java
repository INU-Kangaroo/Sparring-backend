package com.kangaroo.sparring.domain.food.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 음식 영양 정보
 */
@Entity
@Table(name = "meal_nutrition")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MealNutrition extends BaseEntity {

    @Id
    private Long foodId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "food_id")
    private Food food;

    @Column(name = "calories", nullable = false)
    private Double calories;

    @Column(name = "carbs", nullable = false)
    private Double carbs;

    @Column(name = "protein", nullable = false)
    private Double protein;

    @Column(name = "fat", nullable = false)
    private Double fat;

    @Column(name = "sodium")
    private Double sodium;

    @Column(name = "sugar")
    private Double sugar;

    @Column(name = "cholesterol")
    private Double cholesterol;

    @Column(name = "saturated_fat")
    private Double saturatedFat;

    /**
     * 영양 정보 생성
     */
    public static MealNutrition create(Food food, Double calories, Double carbs,
                                       Double protein, Double fat) {
        return MealNutrition.builder()
                .food(food)
                .calories(calories)
                .carbs(carbs)
                .protein(protein)
                .fat(fat)
                .build();
    }

    /**
     * 영양 정보 수정
     */
    public void update(Double calories, Double carbs, Double protein, Double fat,
                       Double sodium, Double sugar, Double cholesterol, Double saturatedFat) {
        this.calories = calories;
        this.carbs = carbs;
        this.protein = protein;
        this.fat = fat;
        this.sodium = sodium;
        this.sugar = sugar;
        this.cholesterol = cholesterol;
        this.saturatedFat = saturatedFat;
    }
}