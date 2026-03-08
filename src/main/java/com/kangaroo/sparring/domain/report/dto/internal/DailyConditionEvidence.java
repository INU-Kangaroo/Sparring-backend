package com.kangaroo.sparring.domain.report.dto.internal;

import com.kangaroo.sparring.domain.report.type.DailyConditionStatus;

public record DailyConditionEvidence(
        String dayOfWeek,
        DailyConditionStatus status,
        double score
) {
}
