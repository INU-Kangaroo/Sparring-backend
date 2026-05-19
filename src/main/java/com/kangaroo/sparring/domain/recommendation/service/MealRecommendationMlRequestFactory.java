package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.recommendation.dto.ml.MealRecommendationMlRequest;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.FoodRecord;
import com.kangaroo.sparring.domain.survey.type.BloodPressureStatus;
import com.kangaroo.sparring.domain.survey.type.ExerciseFrequency;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class MealRecommendationMlRequestFactory {

    private static final String CONTRACT_VERSION = "rec-v4";

    private final Clock kstClock;
    private final ObjectMapper objectMapper;

    public MealRecommendationMlRequest create(
            User user,
            HealthProfile profile,
            MealTime mealTime,
            List<BloodSugarRecord> bloodSugarRecords,
            List<BloodPressureRecord> bloodPressureRecords,
            List<FoodRecord> recentFoods
    ) {
        return create(user, profile, mealTime, bloodSugarRecords, bloodPressureRecords, recentFoods, false);
    }

    public MealRecommendationMlRequest create(
            User user,
            HealthProfile profile,
            MealTime mealTime,
            List<BloodSugarRecord> bloodSugarRecords,
            List<BloodPressureRecord> bloodPressureRecords,
            List<FoodRecord> recentFoods,
            boolean forceRefreshNonce
    ) {
        MealRecommendationMlRequest.HealthProfile healthProfile = new MealRecommendationMlRequest.HealthProfile(
                resolveSex(profile.getGender()),
                resolveAge(profile),
                profile.getHeight() != null ? profile.getHeight().doubleValue() : 170.0,
                profile.getWeight() != null ? profile.getWeight().doubleValue() : 65.0,
                resolveActivityLevel(profile.getExerciseFrequency()),
                resolveBloodPressureStatus(profile.getBloodPressureStatus()) != BloodPressureStatus.NORMAL,
                false
        );

        MealRecommendationMlRequest.Observations observations = buildObservations(bloodSugarRecords, bloodPressureRecords);
        MealRecommendationMlRequest.Nutrition nutrition = new MealRecommendationMlRequest.Nutrition(
                buildConsumedToday(recentFoods)
        );
        MealRecommendationMlRequest.Preferences preferences = buildPreferences(profile);

        List<String> recentFoodNames = recentFoods.stream()
                .map(FoodRecord::getFoodName)
                .filter(name -> name != null && !name.isBlank())
                .distinct()
                .limit(10)
                .toList();

        return new MealRecommendationMlRequest(
                buildRequestId(user.getId(), mealTime),
                CONTRACT_VERSION,
                user.getId(),
                mealTime.name(),
                resolveRequestedCount(mealTime),
                healthProfile,
                observations,
                nutrition,
                preferences,
                recentFoodNames,
                forceRefreshNonce ? buildRefreshNonce(user.getId(), mealTime) : null
        );
    }

    private MealRecommendationMlRequest.Observations buildObservations(
            List<BloodSugarRecord> bloodSugarRecords,
            List<BloodPressureRecord> bloodPressureRecords
    ) {
        MealRecommendationMlRequest.Glucose glucose = buildGlucoseObservation(bloodSugarRecords);
        MealRecommendationMlRequest.BloodPressure bloodPressure = buildBloodPressureObservation(bloodPressureRecords);
        if (glucose == null && bloodPressure == null) {
            return null;
        }
        return new MealRecommendationMlRequest.Observations(glucose, bloodPressure);
    }

    private MealRecommendationMlRequest.Glucose buildGlucoseObservation(List<BloodSugarRecord> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }

        BloodSugarRecord latestRecord = records.get(0);
        MealRecommendationMlRequest.GlucoseLatest latest = new MealRecommendationMlRequest.GlucoseLatest(
                latestRecord.getGlucoseLevel(),
                formatDateTime(latestRecord.getMeasurementTime()),
                resolveGlucoseContext(latestRecord.getMeasurementLabel())
        );

        Map<String, List<Integer>> grouped = new HashMap<>();
        LocalDateTime from = null;
        LocalDateTime to = null;
        for (BloodSugarRecord record : records) {
            if (record.getMeasurementTime() == null || record.getGlucoseLevel() == null) {
                continue;
            }
            String context = resolveGlucoseContext(record.getMeasurementLabel());
            grouped.computeIfAbsent(context, k -> new java.util.ArrayList<>()).add(record.getGlucoseLevel());
            if (from == null || record.getMeasurementTime().isBefore(from)) {
                from = record.getMeasurementTime();
            }
            if (to == null || record.getMeasurementTime().isAfter(to)) {
                to = record.getMeasurementTime();
            }
        }

        Map<String, MealRecommendationMlRequest.GlucoseWindowStats> byContext = new HashMap<>();
        grouped.forEach((context, values) -> {
            if (values.isEmpty()) {
                return;
            }
            int min = values.stream().min(Integer::compareTo).orElse(0);
            int max = values.stream().max(Integer::compareTo).orElse(0);
            double avg = values.stream().mapToInt(Integer::intValue).average().orElse(0d);
            byContext.put(context, new MealRecommendationMlRequest.GlucoseWindowStats(avg, min, max, values.size()));
        });

        MealRecommendationMlRequest.GlucoseWindow window = null;
        if (from != null && to != null && !byContext.isEmpty()) {
            window = new MealRecommendationMlRequest.GlucoseWindow(
                    formatDateTime(from),
                    formatDateTime(to),
                    byContext
            );
        }

        return new MealRecommendationMlRequest.Glucose(latest, window);
    }

    private MealRecommendationMlRequest.BloodPressure buildBloodPressureObservation(List<BloodPressureRecord> records) {
        if (records == null || records.isEmpty()) {
            return null;
        }

        BloodPressureRecord latestRecord = records.get(0);
        MealRecommendationMlRequest.BloodPressureLatest latest = new MealRecommendationMlRequest.BloodPressureLatest(
                latestRecord.getSystolic(),
                latestRecord.getDiastolic(),
                formatDateTime(latestRecord.getMeasuredAt()),
                resolveBloodPressureContext(latestRecord)
        );

        Map<String, List<BloodPressureRecord>> grouped = new HashMap<>();
        LocalDateTime from = null;
        LocalDateTime to = null;

        for (BloodPressureRecord record : records) {
            if (record.getMeasuredAt() == null || record.getSystolic() == null || record.getDiastolic() == null) {
                continue;
            }
            String context = resolveBloodPressureContext(record);
            grouped.computeIfAbsent(context, k -> new java.util.ArrayList<>()).add(record);
            if (from == null || record.getMeasuredAt().isBefore(from)) {
                from = record.getMeasuredAt();
            }
            if (to == null || record.getMeasuredAt().isAfter(to)) {
                to = record.getMeasuredAt();
            }
        }

        Map<String, MealRecommendationMlRequest.BloodPressureWindowStats> byContext = new HashMap<>();
        grouped.forEach((context, values) -> {
            if (values.isEmpty()) {
                return;
            }
            List<Integer> systolicValues = values.stream().map(BloodPressureRecord::getSystolic).toList();
            List<Integer> diastolicValues = values.stream().map(BloodPressureRecord::getDiastolic).toList();

            int minSystolic = systolicValues.stream().min(Integer::compareTo).orElse(0);
            int maxSystolic = systolicValues.stream().max(Integer::compareTo).orElse(0);
            int minDiastolic = diastolicValues.stream().min(Integer::compareTo).orElse(0);
            int maxDiastolic = diastolicValues.stream().max(Integer::compareTo).orElse(0);
            double avgSystolic = systolicValues.stream().mapToInt(Integer::intValue).average().orElse(0d);
            double avgDiastolic = diastolicValues.stream().mapToInt(Integer::intValue).average().orElse(0d);

            byContext.put(context, new MealRecommendationMlRequest.BloodPressureWindowStats(
                    avgSystolic,
                    avgDiastolic,
                    maxSystolic,
                    maxDiastolic,
                    minSystolic,
                    minDiastolic,
                    values.size()
            ));
        });

        MealRecommendationMlRequest.BloodPressureWindow window = null;
        if (from != null && to != null && !byContext.isEmpty()) {
            window = new MealRecommendationMlRequest.BloodPressureWindow(
                    formatDateTime(from),
                    formatDateTime(to),
                    byContext
            );
        }

        return new MealRecommendationMlRequest.BloodPressure(latest, window);
    }

    private MealRecommendationMlRequest.ConsumedToday buildConsumedToday(List<FoodRecord> recentFoods) {
        LocalDate today = LocalDate.now(kstClock);
        double kcal = 0d;
        double carbs = 0d;
        double protein = 0d;
        double fat = 0d;
        double fiber = 0d;
        double sodium = 0d;

        for (FoodRecord food : recentFoods) {
            if (food.getEatenAt() == null || !food.getEatenAt().toLocalDate().isEqual(today)) {
                continue;
            }
            kcal += nvl(food.getCalories());
            carbs += nvl(food.getCarbs());
            protein += nvl(food.getProtein());
            fat += nvl(food.getFat());
            fiber += nvl(food.getFiber());
            sodium += nvl(food.getSodium());
        }

        return new MealRecommendationMlRequest.ConsumedToday(kcal, carbs, protein, fat, fiber, sodium);
    }

    private MealRecommendationMlRequest.Preferences buildPreferences(HealthProfile profile) {
        List<String> allergies = parseJsonArray(profile.getAllergies());
        return new MealRecommendationMlRequest.Preferences(
                List.of(),
                List.of(),
                allergies,
                List.of()
        );
    }

    private List<String> parseJsonArray(String json) {
        if (json == null || json.isBlank()) {
            return List.of();
        }
        try {
            List<String> values = objectMapper.readValue(json, new TypeReference<>() {});
            return values == null ? List.of() : values;
        } catch (Exception ignored) {
            return List.of();
        }
    }

    private int resolveRequestedCount(MealTime mealTime) {
        return mealTime == MealTime.SNACK ? 3 : 4;
    }

    private String buildRequestId(Long userId, MealTime mealTime) {
        return "req_" + userId + "_" + mealTime.name() + "_" + System.currentTimeMillis();
    }

    private String buildRefreshNonce(Long userId, MealTime mealTime) {
        return "refresh_" + userId + "_" + mealTime.name() + "_" + UUID.randomUUID();
    }

    private String formatDateTime(LocalDateTime dateTime) {
        if (dateTime == null) {
            return null;
        }
        return dateTime.atZone(kstClock.getZone()).toOffsetDateTime().toString();
    }

    private double nvl(Double value) {
        return value == null ? 0d : value;
    }

    private String resolveGlucoseContext(String measurementLabel) {
        if (measurementLabel == null || measurementLabel.isBlank()) {
            return "PRE_MEAL";
        }
        String normalized = measurementLabel.toUpperCase();
        if (normalized.contains("공복") || normalized.contains("FAST")) {
            return "FASTING";
        }
        if (normalized.contains("식후") || normalized.contains("POST")) {
            return "POST_MEAL_2H";
        }
        if (normalized.contains("식전") || normalized.contains("PRE")) {
            return "PRE_MEAL";
        }
        return "PRE_MEAL";
    }

    private String resolveBloodPressureContext(BloodPressureRecord record) {
        if (record.getMeasurementLabel() != null && !record.getMeasurementLabel().isBlank()) {
            String normalized = record.getMeasurementLabel().toUpperCase();
            if (normalized.contains("아침") || normalized.contains("MORNING")) {
                return "MORNING";
            }
            if (normalized.contains("취침") || normalized.contains("BED") || normalized.contains("NIGHT")) {
                return "BEDTIME";
            }
        }
        if (record.getMeasuredAt() != null) {
            int hour = record.getMeasuredAt().getHour();
            if (hour >= 5 && hour < 12) {
                return "MORNING";
            }
            if (hour >= 21 || hour < 3) {
                return "BEDTIME";
            }
        }
        return "DAYTIME";
    }

    private String resolveActivityLevel(ExerciseFrequency freq) {
        if (freq == null) return "SEDENTARY";
        return switch (freq) {
            case ZERO -> "SEDENTARY";
            case ONE_TO_TWO -> "LIGHT";
            case TWO_TO_THREE, THREE_TO_FOUR -> "MODERATE";
            case FOUR_TO_FIVE, DAILY -> "ACTIVE";
        };
    }

    private String resolveSex(Gender gender) {
        if (gender == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "성별 정보가 필요합니다.");
        }
        return gender == Gender.MALE ? "MALE" : "FEMALE";
    }

    private BloodPressureStatus resolveBloodPressureStatus(BloodPressureStatus status) {
        return status == null ? BloodPressureStatus.NORMAL : status;
    }

    private int resolveAge(HealthProfile profile) {
        if (profile.getBirthDate() == null) return 30;
        return Math.max(0, Period.between(profile.getBirthDate(), LocalDate.now(kstClock)).getYears());
    }
}
