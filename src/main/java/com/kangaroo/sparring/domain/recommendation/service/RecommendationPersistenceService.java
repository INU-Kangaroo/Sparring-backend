package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.recommendation.entity.ExerciseRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import com.kangaroo.sparring.domain.recommendation.entity.SupplementRecommendation;
import com.kangaroo.sparring.domain.recommendation.repository.ExerciseRecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.repository.RecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.repository.SupplementRecommendationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class RecommendationPersistenceService {

    private final RecommendationRepository recommendationRepository;
    private final ExerciseRecommendationRepository exerciseRecommendationRepository;
    private final SupplementRecommendationRepository supplementRecommendationRepository;

    @Transactional
    public void persistExerciseRecommendation(Recommendation recommendation, List<ExerciseRecommendation> exercises) {
        recommendationRepository.save(recommendation);
        if (!exercises.isEmpty()) {
            exerciseRecommendationRepository.saveAll(exercises);
        }
    }

    @Transactional
    public void persistSupplementRecommendation(Recommendation recommendation, List<SupplementRecommendation> supplements) {
        recommendationRepository.save(recommendation);
        if (!supplements.isEmpty()) {
            supplementRecommendationRepository.saveAll(supplements);
        }
    }
}
