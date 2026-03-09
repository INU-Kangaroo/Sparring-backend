package com.kangaroo.sparring.domain.insight.weekly.dto.internal;

import com.kangaroo.sparring.domain.insight.weekly.type.DailyConditionStatus;

public record DailyConditionEvidence(
        String dayOfWeek,
        DailyConditionStatus status,
        double score
) {
}
