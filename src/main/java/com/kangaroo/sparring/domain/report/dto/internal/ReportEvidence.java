package com.kangaroo.sparring.domain.report.dto.internal;

import java.util.List;

public record ReportEvidence(
        int recordDays,
        int bloodSugarMeasurements,
        int bloodPressureMeasurements,
        ScoreEvidence score,
        List<DailyConditionEvidence> dailyConditions,
        List<HighlightEvidence> highlights,
        ImprovementEvidence improvement,
        CommentEvidence comment
) {
}
