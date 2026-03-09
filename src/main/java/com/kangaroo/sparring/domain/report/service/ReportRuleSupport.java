package com.kangaroo.sparring.domain.report.service;

import com.kangaroo.sparring.domain.exercise.log.entity.ExerciseLog;
import com.kangaroo.sparring.domain.food.log.entity.FoodLog;
import com.kangaroo.sparring.domain.common.health.HealthThresholds;
import com.kangaroo.sparring.domain.common.health.HealthWeeklyTargets;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.report.dto.internal.DailyConditionEvidence;
import com.kangaroo.sparring.domain.report.dto.internal.HighlightEvidence;
import com.kangaroo.sparring.domain.report.dto.internal.ImprovementEvidence;
import com.kangaroo.sparring.domain.report.type.DailyConditionStatus;
import com.kangaroo.sparring.domain.report.type.HighlightType;
import com.kangaroo.sparring.domain.report.type.ImprovementCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
class ReportRuleSupport {

    private static final String[] DAY_LABELS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    BloodSugarStats calcBloodSugarStats(List<BloodSugarLog> logs) {
        BloodSugarStats stats = new BloodSugarStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();
        stats.overallAvg = logs.stream().mapToInt(BloodSugarLog::getGlucoseLevel).average().orElse(0);
        stats.normalCount = (int) logs.stream().filter(this::isBloodSugarNormal).count();

        stats.afterMealByTimeSlot = logs.stream()
                .filter(l -> l.getMeasurementLabel().contains("식후"))
                .collect(Collectors.groupingBy(this::toMealTimeSlotLabel));

        return stats;
    }

    BloodPressureStats calcBloodPressureStats(List<BloodPressureLog> logs) {
        BloodPressureStats stats = new BloodPressureStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();
        stats.systolicAvg = logs.stream().mapToInt(BloodPressureLog::getSystolic).average().orElse(0);
        stats.diastolicAvg = logs.stream().mapToInt(BloodPressureLog::getDiastolic).average().orElse(0);
        stats.normalCount = (int) logs.stream().filter(this::isBloodPressureNormal).count();
        stats.hypertensionCount = (int) countHypertensionLogs(logs);
        return stats;
    }

    MealStats calcMealStats(List<FoodLog> logs) {
        MealStats stats = new MealStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();

        List<FoodLog> withCalories = logs.stream().filter(l -> l.getCalories() != null).toList();
        double totalCalories = withCalories.stream().mapToDouble(FoodLog::getCalories).sum();
        stats.avgCaloriesPerDay = totalCalories / HealthWeeklyTargets.DAYS_PER_WEEK;

        Map<LocalDate, Long> mealsPerDay = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getEatenAt().toLocalDate(), Collectors.counting()));
        stats.fullMealDays = (int) mealsPerDay.values().stream().filter(c -> c >= 3).count();

        return stats;
    }

    ExerciseStats calcExerciseStats(List<ExerciseLog> logs) {
        ExerciseStats stats = new ExerciseStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();
        stats.activeDays = (int) logs.stream().map(l -> l.getLoggedAt().toLocalDate()).distinct().count();
        return stats;
    }

    int calcHealthManagementScore(BloodSugarStats bs, BloodPressureStats bp) {
        List<Double> scores = new ArrayList<>();
        if (bs.totalCount > 0) scores.add((double) bs.normalCount / bs.totalCount * 100);
        if (bp.totalCount > 0) scores.add((double) bp.normalCount / bp.totalCount * 100);
        if (scores.isEmpty()) return 0;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    int calcMeasurementConsistencyScore(int bsCount, int bpCount) {
        List<Double> scores = new ArrayList<>();
        if (bsCount > 0) {
            scores.add(Math.min((double) bsCount / HealthWeeklyTargets.BLOOD_SUGAR_MEASUREMENTS * 100, 100));
        }
        if (bpCount > 0) {
            scores.add(Math.min((double) bpCount / HealthWeeklyTargets.BLOOD_PRESSURE_MEASUREMENTS * 100, 100));
        }
        if (scores.isEmpty()) return 0;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    int calcLifestyleScore(MealStats meal, ExerciseStats exercise) {
        List<Double> scores = new ArrayList<>();
        if (meal.totalCount > 0) {
            scores.add((double) meal.fullMealDays / HealthWeeklyTargets.FULL_MEAL_DAYS * 100);
        }
        if (exercise.totalCount > 0) {
            scores.add(Math.min((double) exercise.activeDays / HealthWeeklyTargets.EXERCISE_ACTIVE_DAYS * 100, 100));
        }
        if (scores.isEmpty()) return 0;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    int calcOverallScore(int healthScore, int consistencyScore, int lifestyleScore,
                         boolean noMeasurement, boolean noLifestyle) {
        if (noMeasurement && noLifestyle) return 0;
        if (noMeasurement) return lifestyleScore;
        if (noLifestyle) return (int) (healthScore * 0.57 + consistencyScore * 0.43);
        return (int) (healthScore * 0.4 + consistencyScore * 0.3 + lifestyleScore * 0.3);
    }

    List<DailyConditionEvidence> buildDailyConditions(
            LocalDate monday,
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<FoodLog> mealLogs,
            List<ExerciseLog> exerciseLogs
    ) {
        ReportWeekDataIndex weekDataIndex = ReportWeekDataIndex.from(bsLogs, bpLogs, mealLogs, exerciseLogs);
        List<DailyConditionEvidence> result = new ArrayList<>();

        for (int i = 0; i < HealthWeeklyTargets.DAYS_PER_WEEK; i++) {
            LocalDate date = monday.plusDays(i);
            List<Double> dayScores = new ArrayList<>();

            List<BloodSugarLog> dayBs = weekDataIndex.bloodSugarByDate().getOrDefault(date, List.of());
            if (!dayBs.isEmpty()) {
                long normalCount = dayBs.stream().filter(this::isBloodSugarNormal).count();
                dayScores.add((double) normalCount / dayBs.size() * 100);
            }

            List<BloodPressureLog> dayBp = weekDataIndex.bloodPressureByDate().getOrDefault(date, List.of());
            if (!dayBp.isEmpty()) {
                long normalCount = dayBp.stream()
                        .filter(this::isBloodPressureNormal)
                        .count();
                dayScores.add((double) normalCount / dayBp.size() * 100);
            }

            long mealCount = weekDataIndex.mealByDate().getOrDefault(date, List.of()).size();
            if (mealCount > 0) dayScores.add(mealCount >= 3 ? 100.0 : 0.0);

            boolean hasExercise = !weekDataIndex.exerciseByDate().getOrDefault(date, List.of()).isEmpty();

            DailyConditionStatus status;
            double dayScore;
            if (dayScores.isEmpty() && !hasExercise) {
                status = DailyConditionStatus.NO_DATA;
                dayScore = 0;
            } else {
                dayScore = dayScores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
                if (hasExercise) dayScore = Math.min(100, dayScore + 10);
                if (dayScore >= 75) status = DailyConditionStatus.GOOD;
                else if (dayScore >= 50) status = DailyConditionStatus.CAUTION;
                else status = DailyConditionStatus.BAD;
            }

            result.add(new DailyConditionEvidence(DAY_LABELS[i], status, dayScore));
        }

        return result;
    }

    List<HighlightEvidence> buildHighlights(
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<FoodLog> mealLogs,
            List<ExerciseLog> exerciseLogs,
            LocalDate monday
    ) {
        List<HighlightEvidence> goods = new ArrayList<>();
        List<HighlightEvidence> warnings = new ArrayList<>();

        addGoodHighlights(goods, bsLogs, bpLogs, mealLogs, exerciseLogs, monday);
        addWarningHighlights(warnings, bsLogs, bpLogs, mealLogs, exerciseLogs, monday);

        List<HighlightEvidence> result = new ArrayList<>();
        result.addAll(goods.stream().limit(2).toList());
        result.addAll(warnings.stream().limit(2).toList());
        return result;
    }

    private void addGoodHighlights(
            List<HighlightEvidence> goods,
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<FoodLog> mealLogs,
            List<ExerciseLog> exerciseLogs,
            LocalDate monday
    ) {
        addBloodPressureStreakGood(goods, bpLogs, monday);
        addBloodSugarStreakGood(goods, bsLogs, monday);
        addExerciseGoalGood(goods, exerciseLogs);
        addMealAttendanceGood(goods, mealLogs);
        addMeasurementGoalGood(goods, bsLogs, bpLogs);
    }

    private void addWarningHighlights(
            List<HighlightEvidence> warnings,
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<FoodLog> mealLogs,
            List<ExerciseLog> exerciseLogs,
            LocalDate monday
    ) {
        addDailyConditionWarning(warnings, bsLogs, bpLogs, mealLogs, exerciseLogs, monday);
        addAfterMealHighWarning(warnings, bsLogs);
        addBloodPressureHighWarning(warnings, bpLogs);
        addLifestyleMissingWarning(warnings, mealLogs, exerciseLogs);
    }

    private void addBloodPressureStreakGood(
            List<HighlightEvidence> goods,
            List<BloodPressureLog> bpLogs,
            LocalDate monday
    ) {
        if (bpLogs.isEmpty()) return;
        int streak = calcBpNormalStreak(bpLogs, monday);
        if (streak < 3) return;

        goods.add(new HighlightEvidence(
                HighlightType.GOOD,
                "BP_NORMAL_STREAK",
                "혈압 " + streak + "일 연속 정상범위 유지",
                Map.of("days", streak)
        ));
    }

    private void addBloodSugarStreakGood(
            List<HighlightEvidence> goods,
            List<BloodSugarLog> bsLogs,
            LocalDate monday
    ) {
        if (bsLogs.isEmpty()) return;
        int streak = calcBsNormalStreak(bsLogs, monday);
        if (streak < 3) return;

        goods.add(new HighlightEvidence(
                HighlightType.GOOD,
                "BS_NORMAL_STREAK",
                "혈당 " + streak + "일 연속 정상범위 유지",
                Map.of("days", streak)
        ));
    }

    private void addExerciseGoalGood(List<HighlightEvidence> goods, List<ExerciseLog> exerciseLogs) {
        long activeDays = exerciseLogs.stream().map(l -> l.getLoggedAt().toLocalDate()).distinct().count();
        if (activeDays < HealthWeeklyTargets.EXERCISE_ACTIVE_DAYS) return;

        goods.add(new HighlightEvidence(
                HighlightType.GOOD,
                "EXERCISE_GOAL",
                "운동 목표 달성 (" + activeDays + "/" + HealthWeeklyTargets.EXERCISE_ACTIVE_DAYS + "회)",
                Map.of("activeDays", activeDays, "target", HealthWeeklyTargets.EXERCISE_ACTIVE_DAYS)
        ));
    }

    private void addMealAttendanceGood(List<HighlightEvidence> goods, List<FoodLog> mealLogs) {
        int fullMealDays = calcMealStats(mealLogs).fullMealDays;
        if (fullMealDays != HealthWeeklyTargets.FULL_MEAL_DAYS) return;

        goods.add(new HighlightEvidence(
                HighlightType.GOOD,
                "MEAL_ATTENDANCE",
                "식사 기록 7일 개근",
                Map.of("days", fullMealDays)
        ));
    }

    private void addMeasurementGoalGood(
            List<HighlightEvidence> goods,
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs
    ) {
        if (bsLogs.size() < HealthWeeklyTargets.BLOOD_SUGAR_MEASUREMENTS
                || bpLogs.size() < HealthWeeklyTargets.BLOOD_PRESSURE_MEASUREMENTS) {
            return;
        }

        goods.add(new HighlightEvidence(
                HighlightType.GOOD,
                "MEASUREMENT_GOAL",
                "측정 목표 100% 달성",
                Map.of("bloodSugar", bsLogs.size(), "bloodPressure", bpLogs.size())
        ));
    }

    private void addDailyConditionWarning(
            List<HighlightEvidence> warnings,
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<FoodLog> mealLogs,
            List<ExerciseLog> exerciseLogs,
            LocalDate monday
    ) {
        List<DailyConditionEvidence> conditions = buildDailyConditions(monday, bsLogs, bpLogs, mealLogs, exerciseLogs);
        conditions.stream()
                .filter(c -> c.status() == DailyConditionStatus.BAD)
                .findFirst()
                .ifPresent(c -> warnings.add(new HighlightEvidence(
                        HighlightType.WARNING,
                        "DAILY_BAD",
                        toDayKorean(c.dayOfWeek()) + "요일 전반적 컨디션 저조",
                        Map.of("dayOfWeek", c.dayOfWeek())
                )));
    }

    private void addAfterMealHighWarning(List<HighlightEvidence> warnings, List<BloodSugarLog> bsLogs) {
        if (bsLogs.isEmpty()) return;
        Map<String, List<BloodSugarLog>> afterMealBySlot = bsLogs.stream()
                .filter(l -> l.getMeasurementLabel().contains("식후"))
                .collect(Collectors.groupingBy(this::toMealTimeSlotLabel));

        afterMealBySlot.entrySet().stream()
                .max(Comparator.comparingDouble(e -> calcAfterMealHighRatio(e.getValue())))
                .ifPresent(e -> {
                    double ratio = calcAfterMealHighRatio(e.getValue());
                    if (ratio < 0.5) return;
                    warnings.add(new HighlightEvidence(
                            HighlightType.WARNING,
                            "AFTER_MEAL_HIGH",
                            e.getKey() + " 혈당 지속 초과",
                            Map.of("slot", e.getKey(), "ratio", ratio)
                    ));
                });
    }

    private void addBloodPressureHighWarning(List<HighlightEvidence> warnings, List<BloodPressureLog> bpLogs) {
        if (bpLogs.isEmpty()) return;
        long highCount = countHypertensionLogs(bpLogs);
        if ((double) highCount / bpLogs.size() < 0.5) return;

        warnings.add(new HighlightEvidence(
                HighlightType.WARNING,
                "BP_HIGH_FREQUENT",
                "혈압 고혈압 범위 초과 잦음",
                Map.of("highCount", highCount, "total", bpLogs.size())
        ));
    }

    private void addLifestyleMissingWarning(
            List<HighlightEvidence> warnings,
            List<FoodLog> mealLogs,
            List<ExerciseLog> exerciseLogs
    ) {
        if (!mealLogs.isEmpty() && !exerciseLogs.isEmpty()) return;
        String message = mealLogs.isEmpty() && exerciseLogs.isEmpty()
                ? "식사/운동 기록 없음"
                : mealLogs.isEmpty() ? "식사 기록 없음" : "운동 기록 없음";
        warnings.add(new HighlightEvidence(
                HighlightType.WARNING,
                "LIFESTYLE_MISSING",
                message,
                Map.of("mealMissing", mealLogs.isEmpty(), "exerciseMissing", exerciseLogs.isEmpty())
        ));
    }

    ImprovementEvidence selectImprovementArea(
            BloodSugarStats bs,
            BloodPressureStats bp,
            MealStats meal,
            ExerciseStats exercise
    ) {
        Map<ImprovementCategory, Double> scores = new LinkedHashMap<>();
        addImprovementScore(scores, ImprovementCategory.BLOOD_SUGAR, bs.totalCount,
                (double) bs.normalCount / bs.totalCount * 100);
        addImprovementScore(scores, ImprovementCategory.BLOOD_PRESSURE, bp.totalCount,
                (double) bp.normalCount / bp.totalCount * 100);
        addImprovementScore(scores, ImprovementCategory.MEAL, meal.totalCount,
                (double) meal.fullMealDays / HealthWeeklyTargets.FULL_MEAL_DAYS * 100);
        addImprovementScore(scores, ImprovementCategory.EXERCISE, exercise.totalCount,
                Math.min((double) exercise.activeDays / HealthWeeklyTargets.EXERCISE_ACTIVE_DAYS * 100, 100));

        if (scores.isEmpty() || scores.values().stream().allMatch(s -> s >= 80)) return null;

        ImprovementCategory worst = scores.entrySet().stream()
                .min(Map.Entry.comparingByValue())
                .map(Map.Entry::getKey)
                .orElse(null);
        if (worst == null) return null;

        return switch (worst) {
            case BLOOD_SUGAR -> buildBloodSugarImprovement(bs);
            case BLOOD_PRESSURE -> buildBloodPressureImprovement(bp);
            case MEAL -> buildMealImprovement(meal);
            case EXERCISE -> buildExerciseImprovement(exercise);
        };
    }

    private ImprovementEvidence buildBloodSugarImprovement(BloodSugarStats bs) {
        String worstSlot = "식후 혈당";
        String detail = String.format("%d번 중 %d번 높음, 평균 %.0f (목표 %d)",
                bs.totalCount, bs.totalCount - bs.normalCount, bs.overallAvg,
                HealthThresholds.BLOOD_SUGAR_POST_MEAL_NORMAL_MAX + 1);
        long highCount = bs.totalCount - bs.normalCount;
        int total = bs.totalCount;
        double avg = bs.overallAvg;

        if (!bs.afterMealByTimeSlot.isEmpty()) {
            String worst = bs.afterMealByTimeSlot.entrySet().stream()
                    .sorted((a, b) -> {
                        double ratioA = calcAfterMealHighRatio(a.getValue());
                        double ratioB = calcAfterMealHighRatio(b.getValue());
                        int ratioCompare = Double.compare(ratioB, ratioA);
                        if (ratioCompare != 0) return ratioCompare;
                        double avgA = a.getValue().stream().mapToInt(BloodSugarLog::getGlucoseLevel).average().orElse(0);
                        double avgB = b.getValue().stream().mapToInt(BloodSugarLog::getGlucoseLevel).average().orElse(0);
                        return Double.compare(avgB, avgA);
                    })
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("식후 혈당");
            worstSlot = worst;

            List<BloodSugarLog> slotLogs = bs.afterMealByTimeSlot.get(worst);
            if (slotLogs != null && !slotLogs.isEmpty()) {
                highCount = slotLogs.stream().filter(this::isPostMealBloodSugarHigh).count();
                avg = slotLogs.stream().mapToInt(BloodSugarLog::getGlucoseLevel).average().orElse(0);
                total = slotLogs.size();
                detail = String.format("%d번 중 %d번 높음, 평균 %.0f (목표 %d)",
                        total, highCount, avg, HealthThresholds.BLOOD_SUGAR_POST_MEAL_NORMAL_MAX + 1);
            }
        }

        return new ImprovementEvidence(
                ImprovementCategory.BLOOD_SUGAR,
                worstSlot,
                detail,
                Map.of("highCount", highCount, "total", total, "avg", avg,
                        "target", HealthThresholds.BLOOD_SUGAR_POST_MEAL_NORMAL_MAX + 1)
        );
    }

    private ImprovementEvidence buildBloodPressureImprovement(BloodPressureStats bp) {
        int abnormalCount = bp.hypertensionCount;
        String detail = String.format("%d번 중 %d번 고혈압 범위 초과, 평균 수축기 %.0f",
                bp.totalCount, abnormalCount, bp.systolicAvg);
        return new ImprovementEvidence(
                ImprovementCategory.BLOOD_PRESSURE,
                "혈압 관리",
                detail,
                Map.of("highCount", abnormalCount, "total", bp.totalCount, "avgSystolic", bp.systolicAvg)
        );
    }

    private ImprovementEvidence buildMealImprovement(MealStats meal) {
        String detail = String.format("이번 주 %d일만 3끼 기록 (목표 7일)", meal.fullMealDays);
        return new ImprovementEvidence(
                ImprovementCategory.MEAL,
                "식사 불규칙",
                detail,
                Map.of("fullMealDays", meal.fullMealDays, "target", 7)
        );
    }

    private ImprovementEvidence buildExerciseImprovement(ExerciseStats exercise) {
        String detail = String.format("이번 주 %d회 운동 (목표 %d회)",
                exercise.activeDays, HealthWeeklyTargets.EXERCISE_ACTIVE_DAYS);
        return new ImprovementEvidence(
                ImprovementCategory.EXERCISE,
                "운동 부족",
                detail,
                Map.of("activeDays", exercise.activeDays, "target", HealthWeeklyTargets.EXERCISE_ACTIVE_DAYS)
        );
    }

    private int calcBpNormalStreak(List<BloodPressureLog> bpLogs, LocalDate monday) {
        Map<LocalDate, List<BloodPressureLog>> bpByDate = bpLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getMeasuredAt().toLocalDate()));
        int streak = 0;
        int maxStreak = 0;
        for (int i = 0; i < HealthWeeklyTargets.DAYS_PER_WEEK; i++) {
            LocalDate date = monday.plusDays(i);
            List<BloodPressureLog> dayLogs = bpByDate.getOrDefault(date, List.of());
            if (dayLogs.isEmpty()) {
                streak = 0;
                continue;
            }
            boolean allNormal = dayLogs.stream().allMatch(this::isBloodPressureNormal);
            if (allNormal) {
                streak++;
                maxStreak = Math.max(maxStreak, streak);
            } else {
                streak = 0;
            }
        }
        return maxStreak;
    }

    private int calcBsNormalStreak(List<BloodSugarLog> bsLogs, LocalDate monday) {
        Map<LocalDate, List<BloodSugarLog>> bsByDate = bsLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getMeasurementTime().toLocalDate()));
        int streak = 0;
        int maxStreak = 0;
        for (int i = 0; i < HealthWeeklyTargets.DAYS_PER_WEEK; i++) {
            LocalDate date = monday.plusDays(i);
            List<BloodSugarLog> dayLogs = bsByDate.getOrDefault(date, List.of());
            if (dayLogs.isEmpty()) {
                streak = 0;
                continue;
            }
            boolean allNormal = dayLogs.stream().allMatch(this::isBloodSugarNormal);
            if (allNormal) {
                streak++;
                maxStreak = Math.max(maxStreak, streak);
            } else {
                streak = 0;
            }
        }
        return maxStreak;
    }

    private boolean isBloodSugarNormal(BloodSugarLog log) {
        int g = log.getGlucoseLevel();
        String label = log.getMeasurementLabel();
        if (label.contains("공복") || label.contains("식전")) {
            return g <= HealthThresholds.BLOOD_SUGAR_FASTING_NORMAL_MAX;
        }
        return g <= HealthThresholds.BLOOD_SUGAR_POST_MEAL_NORMAL_MAX;
    }

    private String toMealTimeSlotLabel(BloodSugarLog log) {
        int hour = log.getMeasurementTime().getHour();
        if (hour >= 6 && hour < 11) return "아침 식후";
        if (hour >= 11 && hour < 15) return "점심 식후";
        if (hour >= 17 && hour < 22) return "저녁 식후";
        return "기타 식후";
    }

    private double calcAfterMealHighRatio(List<BloodSugarLog> logs) {
        if (logs.isEmpty()) return 0.0;
        long highCount = logs.stream().filter(this::isPostMealBloodSugarHigh).count();
        return (double) highCount / logs.size();
    }

    private long countHypertensionLogs(List<BloodPressureLog> logs) {
        return logs.stream()
                .filter(l -> l.getSystolic() >= HealthThresholds.BLOOD_PRESSURE_HYPERTENSION_SYSTOLIC
                        || l.getDiastolic() >= HealthThresholds.BLOOD_PRESSURE_HYPERTENSION_DIASTOLIC)
                .count();
    }

    private boolean isBloodPressureNormal(BloodPressureLog log) {
        return log.getSystolic() <= HealthThresholds.BLOOD_PRESSURE_SYSTOLIC_NORMAL_MAX
                && log.getDiastolic() <= HealthThresholds.BLOOD_PRESSURE_DIASTOLIC_NORMAL_MAX;
    }

    private boolean isPostMealBloodSugarHigh(BloodSugarLog log) {
        return log.getGlucoseLevel() > HealthThresholds.BLOOD_SUGAR_POST_MEAL_NORMAL_MAX;
    }

    private String toDayKorean(String dayLabel) {
        return switch (dayLabel) {
            case "MON" -> "월";
            case "TUE" -> "화";
            case "WED" -> "수";
            case "THU" -> "목";
            case "FRI" -> "금";
            case "SAT" -> "토";
            case "SUN" -> "일";
            default -> dayLabel;
        };
    }

    private void addImprovementScore(
            Map<ImprovementCategory, Double> scores,
            ImprovementCategory category,
            int dataCount,
            double score
    ) {
        if (dataCount > 0) {
            scores.put(category, score);
        }
    }

    static class BloodSugarStats {
        int totalCount = 0;
        int normalCount = 0;
        double overallAvg = 0;
        Map<String, List<BloodSugarLog>> afterMealByTimeSlot = new HashMap<>();
    }

    static class BloodPressureStats {
        int totalCount = 0;
        int normalCount = 0;
        int hypertensionCount = 0;
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
