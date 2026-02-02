package com.kangaroo.sparring.domain.food.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

/**
 * 음식 마스터 데이터
 */
@Entity
@Table(name = "food")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class Food extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "name", nullable = false, length = 100)
    private String name;

    @Column(name = "serving_size", nullable = false)
    private Double servingSize;

    @Column(name = "serving_unit", nullable = false, length = 20)
    private String servingUnit;

    @OneToOne(mappedBy = "food", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private MealNutrition mealNutrition;

    /**
     * 음식 생성
     */
    public static Food create(String name, Double servingSize, String servingUnit) {
        return Food.builder()
                .name(name)
                .servingSize(servingSize)
                .servingUnit(servingUnit)
                .build();
    }

    /**
     * 영양 정보 설정
     */
    public void setMealNutrition(MealNutrition mealNutrition) {
        this.mealNutrition = mealNutrition;
    }

    /**
     * 음식 정보 수정
     */
    public void update(String name, Double servingSize, String servingUnit) {
        this.name = name;
        this.servingSize = servingSize;
        this.servingUnit = servingUnit;
    }
}