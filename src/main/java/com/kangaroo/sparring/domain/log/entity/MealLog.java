package com.kangaroo.sparring.domain.log.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.food.entity.Food;
import com.kangaroo.sparring.domain.food.entity.MealNutrition;
import com.kangaroo.sparring.domain.log.type.MealTime;
import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "meal_log")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MealLog extends BaseEntity {

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

    public static MealLog withFood(User user, Food food, MealTime mealTime, LocalDateTime eatenAt) {
        MealNutrition nutrition = food.getMealNutrition();

        return MealLog.builder()
                .user(user)
                .food(food)
                .foodName(food.getName())
                .mealTime(mealTime)
                .eatenAt(eatenAt)
                .calories(nutrition != null ? nutrition.getCalories() : null)
                .carbs(nutrition != null ? nutrition.getCarbs() : null)
                .protein(nutrition != null ? nutrition.getProtein() : null)
                .fat(nutrition != null ? nutrition.getFat() : null)
                .sodium(nutrition != null ? nutrition.getSodium() : null)
                .build();
    }

    public static MealLog withoutFood(User user, String foodName, MealTime mealTime, LocalDateTime eatenAt) {
        return MealLog.builder()
                .user(user)
                .food(null)
                .foodName(foodName)
                .mealTime(mealTime)
                .eatenAt(eatenAt)
                .build();
    }
}
