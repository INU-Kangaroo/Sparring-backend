package com.kangaroo.sparring.domain.insight.weekly.dto.internal;

import com.kangaroo.sparring.domain.insight.weekly.type.HighlightType;

import java.util.Map;

public record HighlightEvidence(
        HighlightType type,
        String ruleId,
        String message,
        Map<String, Object> facts
) {
}
