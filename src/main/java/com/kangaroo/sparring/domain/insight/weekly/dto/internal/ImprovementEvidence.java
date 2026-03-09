package com.kangaroo.sparring.domain.insight.weekly.dto.internal;

import com.kangaroo.sparring.domain.insight.weekly.type.ImprovementCategory;

import java.util.Map;

public record ImprovementEvidence(
        ImprovementCategory category,
        String timeLabel,
        String detail,
        Map<String, Object> facts
) {
}
