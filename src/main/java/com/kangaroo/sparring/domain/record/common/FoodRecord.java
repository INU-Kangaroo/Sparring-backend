package com.kangaroo.sparring.domain.record.common;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class FoodRecord implements TemporalRecord {
    private final LocalDateTime eatenAt;
    private final Double calories;
    private final String foodName;
    private final Double carbs;
    private final Double protein;
    private final Double fat;
    private final Double sodium;
    private final Double sugar;
    private final Double fiber;

    public FoodRecord(LocalDateTime eatenAt, Double calories) {
        this.eatenAt = eatenAt;
        this.calories = calories;
        this.foodName = null;
        this.carbs = null;
        this.protein = null;
        this.fat = null;
        this.sodium = null;
        this.sugar = null;
        this.fiber = null;
    }

    public FoodRecord(LocalDateTime eatenAt, String foodName, Double calories,
                      Double carbs, Double protein, Double fat,
                      Double sodium, Double sugar, Double fiber) {
        this.eatenAt = eatenAt;
        this.foodName = foodName;
        this.calories = calories;
        this.carbs = carbs;
        this.protein = protein;
        this.fat = fat;
        this.sodium = sodium;
        this.sugar = sugar;
        this.fiber = fiber;
    }

    @Override
    public LocalDateTime occurredAt() {
        return eatenAt;
    }
}
