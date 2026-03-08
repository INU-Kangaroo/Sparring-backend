package com.kangaroo.sparring.domain.report.dto.internal;

import com.kangaroo.sparring.domain.report.type.HighlightType;

import java.util.Map;

public record HighlightEvidence(
        HighlightType type,
        String ruleId,
        String message,
        Map<String, Object> facts
) {
}
