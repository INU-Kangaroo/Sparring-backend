package com.kangaroo.sparring.domain.report.dto.internal;

import com.kangaroo.sparring.domain.report.type.ImprovementCategory;

import java.util.Map;

public record ImprovementEvidence(
        ImprovementCategory category,
        String timeLabel,
        String detail,
        Map<String, Object> facts
) {
}
