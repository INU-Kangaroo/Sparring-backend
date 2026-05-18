package com.kangaroo.sparring.domain.recommendation.entity;

import com.kangaroo.sparring.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import static com.kangaroo.sparring.global.support.KstDateTimeSupport.nowDateTime;

@Entity
@Table(name = "meal_recommendation")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class MealRecommendation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "meal_type", nullable = false, length = 20)
    private String mealType;

    @Column(name = "meal_target_kcal", precision = 8, scale = 2)
    private BigDecimal mealTargetKcal;

    @Column(name = "refresh_nonce", length = 64)
    private String refreshNonce;

    @Column(name = "recommended_at", nullable = false)
    private LocalDateTime recommendedAt;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @OneToMany(mappedBy = "mealRecommendation", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<MealRecommendationItem> items = new ArrayList<>();

    @PrePersist
    protected void onCreate() {
        createdAt = updatedAt = nowDateTime();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = nowDateTime();
    }
}
