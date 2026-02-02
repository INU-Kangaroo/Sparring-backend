package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.ExerciseRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExerciseRecommendationRepository extends JpaRepository<ExerciseRecommendation, Long> {

    List<ExerciseRecommendation> findByRecommendationOrderByIdAsc(Recommendation recommendation);
}
