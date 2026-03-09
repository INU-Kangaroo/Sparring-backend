package com.kangaroo.sparring.domain.insight.weekly.dto.internal;

public record ScoreEvidence(
        int healthManagement,
        int measurementConsistency,
        int lifestyle,
        int overallScore
) {
}
