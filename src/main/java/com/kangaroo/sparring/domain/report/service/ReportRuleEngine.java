package com.kangaroo.sparring.domain.report.service;

import com.kangaroo.sparring.domain.exercise.log.entity.ExerciseLog;
import com.kangaroo.sparring.domain.meal.entity.MealLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.report.dto.internal.*;
import com.kangaroo.sparring.domain.report.type.DailyConditionStatus;
import com.kangaroo.sparring.domain.report.type.HighlightType;
import com.kangaroo.sparring.domain.report.type.ImprovementCategory;
import org.springframework.stereotype.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

@Component
public class ReportRuleEngine {

    private static final int BLOOD_SUGAR_TARGET = 21;
    private static final int BLOOD_PRESSURE_TARGET = 14;
    private static final int EXERCISE_TARGET = 5;
    private static final int BLOOD_SUGAR_AFTER_MEAL_TARGET = 140;
    private static final int BLOOD_PRESSURE_HYPERTENSION_SYSTOLIC = 140;
    private static final int BLOOD_PRESSURE_HYPERTENSION_DIASTOLIC = 90;
    private static final String[] DAY_LABELS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    public ReportEvidence evaluate(
            LocalDate monday,
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<MealLog> mealLogs,
            List<ExerciseLog> exerciseLogs
    ) {
        BloodSugarStats bs = calcBloodSugarStats(bsLogs);
        BloodPressureStats bp = calcBloodPressureStats(bpLogs);
        MealStats meal = calcMealStats(mealLogs);
        ExerciseStats exercise = calcExerciseStats(exerciseLogs);

        int healthManagement = calcHealthManagementScore(bs, bp);
        int measurementConsistency = calcMeasurementConsistencyScore(bsLogs.size(), bpLogs.size());
        int lifestyle = calcLifestyleScore(meal, exercise);
        int overall = calcOverallScore(
                healthManagement,
                measurementConsistency,
                lifestyle,
                bsLogs.isEmpty() && bpLogs.isEmpty(),
                mealLogs.isEmpty() && exerciseLogs.isEmpty()
        );

        List<DailyConditionEvidence> dailyConditions = buildDailyConditions(monday, bsLogs, bpLogs, mealLogs, exerciseLogs);
        List<HighlightEvidence> highlights = buildHighlights(bsLogs, bpLogs, mealLogs, exerciseLogs, monday);
        ImprovementEvidence improvement = selectImprovementArea(bs, bp, meal, exercise);

        CommentEvidence comment = new CommentEvidence(
                bs.totalCount > 0 ? bs.overallAvg : null,
                bp.totalCount > 0 ? bp.systolicAvg : null,
                meal.totalCount > 0 ? meal.avgCaloriesPerDay : null,
                exercise.totalCount > 0 ? exercise.totalCount : null,
                calcRecordDays(bsLogs, bpLogs, mealLogs, exerciseLogs, monday)
        );

        return new ReportEvidence(
                comment.recordDays(),
                bsLogs.size(),
                bpLogs.size(),
                new ScoreEvidence(healthManagement, measurementConsistency, lifestyle, overall),
                dailyConditions,
                highlights,
                improvement,
                comment
        );
    }

    private BloodSugarStats calcBloodSugarStats(List<BloodSugarLog> logs) {
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

    private BloodPressureStats calcBloodPressureStats(List<BloodPressureLog> logs) {
        BloodPressureStats stats = new BloodPressureStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();
        stats.systolicAvg = logs.stream().mapToInt(BloodPressureLog::getSystolic).average().orElse(0);
        stats.diastolicAvg = logs.stream().mapToInt(BloodPressureLog::getDiastolic).average().orElse(0);
        stats.normalCount = (int) logs.stream().filter(l -> l.getSystolic() < 120 && l.getDiastolic() < 80).count();
        stats.hypertensionCount = (int) countHypertensionLogs(logs);
        return stats;
    }

    private MealStats calcMealStats(List<MealLog> logs) {
        MealStats stats = new MealStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();

        List<MealLog> withCalories = logs.stream().filter(l -> l.getCalories() != null).toList();
        double totalCalories = withCalories.stream().mapToDouble(MealLog::getCalories).sum();
        stats.avgCaloriesPerDay = totalCalories / 7.0;

        Map<LocalDate, Long> mealsPerDay = logs.stream()
                .collect(Collectors.groupingBy(l -> l.getEatenAt().toLocalDate(), Collectors.counting()));
        stats.fullMealDays = (int) mealsPerDay.values().stream().filter(c -> c >= 3).count();

        return stats;
    }

    private ExerciseStats calcExerciseStats(List<ExerciseLog> logs) {
        ExerciseStats stats = new ExerciseStats();
        if (logs.isEmpty()) return stats;

        stats.totalCount = logs.size();
        stats.activeDays = (int) logs.stream().map(l -> l.getLoggedAt().toLocalDate()).distinct().count();
        return stats;
    }

    private int calcHealthManagementScore(BloodSugarStats bs, BloodPressureStats bp) {
        List<Double> scores = new ArrayList<>();
        if (bs.totalCount > 0) scores.add((double) bs.normalCount / bs.totalCount * 100);
        if (bp.totalCount > 0) scores.add((double) bp.normalCount / bp.totalCount * 100);
        if (scores.isEmpty()) return 0;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private int calcMeasurementConsistencyScore(int bsCount, int bpCount) {
        List<Double> scores = new ArrayList<>();
        if (bsCount > 0) scores.add(Math.min((double) bsCount / BLOOD_SUGAR_TARGET * 100, 100));
        if (bpCount > 0) scores.add(Math.min((double) bpCount / BLOOD_PRESSURE_TARGET * 100, 100));
        if (scores.isEmpty()) return 0;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private int calcLifestyleScore(MealStats meal, ExerciseStats exercise) {
        List<Double> scores = new ArrayList<>();
        if (meal.totalCount > 0) scores.add((double) meal.fullMealDays / 7.0 * 100);
        if (exercise.totalCount > 0) scores.add(Math.min((double) exercise.activeDays / EXERCISE_TARGET * 100, 100));
        if (scores.isEmpty()) return 0;
        return (int) scores.stream().mapToDouble(Double::doubleValue).average().orElse(0);
    }

    private int calcOverallScore(int healthScore, int consistencyScore, int lifestyleScore,
                                 boolean noMeasurement, boolean noLifestyle) {
        if (noMeasurement && noLifestyle) return 0;
        if (noMeasurement) return lifestyleScore;
        if (noLifestyle) return (int) (healthScore * 0.57 + consistencyScore * 0.43);
        return (int) (healthScore * 0.4 + consistencyScore * 0.3 + lifestyleScore * 0.3);
    }

    private int calcRecordDays(List<BloodSugarLog> bs, List<BloodPressureLog> bp,
                               List<MealLog> meal, List<ExerciseLog> exercise,
                               LocalDate monday) {
        Set<LocalDate> recordedDates = new HashSet<>();
        bs.forEach(l -> recordedDates.add(l.getMeasurementTime().toLocalDate()));
        bp.forEach(l -> recordedDates.add(l.getMeasuredAt().toLocalDate()));
        meal.forEach(l -> recordedDates.add(l.getEatenAt().toLocalDate()));
        exercise.forEach(l -> recordedDates.add(l.getLoggedAt().toLocalDate()));
        return (int) recordedDates.stream()
                .filter(d -> !d.isBefore(monday) && !d.isAfter(monday.plusDays(6)))
                .count();
    }

    private List<DailyConditionEvidence> buildDailyConditions(
            LocalDate monday,
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<MealLog> mealLogs,
            List<ExerciseLog> exerciseLogs
    ) {
        List<DailyConditionEvidence> result = new ArrayList<>();

        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            List<Double> dayScores = new ArrayList<>();

            List<BloodSugarLog> dayBs = bsLogs.stream()
                    .filter(l -> l.getMeasurementTime().toLocalDate().equals(date))
                    .toList();
            if (!dayBs.isEmpty()) {
                long normalCount = dayBs.stream().filter(this::isBloodSugarNormal).count();
                dayScores.add((double) normalCount / dayBs.size() * 100);
            }

            List<BloodPressureLog> dayBp = bpLogs.stream()
                    .filter(l -> l.getMeasuredAt().toLocalDate().equals(date))
                    .toList();
            if (!dayBp.isEmpty()) {
                long normalCount = dayBp.stream()
                        .filter(l -> l.getSystolic() < 120 && l.getDiastolic() < 80)
                        .count();
                dayScores.add((double) normalCount / dayBp.size() * 100);
            }

            long mealCount = mealLogs.stream()
                    .filter(l -> l.getEatenAt().toLocalDate().equals(date))
                    .count();
            if (mealCount > 0) dayScores.add(mealCount >= 3 ? 100.0 : 0.0);

            boolean hasExercise = exerciseLogs.stream()
                    .anyMatch(l -> l.getLoggedAt().toLocalDate().equals(date));

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

    private List<HighlightEvidence> buildHighlights(
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<MealLog> mealLogs,
            List<ExerciseLog> exerciseLogs,
            LocalDate monday
    ) {
        List<HighlightEvidence> goods = new ArrayList<>();
        List<HighlightEvidence> warnings = new ArrayList<>();

        if (!bpLogs.isEmpty()) {
            int streak = calcBpNormalStreak(bpLogs, monday);
            if (streak >= 3) {
                goods.add(new HighlightEvidence(
                        HighlightType.GOOD,
                        "BP_NORMAL_STREAK",
                        "혈압 " + streak + "일 연속 정상범위 유지",
                        Map.of("days", streak)
                ));
            }
        }

        if (!bsLogs.isEmpty()) {
            int streak = calcBsNormalStreak(bsLogs, monday);
            if (streak >= 3) {
                goods.add(new HighlightEvidence(
                        HighlightType.GOOD,
                        "BS_NORMAL_STREAK",
                        "혈당 " + streak + "일 연속 정상범위 유지",
                        Map.of("days", streak)
                ));
            }
        }

        long activeDays = exerciseLogs.stream().map(l -> l.getLoggedAt().toLocalDate()).distinct().count();
        if (activeDays >= EXERCISE_TARGET) {
            goods.add(new HighlightEvidence(
                    HighlightType.GOOD,
                    "EXERCISE_GOAL",
                    "운동 목표 달성 (" + activeDays + "/" + EXERCISE_TARGET + "회)",
                    Map.of("activeDays", activeDays, "target", EXERCISE_TARGET)
            ));
        }

        int fullMealDays = calcMealStats(mealLogs).fullMealDays;
        if (fullMealDays == 7) {
            goods.add(new HighlightEvidence(
                    HighlightType.GOOD,
                    "MEAL_ATTENDANCE",
                    "식사 기록 7일 개근",
                    Map.of("days", fullMealDays)
            ));
        }

        if (bsLogs.size() >= BLOOD_SUGAR_TARGET && bpLogs.size() >= BLOOD_PRESSURE_TARGET) {
            goods.add(new HighlightEvidence(
                    HighlightType.GOOD,
                    "MEASUREMENT_GOAL",
                    "측정 목표 100% 달성",
                    Map.of("bloodSugar", bsLogs.size(), "bloodPressure", bpLogs.size())
            ));
        }

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

        if (!bsLogs.isEmpty()) {
            Map<String, List<BloodSugarLog>> afterMealBySlot = bsLogs.stream()
                    .filter(l -> l.getMeasurementLabel().contains("식후"))
                    .collect(Collectors.groupingBy(this::toMealTimeSlotLabel));

            afterMealBySlot.entrySet().stream()
                    .max(Comparator.comparingDouble(e -> calcAfterMealHighRatio(e.getValue())))
                    .ifPresent(e -> {
                        double ratio = calcAfterMealHighRatio(e.getValue());
                        if (ratio >= 0.5) {
                            warnings.add(new HighlightEvidence(
                                    HighlightType.WARNING,
                                    "AFTER_MEAL_HIGH",
                                    e.getKey() + " 혈당 지속 초과",
                                    Map.of("slot", e.getKey(), "ratio", ratio)
                            ));
                        }
                    });
        }

        if (!bpLogs.isEmpty()) {
            long highCount = countHypertensionLogs(bpLogs);
            if ((double) highCount / bpLogs.size() >= 0.5) {
                warnings.add(new HighlightEvidence(
                        HighlightType.WARNING,
                        "BP_HIGH_FREQUENT",
                        "혈압 고혈압 범위 초과 잦음",
                        Map.of("highCount", highCount, "total", bpLogs.size())
                ));
            }
        }

        if (mealLogs.isEmpty() || exerciseLogs.isEmpty()) {
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

        List<HighlightEvidence> result = new ArrayList<>();
        result.addAll(goods.stream().limit(2).toList());
        result.addAll(warnings.stream().limit(2).toList());
        return result;
    }

    private ImprovementEvidence selectImprovementArea(
            BloodSugarStats bs,
            BloodPressureStats bp,
            MealStats meal,
            ExerciseStats exercise
    ) {
        Map<ImprovementCategory, Double> scores = new LinkedHashMap<>();
        scores.put(ImprovementCategory.BLOOD_SUGAR,
                bs.totalCount > 0 ? (double) bs.normalCount / bs.totalCount * 100 : 0.0);
        scores.put(ImprovementCategory.BLOOD_PRESSURE,
                bp.totalCount > 0 ? (double) bp.normalCount / bp.totalCount * 100 : 0.0);
        scores.put(ImprovementCategory.MEAL, (double) meal.fullMealDays / 7.0 * 100);
        scores.put(ImprovementCategory.EXERCISE,
                Math.min((double) exercise.activeDays / EXERCISE_TARGET * 100, 100));

        if (scores.values().stream().allMatch(s -> s >= 80)) return null;

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
                bs.totalCount, bs.totalCount - bs.normalCount, bs.overallAvg, BLOOD_SUGAR_AFTER_MEAL_TARGET);
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
                highCount = slotLogs.stream().filter(l -> l.getGlucoseLevel() >= BLOOD_SUGAR_AFTER_MEAL_TARGET).count();
                avg = slotLogs.stream().mapToInt(BloodSugarLog::getGlucoseLevel).average().orElse(0);
                total = slotLogs.size();
                detail = String.format("%d번 중 %d번 높음, 평균 %.0f (목표 %d)", total, highCount, avg, BLOOD_SUGAR_AFTER_MEAL_TARGET);
            }
        }

        return new ImprovementEvidence(
                ImprovementCategory.BLOOD_SUGAR,
                worstSlot,
                detail,
                Map.of("highCount", highCount, "total", total, "avg", avg, "target", BLOOD_SUGAR_AFTER_MEAL_TARGET)
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
        String detail = String.format("이번 주 %d회 운동 (목표 %d회)", exercise.activeDays, EXERCISE_TARGET);
        return new ImprovementEvidence(
                ImprovementCategory.EXERCISE,
                "운동 부족",
                detail,
                Map.of("activeDays", exercise.activeDays, "target", EXERCISE_TARGET)
        );
    }

    private int calcBpNormalStreak(List<BloodPressureLog> bpLogs, LocalDate monday) {
        int streak = 0;
        int maxStreak = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            List<BloodPressureLog> dayLogs = bpLogs.stream()
                    .filter(l -> l.getMeasuredAt().toLocalDate().equals(date))
                    .toList();
            if (dayLogs.isEmpty()) {
                streak = 0;
                continue;
            }
            boolean allNormal = dayLogs.stream().allMatch(l -> l.getSystolic() < 120 && l.getDiastolic() < 80);
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
        int streak = 0;
        int maxStreak = 0;
        for (int i = 0; i < 7; i++) {
            LocalDate date = monday.plusDays(i);
            List<BloodSugarLog> dayLogs = bsLogs.stream()
                    .filter(l -> l.getMeasurementTime().toLocalDate().equals(date))
                    .toList();
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
        if (label.contains("공복") || label.contains("식전")) return g < 100;
        return g < BLOOD_SUGAR_AFTER_MEAL_TARGET;
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
        long highCount = logs.stream().filter(l -> l.getGlucoseLevel() >= BLOOD_SUGAR_AFTER_MEAL_TARGET).count();
        return (double) highCount / logs.size();
    }

    private long countHypertensionLogs(List<BloodPressureLog> logs) {
        return logs.stream()
                .filter(l -> l.getSystolic() >= BLOOD_PRESSURE_HYPERTENSION_SYSTOLIC
                        || l.getDiastolic() >= BLOOD_PRESSURE_HYPERTENSION_DIASTOLIC)
                .count();
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

    private static class BloodSugarStats {
        int totalCount = 0;
        int normalCount = 0;
        double overallAvg = 0;
        Map<String, List<BloodSugarLog>> afterMealByTimeSlot = new HashMap<>();
    }

    private static class BloodPressureStats {
        int totalCount = 0;
        int normalCount = 0;
        int hypertensionCount = 0;
        double systolicAvg = 0;
        double diastolicAvg = 0;
    }

    private static class MealStats {
        int totalCount = 0;
        int fullMealDays = 0;
        double avgCaloriesPerDay = 0;
    }

    private static class ExerciseStats {
        int totalCount = 0;
        int activeDays = 0;
    }
}
