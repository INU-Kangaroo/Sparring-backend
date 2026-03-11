package com.kangaroo.sparring.domain.food.log.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.food.catalog.entity.Food;
import com.kangaroo.sparring.domain.food.catalog.entity.MealNutrition;
import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "meal_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class FoodLog extends BaseEntity {
    private static final double DEFAULT_SERVING_SIZE_GRAM = 100d;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "food_id")
    private Food food;

    @Column(name = "food_name", nullable = false, length = 255)
    private String foodName;

    @Enumerated(EnumType.STRING)
    @Column(name = "meal_time", nullable = false, length = 20)
    private MealTime mealTime;

    @Column(name = "eaten_at", nullable = false)
    private LocalDateTime eatenAt;

    @Column(name = "eaten_amount_gram")
    private Double eatenAmountGram;

    @Column(name = "calories")
    private Double calories;

    @Column(name = "carbs")
    private Double carbs;

    @Column(name = "protein")
    private Double protein;

    @Column(name = "fat")
    private Double fat;

    @Column(name = "sodium")
    private Double sodium;

    public static FoodLog withFood(User user, Food food, MealTime mealTime, LocalDateTime eatenAt, Double eatenAmountGram) {
        MealNutrition nutrition = food.getMealNutrition();
        double baseServingSize = resolveBaseServingSize(food.getServingSize());
        double ratio = eatenAmountGram / baseServingSize;

        return FoodLog.builder()
                .user(user)
                .food(food)
                .foodName(food.getName())
                .mealTime(mealTime)
                .eatenAt(eatenAt)
                .eatenAmountGram(eatenAmountGram)
                .calories(scaleNutrition(nutrition != null ? nutrition.getCalories() : null, ratio))
                .carbs(scaleNutrition(nutrition != null ? nutrition.getCarbs() : null, ratio))
                .protein(scaleNutrition(nutrition != null ? nutrition.getProtein() : null, ratio))
                .fat(scaleNutrition(nutrition != null ? nutrition.getFat() : null, ratio))
                .sodium(scaleNutrition(nutrition != null ? nutrition.getSodium() : null, ratio))
                .build();
    }

    public static FoodLog withoutFood(User user, String foodName, MealTime mealTime, LocalDateTime eatenAt) {
        return FoodLog.builder()
                .user(user)
                .food(null)
                .foodName(foodName)
                .mealTime(mealTime)
                .eatenAt(eatenAt)
                .build();
    }

    private static Double scaleNutrition(Double value, double ratio) {
        if (value == null) return null;
        return value * ratio;
    }

    private static double resolveBaseServingSize(Double servingSize) {
        if (servingSize == null || servingSize <= 0d) return DEFAULT_SERVING_SIZE_GRAM;
        return servingSize;
    }
}
