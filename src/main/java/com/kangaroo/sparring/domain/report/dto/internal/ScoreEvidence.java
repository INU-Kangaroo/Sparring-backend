package com.kangaroo.sparring.domain.report.dto.internal;

public record ScoreEvidence(
        int healthManagement,
        int measurementConsistency,
        int lifestyle,
        int overallScore
) {
}
