package com.kangaroo.sparring.domain.insight.weekly.dto.internal;

import java.util.List;

public record ReportEvidence(
        int recordDays,
        int bloodSugarRecordDays,
        int bloodPressureRecordDays,
        ScoreEvidence score,
        List<DailyConditionEvidence> dailyConditions,
        List<HighlightEvidence> highlights,
        ImprovementEvidence improvement,
        CommentEvidence comment
) {
}
