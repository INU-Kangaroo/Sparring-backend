package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.catalog.service.ExerciseCandidateService;
import com.kangaroo.sparring.domain.catalog.entity.Exercise;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.service.HealthProfileService;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.recommendation.dto.req.ExerciseRecommendationRequest;
import com.kangaroo.sparring.domain.recommendation.dto.res.CardiacExerciseResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.ExerciseRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.StrengthExerciseResponse;
import com.kangaroo.sparring.domain.recommendation.entity.ExerciseRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import com.kangaroo.sparring.domain.recommendation.repository.ExerciseRecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.repository.RecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.type.ExerciseType;
import com.kangaroo.sparring.domain.recommendation.type.RecommendationType;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseRecommendationService {
    private static final int CACHE_HOURS = 24;
    private static final int RECENT_MEASUREMENT_COUNT = 7;

    private final RecommendationRepository recommendationRepository;
    private final ExerciseRecommendationRepository exerciseRecommendationRepository;
    private final ExerciseRecommendationAiClient exerciseRecommendationAiClient;
    private final RecommendationPromptTemplateService promptTemplateService;
    private final RecommendationJsonMappingSupport jsonMappingSupport;
    private final ExerciseCandidateService exerciseCandidateService;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final HealthProfileService healthProfileService;
    private final RecordReadService recordReadService;
    private final Clock kstClock;

    public ExerciseRecommendationResponse getExerciseRecommendations(Long userId, ExerciseRecommendationRequest request) {
        User user = userLookupService.getUserOrThrow(userId);
        LocalDateTime cacheThreshold = LocalDateTime.now(kstClock).minusHours(CACHE_HOURS);
        return recommendationRepository
                .findCachedExerciseRecommendation(
                        user.getId(),
                        RecommendationType.EXERCISE,
                        request.getDuration().name(),
                        request.getIntensity().name(),
                        request.getLocation().name(),
                        cacheThreshold
                )
                .map(cached -> {
                    log.info("운동 추천 캐시 반환: userId={}, duration={}, intensity={}, location={}",
                            user.getId(), request.getDuration().name(), request.getIntensity().name(), request.getLocation().name());
                    return buildExerciseRecommendationResponse(cached);
                })
                .orElseGet(() -> {
                    log.info("운동 추천 캐시 미스, 신규 생성: userId={}, duration={}, intensity={}, location={}",
                            user.getId(), request.getDuration().name(), request.getIntensity().name(), request.getLocation().name());
                    return generateNewExerciseRecommendations(user, request);
                });
    }

    public ExerciseRecommendationResponse refreshExerciseRecommendations(Long userId, ExerciseRecommendationRequest request) {
        User user = userLookupService.getUserOrThrow(userId);
        log.info("운동 추천 강제 새로고침 요청: userId={}, duration={}, intensity={}, location={}",
                user.getId(), request.getDuration().name(), request.getIntensity().name(), request.getLocation().name());
        return generateNewExerciseRecommendations(user, request);
    }

    private ExerciseRecommendationResponse generateNewExerciseRecommendations(User user, ExerciseRecommendationRequest request) {
        HealthProfile healthProfile = healthProfileService.getOrCreateHealthProfile(user.getId());
        List<BloodSugarRecord> recentBloodSugars =
                recordReadService.getRecentBloodSugarRecords(user.getId(), RECENT_MEASUREMENT_COUNT);
        List<BloodPressureRecord> recentBloodPressures =
                recordReadService.getRecentBloodPressureRecords(user.getId(), RECENT_MEASUREMENT_COUNT);

        // 마스터 테이블 필터링으로 후보 생성
        List<Exercise> candidates = exerciseCandidateService.findCandidates(
                healthProfile, recentBloodPressures, request.getIntensity(), request.getLocation()
        );
        String candidatesText = exerciseCandidateService.buildCandidatesText(candidates);

        String prompt = buildExercisePrompt(healthProfile, recentBloodSugars, recentBloodPressures, request, candidatesText);
        ExerciseRecommendationResponse response = exerciseRecommendationAiClient.recommend(prompt);

        Recommendation recommendation = Recommendation.createExerciseRecommendation(
                user,
                request.getDuration().name(),
                request.getIntensity().name(),
                request.getLocation().name()
        );
        recommendationRepository.save(recommendation);
        List<ExerciseRecommendation> exercises = buildExerciseEntities(recommendation, response);
        if (!exercises.isEmpty()) {
            exerciseRecommendationRepository.saveAll(exercises);
        }

        log.info("새로운 운동 추천 생성 완료: userId={}", user.getId());
        return response;
    }

    private String buildExercisePrompt(HealthProfile healthProfile,
                                       List<BloodSugarRecord> bloodSugars,
                                       List<BloodPressureRecord> bloodPressures,
                                       ExerciseRecommendationRequest request,
                                       String candidatesText) {
        String userHealthInfo = promptTemplateService.buildUserHealthInfo(healthProfile, bloodSugars, bloodPressures);
        return promptTemplateService.renderExercisePrompt(Map.of(
                "USER_HEALTH_INFO", userHealthInfo,
                "DURATION", request.getDuration().getDescription(),
                "INTENSITY", request.getIntensity().getDescription(),
                "LOCATION", request.getLocation().getDescription(),
                "EXERCISE_CANDIDATES", candidatesText
        ));
    }

    private List<ExerciseRecommendation> buildExerciseEntities(Recommendation recommendation, ExerciseRecommendationResponse response) {
        List<ExerciseRecommendation> exercises = new ArrayList<>();

        for (CardiacExerciseResponse dto : response.getCardiacExercises()) {
            exercises.add(ExerciseRecommendation.of(
                    recommendation,
                    ExerciseType.CARDIAC,
                    dto.getName(),
                    dto.getDuration(),
                    dto.getMinCalories(),
                    dto.getMaxCalories(),
                    null,
                    jsonMappingSupport.writeAsJson(dto.getPrecautions())
            ));
        }

        for (StrengthExerciseResponse dto : response.getStrengthExercises()) {
            exercises.add(ExerciseRecommendation.of(
                    recommendation,
                    ExerciseType.STRENGTH,
                    dto.getName(),
                    dto.getDuration(),
                    null,
                    null,
                    dto.getFrequency(),
                    jsonMappingSupport.writeAsJson(dto.getPrecautions())
            ));
        }

        return exercises;
    }

    private ExerciseRecommendationResponse buildExerciseRecommendationResponse(Recommendation recommendation) {
        List<ExerciseRecommendation> exercises = exerciseRecommendationRepository.findByRecommendationOrderByIdAsc(recommendation);

        List<CardiacExerciseResponse> cardiacExercises = exercises.stream()
                .filter(e -> e.getExerciseType() == ExerciseType.CARDIAC)
                .map(e -> CardiacExerciseResponse.of(
                        e.getName(),
                        e.getDuration(),
                        e.getMinCalories(),
                        e.getMaxCalories(),
                        jsonMappingSupport.readJsonArray(e.getPrecautions())
                ))
                .toList();

        List<StrengthExerciseResponse> strengthExercises = exercises.stream()
                .filter(e -> e.getExerciseType() == ExerciseType.STRENGTH)
                .map(e -> StrengthExerciseResponse.of(
                        e.getName(),
                        e.getDuration(),
                        e.getFrequency(),
                        jsonMappingSupport.readJsonArray(e.getPrecautions())
                ))
                .toList();

        return ExerciseRecommendationResponse.of(cardiacExercises, strengthExercises);
    }
}
