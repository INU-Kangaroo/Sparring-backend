package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.ExerciseRecord;
import com.kangaroo.sparring.domain.record.common.FoodRecord;
import com.kangaroo.sparring.domain.insight.weekly.policy.ReportPolicyProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주간 리포트 점수 계산 전담 컴포넌트.
 * 통계 집계(Stats)와 각 점수 산출 로직을 담당한다.
 */
@Component
@RequiredArgsConstructor
class ReportScoreCalculator {

    private static final int WEEK_DAYS = 7;
    private final ReportPolicyProperties policy;

    BloodSugarStats calcBloodSugarStats(List<BloodSugarRecord> logs) {
        BloodSugarStats stats = new BloodSugarStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();
        stats.overallAvg = logs.stream().mapToInt(BloodSugarRecord::getGlucoseLevel).average().orElse(0);
        stats.normalCount = (int) logs.stream().filter(ReportHealthClassifier::isBloodSugarNormal).count();
        stats.severeHighCount = (int) logs.stream().filter(this::isBloodSugarSeverelyHigh).count();
        stats.afterMealByTimeSlot = logs.stream()
                .filter(l -> l.getMeasurementLabel().contains("식후"))
                .collect(Collectors.groupingBy(ReportHealthClassifier::toMealTimeSlotLabel));

        return stats;
    }

    BloodPressureStats calcBloodPressureStats(List<BloodPressureRecord> logs) {
        BloodPressureStats stats = new BloodPressureStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();
        stats.systolicAvg = logs.stream().mapToInt(BloodPressureRecord::getSystolic).average().orElse(0);
        stats.diastolicAvg = logs.stream().mapToInt(BloodPressureRecord::getDiastolic).average().orElse(0);
        stats.normalCount = (int) logs.stream().filter(ReportHealthClassifier::isBloodPressureNormal).count();
        stats.hypertensionCount = (int) ReportHealthClassifier.countHypertensionLogs(logs);
        stats.severeHighCount = (int) logs.stream().filter(this::isBloodPressureSeverelyHigh).count();
        return stats;
    }

    MealStats calcMealStats(List<FoodRecord> logs) {
        MealStats stats = new MealStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();
        double totalCalories = logs.stream()
                .filter(l -> l.getCalories() != null)
                .mapToDouble(FoodRecord::getCalories)
                .sum();
        Map<LocalDate, Long> mealsPerDay = logs.stream()
                .collect(Collectors.groupingBy(l -> l.occurredAt().toLocalDate(), Collectors.counting()));
        stats.avgCaloriesPerDay = mealsPerDay.isEmpty() ? 0 : totalCalories / mealsPerDay.size();
        stats.fullMealDays = (int) mealsPerDay.values().stream().filter(c -> c >= 3).count();
        return stats;
    }

    ExerciseStats calcExerciseStats(List<ExerciseRecord> logs) {
        ExerciseStats stats = new ExerciseStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();
        stats.activeDays = (int) logs.stream().map(l -> l.occurredAt().toLocalDate()).distinct().count();
        return stats;
    }

    int calcHealthManagementScore(BloodSugarStats bs, BloodPressureStats bp) {
        List<Double> scores = new ArrayList<>();
        if (bs.totalCount > 0) {
            double normalRatio = (double) bs.normalCount / bs.totalCount;
            double severeRatio = (double) bs.severeHighCount / bs.totalCount;
            scores.add(clamp(normalRatio * 100 - severeRatio * policy.getSevereRiskPenalty()));
        }
        if (bp.totalCount > 0) {
            double normalRatio = (double) bp.normalCount / bp.totalCount;
            double severeRatio = (double) bp.severeHighCount / bp.totalCount;
            scores.add(clamp(normalRatio * 100 - severeRatio * policy.getSevereRiskPenalty()));
        }
        if (scores.isEmpty()) return 0;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    int calcMeasurementConsistencyScore(int bsCount, int bpCount) {
        List<Double> scores = new ArrayList<>();
        if (bsCount > 0) {
            scores.add(Math.min((double) bsCount / policy.getBloodSugarWeeklyTarget() * 100, 100));
        }
        if (bpCount > 0) {
            scores.add(Math.min((double) bpCount / policy.getBloodPressureWeeklyTarget() * 100, 100));
        }
        if (scores.isEmpty()) return 0;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    int calcLifestyleScore(MealStats meal, ExerciseStats exercise) {
        List<Double> scores = new ArrayList<>();
        if (meal.totalCount > 0) {
            scores.add((double) meal.fullMealDays / policy.getFullMealDaysTarget() * 100);
        }
        if (exercise.totalCount > 0) {
            scores.add(Math.min((double) exercise.activeDays / policy.getExerciseActiveDaysTarget() * 100, 100));
        }
        if (scores.isEmpty()) return 0;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    int calcOverallScore(int healthScore, int consistencyScore, int lifestyleScore,
                         int stabilityScore, int trendScore,
                         boolean noMeasurement, boolean noLifestyle) {
        List<Double> weightedScores = new ArrayList<>();
        List<Double> weights = new ArrayList<>();

        if (!noMeasurement) {
            addWeighted(weightedScores, weights, healthScore, policy.getOverallHealthWeight());
            addWeighted(weightedScores, weights, consistencyScore, policy.getOverallConsistencyWeight());
            if (stabilityScore >= 0) addWeighted(weightedScores, weights, stabilityScore, policy.getOverallStabilityWeight());
            if (trendScore >= 0) addWeighted(weightedScores, weights, trendScore, policy.getOverallTrendWeight());
        }
        if (!noLifestyle) {
            addWeighted(weightedScores, weights, lifestyleScore, policy.getOverallLifestyleWeight());
        }

        double totalWeight = weights.stream().mapToDouble(Double::doubleValue).sum();
        if (totalWeight == 0) return 0;
        double weightedSum = weightedScores.stream().mapToDouble(Double::doubleValue).sum();
        return (int) (weightedSum / totalWeight);
    }

    int calcStabilityScore(List<BloodSugarRecord> bsLogs, List<BloodPressureRecord> bpLogs) {
        List<Double> scores = new ArrayList<>();

        if (bsLogs.size() >= 3) {
            scores.add(calcVariabilityScore(bsLogs.stream().map(BloodSugarRecord::getGlucoseLevel).toList()));
        }
        if (bpLogs.size() >= 3) {
            double systolic = calcVariabilityScore(bpLogs.stream().map(BloodPressureRecord::getSystolic).toList());
            double diastolic = calcVariabilityScore(bpLogs.stream().map(BloodPressureRecord::getDiastolic).toList());
            scores.add((systolic + diastolic) / 2.0);
        }

        if (scores.isEmpty()) return -1;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    int calcTrendScore(LocalDate monday, List<BloodSugarRecord> bsLogs, List<BloodPressureRecord> bpLogs) {
        List<Double> scores = new ArrayList<>();

        Double bloodSugarSlope = calcDailySlope(monday, bsLogs,
                BloodSugarRecord::getMeasurementTime, BloodSugarRecord::getGlucoseLevel);
        if (bloodSugarSlope != null) {
            scores.add(clamp(50 - bloodSugarSlope * policy.getBloodSugarTrendSlopeFactor()));
        }

        Double systolicSlope = calcDailySlope(monday, bpLogs,
                BloodPressureRecord::getMeasuredAt, BloodPressureRecord::getSystolic);
        Double diastolicSlope = calcDailySlope(monday, bpLogs,
                BloodPressureRecord::getMeasuredAt, BloodPressureRecord::getDiastolic);
        if (systolicSlope != null && diastolicSlope != null) {
            scores.add((clamp(50 - systolicSlope * policy.getSystolicTrendSlopeFactor())
                    + clamp(50 - diastolicSlope * policy.getDiastolicTrendSlopeFactor())) / 2.0);
        }

        if (scores.isEmpty()) return -1;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private boolean isBloodSugarSeverelyHigh(BloodSugarRecord log) {
        return log.getGlucoseLevel() >= policy.getSevereHighBloodSugar();
    }

    private boolean isBloodPressureSeverelyHigh(BloodPressureRecord log) {
        return log.getSystolic() >= policy.getSevereHighSystolic()
                || log.getDiastolic() >= policy.getSevereHighDiastolic();
    }

    private void addWeighted(List<Double> weightedScores, List<Double> weights, int score, double weight) {
        weightedScores.add(score * weight);
        weights.add(weight);
    }

    double clamp(double value) {
        return Math.max(0, Math.min(100, value));
    }

    private double calcVariabilityScore(List<Integer> values) {
        double mean = values.stream().mapToDouble(Integer::doubleValue).average().orElse(0);
        if (mean <= 0) return 0;
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0);
        double cv = Math.sqrt(variance) / mean;
        return clamp(100 - (cv * policy.getVariabilityPenaltyFactor()));
    }

    private <T> Double calcDailySlope(
            LocalDate monday,
            List<T> logs,
            java.util.function.Function<T, java.time.LocalDateTime> timeExtractor,
            java.util.function.ToIntFunction<T> valueExtractor
    ) {
        Map<Integer, Double> dayAverages = logs.stream()
                .collect(Collectors.groupingBy(
                        log -> (int) java.time.temporal.ChronoUnit.DAYS.between(
                                monday, timeExtractor.apply(log).toLocalDate()),
                        Collectors.averagingInt(valueExtractor)
                ));

        List<Integer> x = dayAverages.keySet().stream()
                .filter(day -> day >= 0 && day < WEEK_DAYS)
                .sorted()
                .toList();
        if (x.size() < 3) return null;

        List<Double> y = x.stream().map(dayAverages::get).toList();
        return linearRegressionSlope(x, y);
    }

    private Double linearRegressionSlope(List<Integer> x, List<Double> y) {
        int n = x.size();
        if (n < 2) return null;
        double sumX = x.stream().mapToDouble(Integer::doubleValue).sum();
        double sumY = y.stream().mapToDouble(Double::doubleValue).sum();
        double sumXY = 0;
        double sumXX = 0;
        for (int i = 0; i < n; i++) {
            sumXY += x.get(i) * y.get(i);
            sumXX += x.get(i) * x.get(i);
        }
        double denominator = (n * sumXX) - (sumX * sumX);
        if (denominator == 0) return null;
        return ((n * sumXY) - (sumX * sumY)) / denominator;
    }

    static class BloodSugarStats {
        int totalCount = 0;
        int normalCount = 0;
        int severeHighCount = 0;
        double overallAvg = 0;
        Map<String, List<BloodSugarRecord>> afterMealByTimeSlot = new HashMap<>();
    }

    static class BloodPressureStats {
        int totalCount = 0;
        int normalCount = 0;
        int hypertensionCount = 0;
        int severeHighCount = 0;
        double systolicAvg = 0;
        double diastolicAvg = 0;
    }

    static class MealStats {
        int totalCount = 0;
        int fullMealDays = 0;
        double avgCaloriesPerDay = 0;
    }

    static class ExerciseStats {
        int totalCount = 0;
        int activeDays = 0;
    }
}
