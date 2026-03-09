package com.kangaroo.sparring.domain.report.service;

import com.kangaroo.sparring.domain.exercise.log.entity.ExerciseLog;
import com.kangaroo.sparring.domain.food.log.entity.FoodLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record ReportWeekDataIndex(
        Map<LocalDate, List<BloodSugarLog>> bloodSugarByDate,
        Map<LocalDate, List<BloodPressureLog>> bloodPressureByDate,
        Map<LocalDate, List<FoodLog>> mealByDate,
        Map<LocalDate, List<ExerciseLog>> exerciseByDate
) {
    static ReportWeekDataIndex from(
            List<BloodSugarLog> bsLogs,
            List<BloodPressureLog> bpLogs,
            List<FoodLog> mealLogs,
            List<ExerciseLog> exerciseLogs
    ) {
        return new ReportWeekDataIndex(
                bsLogs.stream().collect(Collectors.groupingBy(log -> log.getMeasurementTime().toLocalDate())),
                bpLogs.stream().collect(Collectors.groupingBy(log -> log.getMeasuredAt().toLocalDate())),
                mealLogs.stream().collect(Collectors.groupingBy(log -> log.getEatenAt().toLocalDate())),
                exerciseLogs.stream().collect(Collectors.groupingBy(log -> log.getLoggedAt().toLocalDate()))
        );
    }
}
