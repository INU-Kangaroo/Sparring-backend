package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.exercise.log.entity.ExerciseLog;
import com.kangaroo.sparring.domain.food.log.entity.FoodLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.CommentEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.DailyConditionEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.HighlightEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ImprovementEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ReportEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ScoreEvidence;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Function;

@Component
@RequiredArgsConstructor
public class ReportRuleEngine {

    private static final int WEEK_DAYS = 7;

    private final ReportRuleSupport reportRuleSupport;

    public ReportEvidence evaluate(
            LocalDate monday,
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<FoodLog> mealLogs,
            List<ExerciseLog> exerciseLogs
    ) {
        ReportRuleSupport.BloodSugarStats bs = reportRuleSupport.calcBloodSugarStats(bsLogs);
        ReportRuleSupport.BloodPressureStats bp = reportRuleSupport.calcBloodPressureStats(bpLogs);
        ReportRuleSupport.MealStats meal = reportRuleSupport.calcMealStats(mealLogs);
        ReportRuleSupport.ExerciseStats exercise = reportRuleSupport.calcExerciseStats(exerciseLogs);

        int healthManagement = reportRuleSupport.calcHealthManagementScore(bs, bp);
        int measurementConsistency = reportRuleSupport.calcMeasurementConsistencyScore(bsLogs.size(), bpLogs.size());
        int lifestyle = reportRuleSupport.calcLifestyleScore(meal, exercise);
        int stability = reportRuleSupport.calcStabilityScore(bsLogs, bpLogs);
        int trend = reportRuleSupport.calcTrendScore(monday, bsLogs, bpLogs);
        int overall = reportRuleSupport.calcOverallScore(
                healthManagement,
                measurementConsistency,
                lifestyle,
                stability,
                trend,
                bsLogs.isEmpty() && bpLogs.isEmpty(),
                mealLogs.isEmpty() && exerciseLogs.isEmpty()
        );

        List<DailyConditionEvidence> dailyConditions = reportRuleSupport.buildDailyConditions(
                monday, bsLogs, bpLogs, mealLogs, exerciseLogs
        );
        List<HighlightEvidence> highlights = reportRuleSupport.buildHighlights(
                bsLogs, bpLogs, mealLogs, exerciseLogs, monday
        );
        ImprovementEvidence improvement = reportRuleSupport.selectImprovementArea(bs, bp, meal, exercise);

        CommentEvidence comment = new CommentEvidence(
                bs.totalCount > 0 ? bs.overallAvg : null,
                bp.totalCount > 0 ? bp.systolicAvg : null,
                meal.totalCount > 0 ? meal.avgCaloriesPerDay : null,
                exercise.totalCount > 0 ? exercise.totalCount : null,
                calcRecordDays(bsLogs, bpLogs, mealLogs, exerciseLogs, monday)
        );

        return new ReportEvidence(
                comment.recordDays(),
                calcMeasurementRecordDays(bsLogs, monday, BloodSugarLog::getMeasurementTime),
                calcMeasurementRecordDays(bpLogs, monday, BloodPressureLog::getMeasuredAt),
                new ScoreEvidence(healthManagement, measurementConsistency, lifestyle, overall),
                dailyConditions,
                highlights,
                improvement,
                comment
        );
    }

    private int calcRecordDays(
            List<BloodSugarLog> bs,
            List<BloodPressureLog> bp,
            List<FoodLog> meal,
            List<ExerciseLog> exercise,
            LocalDate monday
    ) {
        Set<LocalDate> recordedDates = new HashSet<>();
        bs.forEach(l -> recordedDates.add(l.getMeasurementTime().toLocalDate()));
        bp.forEach(l -> recordedDates.add(l.getMeasuredAt().toLocalDate()));
        meal.forEach(l -> recordedDates.add(l.getEatenAt().toLocalDate()));
        exercise.forEach(l -> recordedDates.add(l.getLoggedAt().toLocalDate()));
        return (int) recordedDates.stream()
                .filter(d -> !d.isBefore(monday) && !d.isAfter(monday.plusDays(WEEK_DAYS - 1L)))
                .count();
    }

    private <T> int calcMeasurementRecordDays(
            List<T> logs,
            LocalDate monday,
            Function<T, java.time.LocalDateTime> timeExtractor
    ) {
        LocalDate sunday = monday.plusDays(WEEK_DAYS - 1L);
        return (int) logs.stream()
                .map(log -> timeExtractor.apply(log).toLocalDate())
                .filter(date -> !date.isBefore(monday) && !date.isAfter(sunday))
                .distinct()
                .count();
    }
}
