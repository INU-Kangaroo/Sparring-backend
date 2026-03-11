package com.kangaroo.sparring.domain.food.catalog.entity;

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

    @Column(name = "name", nullable = false, length = 255)
    private String name;

    @Column(name = "serving_size", nullable = false)
    private Double servingSize;

    @Column(name = "serving_unit", nullable = false, length = 20)
    private String servingUnit;

    @Column(name = "portion_label", length = 20)
    private String portionLabel;

    @Column(name = "portion_amount", columnDefinition = "TEXT")
    private String portionAmount;

    @Column(name = "manufacturer", length = 255)
    private String manufacturer;

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
                .portionLabel(null)
                .portionAmount(null)
                .manufacturer(null)
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

    public void updateDisplayMeta(String portionLabel, String portionAmount, String manufacturer) {
        this.portionLabel = portionLabel;
        this.portionAmount = portionAmount;
        this.manufacturer = manufacturer;
    }
}
