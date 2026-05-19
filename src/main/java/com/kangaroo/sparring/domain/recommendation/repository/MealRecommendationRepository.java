package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface MealRecommendationRepository extends JpaRepository<MealRecommendation, Long> {

    Optional<MealRecommendation> findTopByUser_IdAndMealTypeOrderByRecommendedAtDesc(Long userId, String mealType);
}
