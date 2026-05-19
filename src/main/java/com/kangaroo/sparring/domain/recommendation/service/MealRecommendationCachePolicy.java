package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendation;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;

@Component
@RequiredArgsConstructor
public class MealRecommendationCachePolicy {

    private static final LocalTime BREAKFAST_END = LocalTime.of(11, 0);
    private static final LocalTime LUNCH_END = LocalTime.of(15, 0);
    private static final LocalTime DINNER_END = LocalTime.of(21, 0);

    private final RecordReadService recordReadService;

    public InvalidationReason resolve(
            Long userId,
            HealthProfile profile,
            MealTime mealTime,
            LocalDate today,
            LocalDateTime now,
            MealRecommendation cachedRecommendation
    ) {
        LocalDateTime recommendedAt = cachedRecommendation.getRecommendedAt();
        LocalDate cachedDate = recommendedAt != null ? recommendedAt.toLocalDate() : null;
        if (cachedDate == null || !cachedDate.isEqual(today)) {
            return InvalidationReason.EXPIRED_DATE;
        }
        if (isProfileChangedSinceRecommendation(profile, recommendedAt)) {
            return InvalidationReason.PROFILE_CHANGED;
        }
        if (hasFoodInputChangedAffectingMealType(userId, mealTime, recommendedAt)) {
            return InvalidationReason.FOOD_INPUT_CHANGED;
        }
        if (isRemainingMealType(mealTime, now) && hasVitalsInputChangedSince(userId, recommendedAt)) {
            return InvalidationReason.VITALS_INPUT_CHANGED;
        }
        return InvalidationReason.NONE;
    }

    private boolean isProfileChangedSinceRecommendation(HealthProfile profile, LocalDateTime recommendedAt) {
        return profile != null
                && profile.getUpdatedAt() != null
                && recommendedAt != null
                && profile.getUpdatedAt().isAfter(recommendedAt);
    }

    private boolean hasFoodInputChangedAffectingMealType(Long userId, MealTime mealTime, LocalDateTime recommendedAt) {
        List<MealTime> affectingMealTimes = getAffectingFoodMealTimes(mealTime);
        return recordReadService.hasFoodInputChangedSince(userId, affectingMealTimes, recommendedAt);
    }

    private boolean hasVitalsInputChangedSince(Long userId, LocalDateTime recommendedAt) {
        return recordReadService.hasBloodSugarInputChangedSince(userId, recommendedAt)
                || recordReadService.hasBloodPressureInputChangedSince(userId, recommendedAt);
    }

    private boolean isRemainingMealType(MealTime targetMealType, LocalDateTime now) {
        MealTime currentMealSlot = resolveCurrentMealSlot(now.toLocalTime());
        return targetMealType.ordinal() >= currentMealSlot.ordinal();
    }

    private MealTime resolveCurrentMealSlot(LocalTime now) {
        if (now.isBefore(BREAKFAST_END)) {
            return MealTime.BREAKFAST;
        }
        if (now.isBefore(LUNCH_END)) {
            return MealTime.LUNCH;
        }
        if (now.isBefore(DINNER_END)) {
            return MealTime.DINNER;
        }
        return MealTime.SNACK;
    }

    private List<MealTime> getAffectingFoodMealTimes(MealTime targetMealType) {
        return Arrays.stream(MealTime.values())
                .filter(mealTime -> mealTime.ordinal() < targetMealType.ordinal())
                .toList();
    }

    public enum InvalidationReason {
        NONE,
        EXPIRED_DATE,
        PROFILE_CHANGED,
        FOOD_INPUT_CHANGED,
        VITALS_INPUT_CHANGED
    }
}
