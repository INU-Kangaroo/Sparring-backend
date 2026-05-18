package com.kangaroo.sparring.domain.recommendation.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static com.kangaroo.sparring.global.support.KstDateTimeSupport.nowDateTime;

@Entity
@Table(name = "meal_recommendation_item")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MealRecommendationItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "meal_recommendation_id", nullable = false)
    private MealRecommendation mealRecommendation;

    @Column(name = "rank_order", nullable = false)
    private int rankOrder;

    @Column(name = "title", nullable = false, length = 500)
    private String title;

    @Column(name = "total_kcal", precision = 8, scale = 2)
    private BigDecimal totalKcal;

    @Column(name = "total_carbs", precision = 8, scale = 2)
    private BigDecimal totalCarbs;

    @Column(name = "total_protein", precision = 8, scale = 2)
    private BigDecimal totalProtein;

    @Column(name = "total_fat", precision = 8, scale = 2)
    private BigDecimal totalFat;

    @Column(name = "total_sodium", precision = 8, scale = 2)
    private BigDecimal totalSodium;

    @Column(name = "reasons_json", columnDefinition = "JSON")
    private String reasonsJson;

    @Column(name = "menus_json", columnDefinition = "JSON")
    private String menusJson;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        createdAt = nowDateTime();
    }
}
