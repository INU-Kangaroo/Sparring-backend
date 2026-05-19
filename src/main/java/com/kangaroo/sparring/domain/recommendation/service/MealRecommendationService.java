package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.recommendation.dto.ml.MealRecommendationMlRequest;
import com.kangaroo.sparring.domain.recommendation.dto.res.MealRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendationItem;
import com.kangaroo.sparring.domain.recommendation.repository.MealRecommendationRepository;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.FoodRecord;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class MealRecommendationService {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static final int RECENT_BLOOD_SUGAR_COUNT = 20;
    private static final int RECENT_BLOOD_PRESSURE_COUNT = 20;

    private final RecordReadService recordReadService;
    private final MealRecommendationAiClient aiClient;
    private final MealRecommendationMlRequestFactory mlRequestFactory;
    private final MealRecommendationRepository mealRecommendationRepository;
    private final MealRecommendationCachePolicy cachePolicy;
    private final Clock kstClock;

    /**
     * 식단 추천 조회 — DB 캐시 있으면 반환, 없으면 FastAPI 호출
     */
    @Transactional
    public MealRecommendationResponse recommend(User user, HealthProfile profile, MealTime mealTime) {
        LocalDate today = LocalDate.now(kstClock);
        LocalDateTime now = LocalDateTime.now(kstClock);

        return mealRecommendationRepository
                .findTopByUser_IdAndMealTypeOrderByRecommendedAtDesc(user.getId(), mealTime.name())
                .map(cachedRecommendation -> toCachedResponseOrRefresh(
                        user, profile, mealTime, today, now, cachedRecommendation
                ))
                .orElseGet(() -> {
                    log.info("식단 추천 캐시 미스, 신규 생성: userId={}, mealType={}", user.getId(), mealTime.name());
                    return refresh(user, profile, mealTime);
                });
    }

    private MealRecommendationResponse toCachedResponseOrRefresh(
            User user,
            HealthProfile profile,
            MealTime mealTime,
            LocalDate today,
            LocalDateTime now,
            MealRecommendation cachedRecommendation
    ) {
        MealRecommendationCachePolicy.InvalidationReason invalidationReason = cachePolicy.resolve(
                user.getId(), profile, mealTime, today, now, cachedRecommendation
        );
        if (invalidationReason != MealRecommendationCachePolicy.InvalidationReason.NONE) {
            logCacheInvalidation(invalidationReason, user, profile, mealTime, today, cachedRecommendation);
            return refresh(user, profile, mealTime);
        }

        try {
            log.info("식단 추천 캐시 반환: userId={}, mealType={}", user.getId(), mealTime.name());
            return toResponse(cachedRecommendation);
        } catch (Exception e) {
            log.warn("식단 추천 캐시 변환 실패, 재생성 시도 userId={}, mealType={}", user.getId(), mealTime.name(), e);
            return refresh(user, profile, mealTime);
        }
    }

    private void logCacheInvalidation(
            MealRecommendationCachePolicy.InvalidationReason reason,
            User user,
            HealthProfile profile,
            MealTime mealTime,
            LocalDate today,
            MealRecommendation cachedRecommendation
    ) {
        LocalDateTime recommendedAt = cachedRecommendation.getRecommendedAt();
        switch (reason) {
            case EXPIRED_DATE -> {
                LocalDate cachedDate = recommendedAt != null ? recommendedAt.toLocalDate() : null;
                log.info("식단 추천 캐시 만료(당일 아님), 신규 생성: userId={}, mealType={}, cachedDate={}, today={}",
                        user.getId(), mealTime.name(), cachedDate, today);
            }
            case PROFILE_CHANGED -> log.info("식단 추천 캐시 무효화(프로필 변경): userId={}, mealType={}, recommendedAt={}, profileUpdatedAt={}",
                    user.getId(), mealTime.name(), recommendedAt, profile != null ? profile.getUpdatedAt() : null);
            case FOOD_INPUT_CHANGED -> log.info("식단 추천 캐시 무효화(식사 로그 변경): userId={}, mealType={}, recommendedAt={}",
                    user.getId(), mealTime.name(), recommendedAt);
            case VITALS_INPUT_CHANGED -> log.info("식단 추천 캐시 무효화(혈당/혈압 로그 변경): userId={}, mealType={}, recommendedAt={}",
                    user.getId(), mealTime.name(), recommendedAt);
            case NONE -> {
            }
        }
    }

    /**
     * 식단 추천 새로고침 — FastAPI 호출 후 DB 저장
     */
    @Transactional
    public MealRecommendationResponse refresh(User user, HealthProfile profile, MealTime mealTime) {
        return refresh(user, profile, mealTime, false);
    }

    @Transactional
    public MealRecommendationResponse refresh(User user, HealthProfile profile, MealTime mealTime, boolean forceRefreshNonce) {
        long startedAt = System.currentTimeMillis();
        log.info("식단 추천 강제 새로고침/생성 요청: userId={}, mealType={}", user.getId(), mealTime.name());
        LocalDate today = LocalDate.now(kstClock);

        List<BloodSugarRecord> recentBs = recordReadService.getRecentBloodSugarRecords(user.getId(), RECENT_BLOOD_SUGAR_COUNT);
        List<BloodPressureRecord> recentBp = recordReadService.getRecentBloodPressureRecords(user.getId(), RECENT_BLOOD_PRESSURE_COUNT);
        List<FoodRecord> recentFoods = recordReadService.getRecentFoodRecords(user.getId(), today.minusDays(3), today);

        log.info("식단 추천 AI 요청 시작: userId={}, mealType={}, bloodSugarLogs={}, bloodPressureLogs={}, recentFoods={}",
                user.getId(), mealTime.name(), recentBs.size(), recentBp.size(), recentFoods.size());
        MealRecommendationMlRequest requestBody = mlRequestFactory.create(
                user, profile, mealTime, recentBs, recentBp, recentFoods, forceRefreshNonce
        );
        MealRecommendationAiClient.AiRecommendResult result = aiClient.recommend(requestBody);
        log.info("식단 추천 AI 응답 수신: userId={}, mealType={}, cards={}",
                user.getId(), mealTime.name(), result.cards() == null ? 0 : result.cards().size());

        MealRecommendation saved = saveRecommendation(user, mealTime, requestBody, result);
        log.info("식단 추천 저장 완료: userId={}, mealType={}, recommendationId={}, elapsedMs={}",
                user.getId(), mealTime.name(), saved.getId(), System.currentTimeMillis() - startedAt);
        return toResponse(saved);
    }

    private MealRecommendation saveRecommendation(
            User user,
            MealTime mealTime,
            MealRecommendationMlRequest requestBody,
            MealRecommendationAiClient.AiRecommendResult result
    ) {
        if (result.cards() == null || result.cards().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "추천 카드가 비어 있습니다.");
        }

        MealRecommendation recommendation = MealRecommendation.builder()
                .user(user)
                .mealType(mealTime.name())
                .mealTargetKcal(BigDecimal.valueOf(result.mealTargetKcal()))
                .refreshNonce(requestBody.refreshNonce())
                .recommendedAt(LocalDateTime.now(kstClock))
                .build();

        for (MealRecommendationResponse.RecommendationCardDto card : result.cards()) {
            if (card.getNutrients() == null) {
                throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "추천 카드 영양 정보가 누락되었습니다.");
            }

            try {
                MealRecommendationItem item = MealRecommendationItem.builder()
                        .mealRecommendation(recommendation)
                        .rankOrder(card.getRank())
                        .title(card.getTitle())
                        .totalKcal(BigDecimal.valueOf(card.getNutrients().getKcal()))
                        .totalCarbs(BigDecimal.valueOf(card.getNutrients().getCarbs()))
                        .totalProtein(BigDecimal.valueOf(card.getNutrients().getProtein()))
                        .totalFat(BigDecimal.valueOf(card.getNutrients().getFat()))
                        .totalSodium(card.getNutrients().getSodium() != null
                                ? BigDecimal.valueOf(card.getNutrients().getSodium()) : null)
                        .reasonsJson(OBJECT_MAPPER.writeValueAsString(card.getReasons()))
                        .menusJson(OBJECT_MAPPER.writeValueAsString(card.getMenus()))
                        .build();
                recommendation.getItems().add(item);
            } catch (CustomException e) {
                throw e;
            } catch (Exception e) {
                throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "추천 카드 저장 중 오류가 발생했습니다.");
            }
        }

        if (recommendation.getItems().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "저장 가능한 추천 카드가 없습니다.");
        }

        return mealRecommendationRepository.save(recommendation);
    }

    private MealRecommendationResponse toResponse(MealRecommendation rec) {
        if (rec.getItems() == null || rec.getItems().isEmpty()) {
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "캐시된 추천 카드가 없습니다.");
        }

        List<MealRecommendationResponse.RecommendationCardDto> cards = rec.getItems().stream()
                .sorted((a, b) -> Integer.compare(a.getRankOrder(), b.getRankOrder()))
                .map(item -> {
                    try {
                        List<String> reasons = OBJECT_MAPPER.readValue(
                                item.getReasonsJson(), new TypeReference<>() {});
                        List<MealRecommendationResponse.MenuItemDto> menus = OBJECT_MAPPER.readValue(
                                item.getMenusJson(), new TypeReference<>() {});

                        return MealRecommendationResponse.RecommendationCardDto.builder()
                                .recommendationCardId(item.getId())
                                .rank(item.getRankOrder())
                                .title(item.getTitle())
                                .nutrients(MealRecommendationResponse.NutrientsDto.builder()
                                        .kcal(item.getTotalKcal() != null ? item.getTotalKcal().doubleValue() : 0)
                                        .carbs(item.getTotalCarbs() != null ? item.getTotalCarbs().doubleValue() : 0)
                                        .protein(item.getTotalProtein() != null ? item.getTotalProtein().doubleValue() : 0)
                                        .fat(item.getTotalFat() != null ? item.getTotalFat().doubleValue() : 0)
                                        .sodium(item.getTotalSodium() != null ? item.getTotalSodium().doubleValue() : null)
                                        .build())
                                .reasons(reasons)
                                .menus(menus)
                                .build();
                    } catch (Exception e) {
                        throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED, "추천 카드 변환 중 오류가 발생했습니다.");
                    }
                })
                .toList();

        return MealRecommendationResponse.builder()
                .recommendationId(rec.getId())
                .mealType(rec.getMealType())
                .recommendations(cards)
                .build();
    }
}
