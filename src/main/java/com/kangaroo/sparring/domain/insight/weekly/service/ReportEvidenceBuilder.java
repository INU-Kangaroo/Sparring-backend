package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.common.health.HealthThresholds;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.ExerciseRecord;
import com.kangaroo.sparring.domain.record.common.FoodRecord;
import com.kangaroo.sparring.domain.record.common.TemporalRecord;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.DailyConditionEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.HighlightEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ImprovementEvidence;
import com.kangaroo.sparring.domain.insight.weekly.policy.ReportPolicyProperties;
import com.kangaroo.sparring.domain.insight.weekly.type.DailyConditionStatus;
import com.kangaroo.sparring.domain.insight.weekly.type.HighlightType;
import com.kangaroo.sparring.domain.insight.weekly.type.ImprovementCategory;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 주간 리포트 Evidence 생성 전담 컴포넌트.
 * 일별 컨디션, 하이라이트, 개선 영역 선택 로직을 담당한다.
 */
@Component
@RequiredArgsConstructor
class ReportEvidenceBuilder {

    private static final int WEEK_DAYS = 7;
    private static final String[] DAY_LABELS = {"MON", "TUE", "WED", "THU", "FRI", "SAT", "SUN"};

    private final ReportPolicyProperties policy;

    List<DailyConditionEvidence> buildDailyConditions(
            LocalDate monday,
            List<BloodSugarRecord> bsLogs,
            List<BloodPressureRecord> bpLogs,
            List<FoodRecord> foodLogs,
            List<ExerciseRecord> exerciseLogs
    ) {
        ReportWeekDataIndex weekDataIndex = ReportWeekDataIndex.from(bsLogs, bpLogs, foodLogs, exerciseLogs);
        List<DailyConditionEvidence> result = new ArrayList<>();

        for (int i = 0; i < WEEK_DAYS; i++) {
            LocalDate date = monday.plusDays(i);
            List<Double> dayScores = new ArrayList<>();

            List<BloodSugarRecord> dayBs = weekDataIndex.bloodSugarByDate().getOrDefault(date, List.of());
            if (!dayBs.isEmpty()) {
                long normalCount = dayBs.stream().filter(ReportHealthClassifier::isBloodSugarNormal).count();
                dayScores.add((double) normalCount / dayBs.size() * 100);
            }

            List<BloodPressureRecord> dayBp = weekDataIndex.bloodPressureByDate().getOrDefault(date, List.of());
            if (!dayBp.isEmpty()) {
                long normalCount = dayBp.stream().filter(ReportHealthClassifier::isBloodPressureNormal).count();
                dayScores.add((double) normalCount / dayBp.size() * 100);
            }

            long mealCount = weekDataIndex.foodByDate().getOrDefault(date, List.of()).size();
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
            List<BloodSugarRecord> bsLogs,
            List<BloodPressureRecord> bpLogs,
            List<FoodRecord> foodLogs,
            List<ExerciseRecord> exerciseLogs,
            LocalDate monday,
            List<DailyConditionEvidence> dailyConditions
    ) {
        List<HighlightEvidence> goods = new ArrayList<>();
        List<HighlightEvidence> warnings = new ArrayList<>();

        addGoodHighlights(goods, bsLogs, bpLogs, foodLogs, exerciseLogs, monday);
        addWarningHighlights(warnings, bsLogs, bpLogs, foodLogs, exerciseLogs, dailyConditions);

        List<HighlightEvidence> result = new ArrayList<>();
        result.addAll(goods.stream().limit(2).toList());
        result.addAll(warnings.stream().limit(2).toList());
        return result;
    }

    ImprovementEvidence selectImprovementArea(
            ReportScoreCalculator.BloodSugarStats bs,
            ReportScoreCalculator.BloodPressureStats bp,
            ReportScoreCalculator.MealStats meal,
            ReportScoreCalculator.ExerciseStats exercise
    ) {
        Map<ImprovementCategory, Double> scores = new LinkedHashMap<>();
        addImprovementScore(scores, ImprovementCategory.BLOOD_SUGAR,
                bs.totalCount > 0 ? percentage(bs.normalCount, bs.totalCount) : null);
        addImprovementScore(scores, ImprovementCategory.BLOOD_PRESSURE,
                bp.totalCount > 0 ? percentage(bp.normalCount, bp.totalCount) : null);
        addImprovementScore(scores, ImprovementCategory.MEAL,
                meal.totalCount > 0 ? percentage(meal.fullMealDays, policy.getFullMealDaysTarget()) : null);
        addImprovementScore(scores, ImprovementCategory.EXERCISE,
                exercise.totalCount > 0 ? cappedPercentage(exercise.activeDays, policy.getExerciseActiveDaysTarget(), 100) : null);

        // 1) 실제 데이터에서 위험 신호가 보이면 우선 개선 영역으로 선택
        if (!scores.isEmpty() && scores.values().stream().anyMatch(s -> s < 80)) {
            ImprovementCategory worst = scores.entrySet().stream()
                    .min(Map.Entry.comparingByValue())
                    .map(Map.Entry::getKey)
                    .orElse(null);
            if (worst != null) {
                return switch (worst) {
                    case BLOOD_SUGAR -> buildBloodSugarImprovement(bs);
                    case BLOOD_PRESSURE -> buildBloodPressureImprovement(bp);
                    case MEAL -> buildMealImprovement(meal);
                    case EXERCISE -> buildExerciseImprovement(exercise);
                };
            }
        }

        // 2) 위험 신호가 없으면 데이터 부족 영역을 개선 과제로 노출
        if (bs.totalCount == 0) {
            return buildBloodSugarNoDataImprovement();
        }
        if (bp.totalCount == 0) {
            return buildBloodPressureNoDataImprovement();
        }
        if (meal.totalCount == 0) {
            return buildMealNoDataImprovement();
        }
        if (exercise.totalCount == 0) {
            return buildExerciseNoDataImprovement();
        }

        return null;
    }

    private void addGoodHighlights(
            List<HighlightEvidence> goods,
            List<BloodSugarRecord> bsLogs,
            List<BloodPressureRecord> bpLogs,
            List<FoodRecord> foodLogs,
            List<ExerciseRecord> exerciseLogs,
            LocalDate monday
    ) {
        addBloodPressureStreakGood(goods, bpLogs, monday);
        addBloodSugarStreakGood(goods, bsLogs, monday);
        addExerciseGoalGood(goods, exerciseLogs);
        addMealAttendanceGood(goods, foodLogs);

        int bsRecordDays = calcRecordDaysForLogs(bsLogs, monday);
        int bpRecordDays = calcRecordDaysForLogs(bpLogs, monday);
        addMeasurementGoalGood(goods, bsRecordDays, bpRecordDays);
    }

    private void addWarningHighlights(
            List<HighlightEvidence> warnings,
            List<BloodSugarRecord> bsLogs,
            List<BloodPressureRecord> bpLogs,
            List<FoodRecord> foodLogs,
            List<ExerciseRecord> exerciseLogs,
            List<DailyConditionEvidence> dailyConditions
    ) {
        addDailyConditionWarning(warnings, dailyConditions);
        addAfterMealHighWarning(warnings, bsLogs);
        addBloodPressureHighWarning(warnings, bpLogs);
        addLifestyleMissingWarning(warnings, foodLogs, exerciseLogs);
    }

    private void addBloodPressureStreakGood(
            List<HighlightEvidence> goods,
            List<BloodPressureRecord> bpLogs,
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
            List<BloodSugarRecord> bsLogs,
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

    private void addExerciseGoalGood(List<HighlightEvidence> goods, List<ExerciseRecord> exerciseLogs) {
        long activeDays = exerciseLogs.stream().map(l -> l.occurredAt().toLocalDate()).distinct().count();
        if (activeDays < policy.getExerciseActiveDaysTarget()) return;
        goods.add(new HighlightEvidence(
                HighlightType.GOOD,
                "EXERCISE_GOAL",
                "운동 목표 달성 (" + activeDays + "/" + policy.getExerciseActiveDaysTarget() + "회)",
                Map.of("activeDays", activeDays, "target", policy.getExerciseActiveDaysTarget())
        ));
    }

    private void addMealAttendanceGood(List<HighlightEvidence> goods, List<FoodRecord> foodLogs) {
        Map<LocalDate, Long> mealsPerDay = foodLogs.stream()
                .collect(Collectors.groupingBy(l -> l.occurredAt().toLocalDate(), Collectors.counting()));
        int fullMealDays = (int) mealsPerDay.values().stream().filter(c -> c >= 3).count();
        if (fullMealDays < policy.getFullMealDaysTarget()) return;
        goods.add(new HighlightEvidence(
                HighlightType.GOOD,
                "MEAL_ATTENDANCE",
                "식사 기록 " + policy.getFullMealDaysTarget() + "일 개근",
                Map.of("days", fullMealDays)
        ));
    }

    private void addMeasurementGoalGood(List<HighlightEvidence> goods, int bsRecordDays, int bpRecordDays) {
        if (bsRecordDays < WEEK_DAYS && bpRecordDays < WEEK_DAYS) return;
        goods.add(new HighlightEvidence(
                HighlightType.GOOD,
                "MEASUREMENT_GOAL",
                "측정 목표 100% 달성",
                Map.of("bloodSugarRecordDays", bsRecordDays, "bloodPressureRecordDays", bpRecordDays)
        ));
    }

    private void addDailyConditionWarning(
            List<HighlightEvidence> warnings,
            List<DailyConditionEvidence> conditions
    ) {
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

    private void addAfterMealHighWarning(List<HighlightEvidence> warnings, List<BloodSugarRecord> bsLogs) {
        if (bsLogs.isEmpty()) return;
        Map<String, List<BloodSugarRecord>> afterMealBySlot = bsLogs.stream()
                .filter(l -> l.getMeasurementLabel().contains("식후"))
                .collect(Collectors.groupingBy(ReportHealthClassifier::toMealTimeSlotLabel));

        afterMealBySlot.entrySet().stream()
                .max(Comparator.comparingDouble(e -> ReportHealthClassifier.calcAfterMealHighRatio(e.getValue())))
                .ifPresent(e -> {
                    double ratio = ReportHealthClassifier.calcAfterMealHighRatio(e.getValue());
                    if (ratio < 0.5) return;
                    warnings.add(new HighlightEvidence(
                            HighlightType.WARNING,
                            "AFTER_MEAL_HIGH",
                            e.getKey() + " 혈당 지속 초과",
                            Map.of("slot", e.getKey(), "ratio", ratio)
                    ));
                });
    }

    private void addBloodPressureHighWarning(List<HighlightEvidence> warnings, List<BloodPressureRecord> bpLogs) {
        if (bpLogs.isEmpty()) return;
        long highCount = ReportHealthClassifier.countHypertensionLogs(bpLogs);
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
            List<FoodRecord> foodLogs,
            List<ExerciseRecord> exerciseLogs
    ) {
        if (!foodLogs.isEmpty() && !exerciseLogs.isEmpty()) return;
        String message = foodLogs.isEmpty() && exerciseLogs.isEmpty()
                ? "식사/운동 기록 없음"
                : foodLogs.isEmpty() ? "식사 기록 없음" : "운동 기록 없음";
        warnings.add(new HighlightEvidence(
                HighlightType.WARNING,
                "LIFESTYLE_MISSING",
                message,
                Map.of("mealMissing", foodLogs.isEmpty(), "exerciseMissing", exerciseLogs.isEmpty())
        ));
    }

    private ImprovementEvidence buildBloodSugarImprovement(ReportScoreCalculator.BloodSugarStats bs) {
        String worstSlot = "식후 혈당";
        long highCount = bs.totalCount - bs.normalCount;
        int total = bs.totalCount;
        double avg = bs.overallAvg;

        if (!bs.afterMealByTimeSlot.isEmpty()) {
            String worst = bs.afterMealByTimeSlot.entrySet().stream()
                    .sorted((a, b) -> {
                        double ratioA = ReportHealthClassifier.calcAfterMealHighRatio(a.getValue());
                        double ratioB = ReportHealthClassifier.calcAfterMealHighRatio(b.getValue());
                        int ratioCompare = Double.compare(ratioB, ratioA);
                        if (ratioCompare != 0) return ratioCompare;
                        double avgA = a.getValue().stream().mapToInt(BloodSugarRecord::getGlucoseLevel).average().orElse(0);
                        double avgB = b.getValue().stream().mapToInt(BloodSugarRecord::getGlucoseLevel).average().orElse(0);
                        return Double.compare(avgB, avgA);
                    })
                    .map(Map.Entry::getKey)
                    .findFirst()
                    .orElse("식후 혈당");
            worstSlot = worst;

            List<BloodSugarRecord> slotLogs = bs.afterMealByTimeSlot.get(worst);
            if (slotLogs != null && !slotLogs.isEmpty()) {
                highCount = slotLogs.stream().filter(ReportHealthClassifier::isPostMealBloodSugarHigh).count();
                avg = slotLogs.stream().mapToInt(BloodSugarRecord::getGlucoseLevel).average().orElse(0);
                total = slotLogs.size();
            }
        }

        String detail = String.format("%d번 중 %d번 높음, 평균 %.0f (목표 %d)",
                total, highCount, avg, HealthThresholds.BLOOD_SUGAR_POST_MEAL_NORMAL_MAX + 1);
        return new ImprovementEvidence(
                ImprovementCategory.BLOOD_SUGAR,
                worstSlot,
                detail,
                Map.of("highCount", highCount, "total", total, "avg", avg,
                        "target", HealthThresholds.BLOOD_SUGAR_POST_MEAL_NORMAL_MAX + 1)
        );
    }

    private ImprovementEvidence buildBloodPressureImprovement(ReportScoreCalculator.BloodPressureStats bp) {
        String detail = String.format("%d번 중 %d번 고혈압 범위 초과, 평균 수축기 %.0f",
                bp.totalCount, bp.hypertensionCount, bp.systolicAvg);
        return new ImprovementEvidence(
                ImprovementCategory.BLOOD_PRESSURE,
                "혈압 관리",
                detail,
                Map.of("highCount", bp.hypertensionCount, "total", bp.totalCount, "avgSystolic", bp.systolicAvg)
        );
    }

    private ImprovementEvidence buildMealImprovement(ReportScoreCalculator.MealStats meal) {
        String detail = String.format("이번 주 %d일만 3끼 기록 (목표 %d일)",
                meal.fullMealDays, policy.getFullMealDaysTarget());
        return new ImprovementEvidence(
                ImprovementCategory.MEAL,
                "식사 불규칙",
                detail,
                Map.of("fullMealDays", meal.fullMealDays, "target", policy.getFullMealDaysTarget())
        );
    }

    private ImprovementEvidence buildExerciseImprovement(ReportScoreCalculator.ExerciseStats exercise) {
        String detail = String.format("이번 주 %d회 운동 (목표 %d회)",
                exercise.activeDays, policy.getExerciseActiveDaysTarget());
        return new ImprovementEvidence(
                ImprovementCategory.EXERCISE,
                "운동 부족",
                detail,
                Map.of("activeDays", exercise.activeDays, "target", policy.getExerciseActiveDaysTarget())
        );
    }

    private ImprovementEvidence buildBloodSugarNoDataImprovement() {
        return new ImprovementEvidence(
                ImprovementCategory.BLOOD_SUGAR,
                "혈당 데이터 부족",
                String.format("혈당 기록이 없어 분석할 수 없어요. 이번 주 최소 %d회 측정해보세요.",
                        policy.getBloodSugarWeeklyTarget()),
                Map.of("recordedCount", 0, "target", policy.getBloodSugarWeeklyTarget())
        );
    }

    private ImprovementEvidence buildBloodPressureNoDataImprovement() {
        return new ImprovementEvidence(
                ImprovementCategory.BLOOD_PRESSURE,
                "혈압 데이터 부족",
                String.format("혈압 기록이 없어 분석할 수 없어요. 이번 주 최소 %d회 측정해보세요.",
                        policy.getBloodPressureWeeklyTarget()),
                Map.of("recordedCount", 0, "target", policy.getBloodPressureWeeklyTarget())
        );
    }

    private ImprovementEvidence buildMealNoDataImprovement() {
        return new ImprovementEvidence(
                ImprovementCategory.MEAL,
                "식사 기록 부족",
                String.format("식사 기록이 없어 분석할 수 없어요. 이번 주 최소 %d일 3끼를 기록해보세요.",
                        policy.getFullMealDaysTarget()),
                Map.of("fullMealDays", 0, "target", policy.getFullMealDaysTarget())
        );
    }

    private ImprovementEvidence buildExerciseNoDataImprovement() {
        return new ImprovementEvidence(
                ImprovementCategory.EXERCISE,
                "운동 기록 부족",
                String.format("운동 기록이 없어 분석할 수 없어요. 이번 주 최소 %d일 기록해보세요.",
                        policy.getExerciseActiveDaysTarget()),
                Map.of("activeDays", 0, "target", policy.getExerciseActiveDaysTarget())
        );
    }

    private int calcBpNormalStreak(List<BloodPressureRecord> bpLogs, LocalDate monday) {
        Map<LocalDate, List<BloodPressureRecord>> bpByDate = bpLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getMeasuredAt().toLocalDate()));
        int streak = 0;
        int maxStreak = 0;
        for (int i = 0; i < WEEK_DAYS; i++) {
            List<BloodPressureRecord> dayLogs = bpByDate.getOrDefault(monday.plusDays(i), List.of());
            if (dayLogs.isEmpty()) {
                streak = 0;
                continue;
            }
            if (dayLogs.stream().allMatch(ReportHealthClassifier::isBloodPressureNormal)) {
                maxStreak = Math.max(maxStreak, ++streak);
            } else {
                streak = 0;
            }
        }
        return maxStreak;
    }

    private int calcBsNormalStreak(List<BloodSugarRecord> bsLogs, LocalDate monday) {
        Map<LocalDate, List<BloodSugarRecord>> bsByDate = bsLogs.stream()
                .collect(Collectors.groupingBy(log -> log.getMeasurementTime().toLocalDate()));
        int streak = 0;
        int maxStreak = 0;
        for (int i = 0; i < WEEK_DAYS; i++) {
            List<BloodSugarRecord> dayLogs = bsByDate.getOrDefault(monday.plusDays(i), List.of());
            if (dayLogs.isEmpty()) {
                streak = 0;
                continue;
            }
            if (dayLogs.stream().allMatch(ReportHealthClassifier::isBloodSugarNormal)) {
                maxStreak = Math.max(maxStreak, ++streak);
            } else {
                streak = 0;
            }
        }
        return maxStreak;
    }

    private int calcRecordDaysForLogs(List<? extends TemporalRecord> logs, LocalDate monday) {
        LocalDate sunday = monday.plusDays(WEEK_DAYS - 1L);
        return (int) logs.stream()
                .map(log -> log.occurredAt().toLocalDate())
                .filter(d -> !d.isBefore(monday) && !d.isAfter(sunday))
                .distinct()
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

    private Double percentage(int numerator, int denominator) {
        if (denominator <= 0) return null;
        return (double) numerator / denominator * 100;
    }

    private Double cappedPercentage(int numerator, int denominator, int max) {
        Double score = percentage(numerator, denominator);
        return score == null ? null : Math.min(score, max);
    }

    private void addImprovementScore(Map<ImprovementCategory, Double> scores,
                                      ImprovementCategory category, Double score) {
        if (score != null) scores.put(category, score);
    }
}
