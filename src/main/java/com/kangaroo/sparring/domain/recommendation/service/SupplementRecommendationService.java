package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.service.HealthProfileService;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.recommendation.dto.res.SupplementResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.SupplementRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.entity.SupplementRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import com.kangaroo.sparring.domain.recommendation.repository.SupplementRecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.repository.RecommendationRepository;
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
public class SupplementRecommendationService {

    private static final int CACHE_HOURS = 168;
    private static final int RECENT_MEASUREMENT_COUNT = 7;

    private final RecommendationRepository recommendationRepository;
    private final SupplementRecommendationRepository supplementRecommendationRepository;
    private final SupplementRecommendationAiClient supplementRecommendationAiClient;
    private final RecommendationPromptTemplateService promptTemplateService;
    private final RecommendationJsonMappingSupport jsonMappingSupport;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final HealthProfileService healthProfileService;
    private final RecordReadService recordReadService;
    private final Clock kstClock;

    public SupplementRecommendationResponse getSupplementRecommendations(Long userId) {
        User user = userLookupService.getUserOrThrow(userId);
        LocalDateTime cacheThreshold = LocalDateTime.now(kstClock).minusHours(CACHE_HOURS);
        return recommendationRepository
                .findCachedRecommendation(
                        user.getId(),
                        RecommendationType.SUPPLEMENT,
                        cacheThreshold
                )
                .map(cached -> {
                    log.info("영양제 추천 캐시 반환: userId={}", user.getId());
                    return buildSupplementRecommendationResponse(cached);
                })
                .orElseGet(() -> {
                    log.info("영양제 추천 캐시 미스, 신규 생성: userId={}", user.getId());
                    return generateNewSupplementRecommendations(user);
                });
    }

    public SupplementRecommendationResponse refreshSupplementRecommendations(Long userId) {
        User user = userLookupService.getUserOrThrow(userId);
        log.info("영양제 추천 강제 새로고침 요청: userId={}", user.getId());
        return generateNewSupplementRecommendations(user);
    }

    private SupplementRecommendationResponse generateNewSupplementRecommendations(User user) {
        HealthProfile healthProfile = healthProfileService.getOrCreateHealthProfile(user.getId());
        List<BloodSugarRecord> recentBloodSugars =
                recordReadService.getRecentBloodSugarRecords(user.getId(), RECENT_MEASUREMENT_COUNT);
        List<BloodPressureRecord> recentBloodPressures =
                recordReadService.getRecentBloodPressureRecords(user.getId(), RECENT_MEASUREMENT_COUNT);

        String prompt = buildSupplementPrompt(healthProfile, recentBloodSugars, recentBloodPressures);
        SupplementRecommendationResponse response = supplementRecommendationAiClient.recommend(prompt);

        Recommendation recommendation = Recommendation.createSupplementRecommendation(user);
        recommendationRepository.save(recommendation);
        List<SupplementRecommendation> supplements = buildSupplementEntities(recommendation, response);
        if (!supplements.isEmpty()) {
            supplementRecommendationRepository.saveAll(supplements);
        }

        log.info("새로운 영양제 추천 생성 완료: userId={}", user.getId());
        return response;
    }

    private String buildSupplementPrompt(HealthProfile healthProfile,
                                         List<BloodSugarRecord> bloodSugars,
                                         List<BloodPressureRecord> bloodPressures) {
        String userHealthInfo = promptTemplateService.buildUserHealthInfo(healthProfile, bloodSugars, bloodPressures);
        return promptTemplateService.renderSupplementPrompt(Map.of(
                "USER_HEALTH_INFO", userHealthInfo
        ));
    }

    private List<SupplementRecommendation> buildSupplementEntities(Recommendation recommendation, SupplementRecommendationResponse response) {
        List<SupplementRecommendation> supplements = new ArrayList<>();

        for (SupplementResponse dto : response.getSupplements()) {
            supplements.add(SupplementRecommendation.of(
                    recommendation,
                    dto.getName(),
                    dto.getDosage(),
                    dto.getFrequency(),
                    jsonMappingSupport.writeAsJson(dto.getBenefits()),
                    jsonMappingSupport.writeAsJson(dto.getPrecautions())
            ));
        }

        return supplements;
    }

    private SupplementRecommendationResponse buildSupplementRecommendationResponse(Recommendation recommendation) {
        List<SupplementRecommendation> supplements = supplementRecommendationRepository.findByRecommendationOrderByIdAsc(recommendation);

        List<SupplementResponse> supplementDtos = supplements.stream()
                .map(n -> SupplementResponse.of(
                        n.getName(),
                        n.getDosage(),
                        n.getFrequency(),
                        jsonMappingSupport.readJsonArray(n.getBenefits()),
                        jsonMappingSupport.readJsonArray(n.getPrecautions())
                ))
                .toList();

        return SupplementRecommendationResponse.of(supplementDtos);
    }
}
