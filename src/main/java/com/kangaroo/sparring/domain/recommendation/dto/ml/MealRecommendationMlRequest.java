package com.kangaroo.sparring.domain.recommendation.dto.ml;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;
import java.util.Map;

public record MealRecommendationMlRequest(
        @JsonProperty("requestId") String requestId,
        @JsonProperty("contractVersion") String contractVersion,
        @JsonProperty("userId") Long userId,
        @JsonProperty("mealType") String mealType,
        @JsonProperty("requestedCount") Integer requestedCount,
        @JsonProperty("healthProfile") HealthProfile healthProfile,
        @JsonProperty("observations") Observations observations,
        @JsonProperty("nutrition") Nutrition nutrition,
        @JsonProperty("preferences") Preferences preferences,
        @JsonProperty("recentFoods") List<String> recentFoods,
        @JsonProperty("refreshNonce") String refreshNonce
) {
    public record HealthProfile(
            @JsonProperty("sex") String sex,
            @JsonProperty("age") int age,
            @JsonProperty("height") double height,
            @JsonProperty("weight") double weight,
            @JsonProperty("activityLevel") String activityLevel,
            @JsonProperty("hasHypertension") boolean hasHypertension,
            @JsonProperty("hasDyslipidemia") boolean hasDyslipidemia
    ) {}

    public record Observations(
            @JsonProperty("glucose") Glucose glucose,
            @JsonProperty("bloodPressure") BloodPressure bloodPressure
    ) {}

    public record Glucose(
            @JsonProperty("latest") GlucoseLatest latest,
            @JsonProperty("window") GlucoseWindow window
    ) {}

    public record GlucoseLatest(
            @JsonProperty("valueMgDl") double valueMgDl,
            @JsonProperty("measuredAt") String measuredAt,
            @JsonProperty("context") String context
    ) {}

    public record GlucoseWindow(
            @JsonProperty("from") String from,
            @JsonProperty("to") String to,
            @JsonProperty("byContext") Map<String, GlucoseWindowStats> byContext
    ) {}

    public record GlucoseWindowStats(
            @JsonProperty("avg") Double avg,
            @JsonProperty("min") Integer min,
            @JsonProperty("max") Integer max,
            @JsonProperty("count") Integer count
    ) {}

    public record BloodPressure(
            @JsonProperty("latest") BloodPressureLatest latest,
            @JsonProperty("window") BloodPressureWindow window
    ) {}

    public record BloodPressureLatest(
            @JsonProperty("systolic") int systolic,
            @JsonProperty("diastolic") int diastolic,
            @JsonProperty("measuredAt") String measuredAt,
            @JsonProperty("context") String context
    ) {}

    public record BloodPressureWindow(
            @JsonProperty("from") String from,
            @JsonProperty("to") String to,
            @JsonProperty("byContext") Map<String, BloodPressureWindowStats> byContext
    ) {}

    public record BloodPressureWindowStats(
            @JsonProperty("avgSystolic") Double avgSystolic,
            @JsonProperty("avgDiastolic") Double avgDiastolic,
            @JsonProperty("maxSystolic") Integer maxSystolic,
            @JsonProperty("maxDiastolic") Integer maxDiastolic,
            @JsonProperty("minSystolic") Integer minSystolic,
            @JsonProperty("minDiastolic") Integer minDiastolic,
            @JsonProperty("count") Integer count
    ) {}

    public record Nutrition(
            @JsonProperty("consumedToday") ConsumedToday consumedToday
    ) {}

    public record ConsumedToday(
            @JsonProperty("kcal") Double kcal,
            @JsonProperty("carbs") Double carbs,
            @JsonProperty("protein") Double protein,
            @JsonProperty("fat") Double fat,
            @JsonProperty("fiber") Double fiber,
            @JsonProperty("sodium") Double sodium
    ) {}

    public record Preferences(
            @JsonProperty("likes") List<String> likes,
            @JsonProperty("dislikes") List<String> dislikes,
            @JsonProperty("allergies") List<String> allergies,
            @JsonProperty("preferredCuisines") List<String> preferredCuisines
    ) {}
}
