package com.kangaroo.sparring.domain.report.dto.internal;

public record CommentEvidence(
        Double bloodSugarAvg,
        Double systolicAvg,
        Double avgCaloriesPerDay,
        Integer exerciseSessions,
        int recordDays
) {
}
