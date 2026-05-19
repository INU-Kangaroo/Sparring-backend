package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.record.common.ExerciseRecord;
import com.kangaroo.sparring.domain.record.common.FoodRecord;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

record ReportWeekDataIndex(
        Map<LocalDate, List<BloodSugarRecord>> bloodSugarByDate,
        Map<LocalDate, List<BloodPressureRecord>> bloodPressureByDate,
        Map<LocalDate, List<FoodRecord>> foodByDate,
        Map<LocalDate, List<ExerciseRecord>> exerciseByDate
) {
    static ReportWeekDataIndex from(
            List<BloodSugarRecord> bsLogs,
            List<BloodPressureRecord> bpLogs,
            List<FoodRecord> foodLogs,
            List<ExerciseRecord> exerciseLogs
    ) {
        return new ReportWeekDataIndex(
                bsLogs.stream().collect(Collectors.groupingBy(log -> log.occurredAt().toLocalDate())),
                bpLogs.stream().collect(Collectors.groupingBy(log -> log.occurredAt().toLocalDate())),
                foodLogs.stream().collect(Collectors.groupingBy(log -> log.occurredAt().toLocalDate())),
                exerciseLogs.stream().collect(Collectors.groupingBy(log -> log.occurredAt().toLocalDate()))
        );
    }
}
