package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.SupplementRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface SupplementRecommendationRepository extends JpaRepository<SupplementRecommendation, Long> {

    List<SupplementRecommendation> findByRecommendationOrderByIdAsc(Recommendation recommendation);
}
