package com.kangaroo.sparring.domain.recommendation.repository;

import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import com.kangaroo.sparring.domain.recommendation.type.RecommendationType;
import com.kangaroo.sparring.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.Optional;

public interface RecommendationRepository extends JpaRepository<Recommendation, Long> {

    Optional<Recommendation> findTopByUserAndTypeAndFilterDurationAndFilterIntensityAndFilterLocationAndIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            User user,
            RecommendationType type,
            String filterDuration,
            String filterIntensity,
            String filterLocation,
            LocalDateTime createdAt
    );

    Optional<Recommendation> findTopByUserAndTypeAndIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
            User user,
            RecommendationType type,
            LocalDateTime createdAt
    );
}
