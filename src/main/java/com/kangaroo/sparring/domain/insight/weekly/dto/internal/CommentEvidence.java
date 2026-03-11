package com.kangaroo.sparring.domain.insight.weekly.dto.internal;

public record CommentEvidence(
        Double bloodSugarAvg,
        Double systolicAvg,
        Double avgCaloriesPerDay,
        Integer exerciseSessions,
        int recordDays
) {
}
