package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.record.common.ExerciseRecord;
import com.kangaroo.sparring.domain.record.common.FoodRecord;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.TemporalRecord;
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

@Component
@RequiredArgsConstructor
public class ReportRuleEngine {

    private static final int WEEK_DAYS = 7;

    private final ReportScoreCalculator scoreCalculator;
    private final ReportEvidenceBuilder evidenceBuilder;

    public ReportEvidence evaluate(
            LocalDate monday,
            List<BloodSugarRecord> bsLogs,
            List<BloodPressureRecord> bpLogs,
            List<FoodRecord> foodLogs,
            List<ExerciseRecord> exerciseLogs
    ) {
        ReportScoreCalculator.BloodSugarStats bs = scoreCalculator.calcBloodSugarStats(bsLogs);
        ReportScoreCalculator.BloodPressureStats bp = scoreCalculator.calcBloodPressureStats(bpLogs);
        ReportScoreCalculator.MealStats meal = scoreCalculator.calcMealStats(foodLogs);
        ReportScoreCalculator.ExerciseStats exercise = scoreCalculator.calcExerciseStats(exerciseLogs);

        int healthManagement = scoreCalculator.calcHealthManagementScore(bs, bp);
        int bsRecordDays = calcMeasurementRecordDays(bsLogs, monday);
        int bpRecordDays = calcMeasurementRecordDays(bpLogs, monday);
        int measurementConsistency = scoreCalculator.calcMeasurementConsistencyScore(bs.totalCount, bp.totalCount);
        int lifestyle = scoreCalculator.calcLifestyleScore(meal, exercise);
        int stability = scoreCalculator.calcStabilityScore(bsLogs, bpLogs);
        int trend = scoreCalculator.calcTrendScore(monday, bsLogs, bpLogs);
        int overall = scoreCalculator.calcOverallScore(
                healthManagement,
                measurementConsistency,
                lifestyle,
                stability,
                trend,
                bsLogs.isEmpty() && bpLogs.isEmpty(),
                foodLogs.isEmpty() && exerciseLogs.isEmpty()
        );

        List<DailyConditionEvidence> dailyConditions = evidenceBuilder.buildDailyConditions(
                monday, bsLogs, bpLogs, foodLogs, exerciseLogs
        );
        List<HighlightEvidence> highlights = evidenceBuilder.buildHighlights(
                bsLogs, bpLogs, foodLogs, exerciseLogs, monday, dailyConditions
        );
        ImprovementEvidence improvement = evidenceBuilder.selectImprovementArea(bs, bp, meal, exercise);

        CommentEvidence comment = new CommentEvidence(
                bs.totalCount > 0 ? bs.overallAvg : null,
                bp.totalCount > 0 ? bp.systolicAvg : null,
                meal.totalCount > 0 ? meal.avgCaloriesPerDay : null,
                exercise.totalCount > 0 ? exercise.totalCount : null,
                calcRecordDays(bsLogs, bpLogs, foodLogs, exerciseLogs, monday)
        );

        return new ReportEvidence(
                comment.recordDays(),
                bsRecordDays,
                bpRecordDays,
                new ScoreEvidence(healthManagement, measurementConsistency, lifestyle, overall),
                dailyConditions,
                highlights,
                improvement,
                comment
        );
    }

    private int calcRecordDays(
            List<BloodSugarRecord> bs,
            List<BloodPressureRecord> bp,
            List<FoodRecord> meal,
            List<ExerciseRecord> exercise,
            LocalDate monday
    ) {
        Set<LocalDate> recordedDates = new HashSet<>();
        bs.forEach(l -> recordedDates.add(l.occurredAt().toLocalDate()));
        bp.forEach(l -> recordedDates.add(l.occurredAt().toLocalDate()));
        meal.forEach(l -> recordedDates.add(l.occurredAt().toLocalDate()));
        exercise.forEach(l -> recordedDates.add(l.occurredAt().toLocalDate()));
        return (int) recordedDates.stream()
                .filter(d -> !d.isBefore(monday) && !d.isAfter(monday.plusDays(WEEK_DAYS - 1L)))
                .count();
    }

    private int calcMeasurementRecordDays(List<? extends TemporalRecord> logs, LocalDate monday) {
        LocalDate sunday = monday.plusDays(WEEK_DAYS - 1L);
        return (int) logs.stream()
                .map(log -> log.occurredAt().toLocalDate())
                .filter(date -> !date.isBefore(monday) && !date.isAfter(sunday))
                .distinct()
                .count();
    }
}
