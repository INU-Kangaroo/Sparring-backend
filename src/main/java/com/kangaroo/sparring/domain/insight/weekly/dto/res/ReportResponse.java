package com.kangaroo.sparring.domain.insight.weekly.dto.res;

import com.kangaroo.sparring.domain.insight.weekly.entity.Report;
import com.kangaroo.sparring.domain.insight.weekly.type.DailyConditionStatus;
import com.kangaroo.sparring.domain.insight.weekly.type.HighlightType;
import com.kangaroo.sparring.domain.insight.weekly.type.ImprovementCategory;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.util.List;

@Getter
@Builder
@Schema(
        description = "주간 종합 건강 보고서",
        example = """
                {
                  "reportId": 12,
                  "startDate": "2026-03-02",
                  "endDate": "2026-03-08",
                  "recordDays": 7,
                  "bloodSugarRecordDays": 6,
                  "bloodPressureRecordDays": 7,
                  "aiComment": "이번 주 전반적으로 안정적인 흐름이에요. 점심 식후 혈당만 조금 더 관리하면 더 좋아질 거예요.",
                  "overallScore": 78,
                  "scoreLabel": "잘하고 있어요! 조금만 더 💪",
                  "scores": {
                    "healthManagement": 80,
                    "measurementConsistency": 74,
                    "lifestyle": 79
                  },
                  "dailyConditions": [
                    { "dayOfWeek": "MON", "status": "GOOD" },
                    { "dayOfWeek": "TUE", "status": "GOOD" },
                    { "dayOfWeek": "WED", "status": "CAUTION" },
                    { "dayOfWeek": "THU", "status": "BAD" },
                    { "dayOfWeek": "FRI", "status": "GOOD" },
                    { "dayOfWeek": "SAT", "status": "CAUTION" },
                    { "dayOfWeek": "SUN", "status": "GOOD" }
                  ],
                  "highlights": [
                    { "type": "GOOD", "message": "혈압 7일 연속 정상범위 유지" },
                    { "type": "GOOD", "message": "운동 목표 달성 (5/5회)" },
                    { "type": "WARNING", "message": "목요일 전반적 컨디션 저조" },
                    { "type": "WARNING", "message": "점심 식후 혈당 지속 초과" }
                  ],
                  "improvement": {
                    "category": "BLOOD_SUGAR",
                    "timeLabel": "점심 식후",
                    "detail": "10번 중 7번 높음, 평균 165 (목표 140)",
                    "tips": ["밥 양 20% 줄이기", "현미밥으로 변경", "식후 15분 걷기"]
                  }
                }
                """
)
public class ReportResponse {

    @Schema(description = "보고서 ID", example = "12")
    private Long reportId;

    @Schema(description = "시작일 (월요일)", example = "2026-03-02")
    private LocalDate startDate;

    @Schema(description = "종료일 (일요일)", example = "2026-03-08")
    private LocalDate endDate;

    // ── 헤더 ──────────────────────────────────────
    @Schema(description = "기록일 수", example = "7")
    private Integer recordDays;

    @Schema(description = "혈당 기록한 날 수", example = "6")
    private Integer bloodSugarRecordDays;

    @Schema(description = "혈압 기록한 날 수", example = "7")
    private Integer bloodPressureRecordDays;

    // ── AI 코멘트 ──────────────────────────────────
    @Schema(description = "AI 종합 코멘트", example = "이번 주 전반적으로 안정적인 흐름이에요. 점심 식후 혈당만 조금 더 관리하면 더 좋아질 거예요.")
    private String aiComment;

    // ── 종합 점수 ──────────────────────────────────
    @Schema(description = "종합 점수", example = "78")
    private Integer overallScore;

    @Schema(description = "종합 점수 라벨", example = "잘하고 있어요! 조금만 더 💪")
    private String scoreLabel;

    @Schema(description = "항목별 점수")
    private ScoreDetail scores;

    // ── 이번 주 요약 ────────────────────────────────
    @Schema(description = "요일별 종합 컨디션 (월~일)")
    private List<DailyCondition> dailyConditions;

    @Schema(description = "이번 주 하이라이트")
    private List<HighlightItem> highlights;

    // ── 개선 필요 ───────────────────────────────────
    @Schema(description = "개선 필요 섹션 (최하위 1개, 없으면 null)")
    private ImprovementDetail improvement;

    // ── 내부 record ─────────────────────────────────

    @Getter
    @Builder
    @Schema(description = "항목별 점수", example = "{\"healthManagement\":80,\"measurementConsistency\":74,\"lifestyle\":79}")
    public static class ScoreDetail {
        @Schema(description = "건강 관리 점수 (혈당+혈압 정상범위 비율)", example = "80")
        private Integer healthManagement;

        @Schema(description = "측정 꾸준함 점수", example = "74")
        private Integer measurementConsistency;

        @Schema(description = "생활 습관 점수 (식사+운동)", example = "79")
        private Integer lifestyle;
    }

    @Getter
    @Builder
    @Schema(description = "요일별 종합 컨디션", example = "{\"dayOfWeek\":\"THU\",\"status\":\"BAD\"}")
    public static class DailyCondition {
        @Schema(description = "요일 (MON~SUN)", example = "MON")
        private String dayOfWeek;

        @Schema(description = "컨디션 상태", example = "GOOD")
        private DailyConditionStatus status;
    }

    @Getter
    @Builder
    @Schema(description = "이번 주 하이라이트 항목", example = "{\"type\":\"GOOD\",\"message\":\"혈압 7일 연속 정상범위 유지\"}")
    public static class HighlightItem {
        @Schema(description = "타입 (GOOD / WARNING)", example = "GOOD")
        private HighlightType type;

        @Schema(description = "하이라이트 메시지", example = "혈압 7일 연속 정상범위 유지")
        private String message;
    }

    @Getter
    @Builder
    @Schema(description = "개선 필요 섹션", example = "{\"category\":\"BLOOD_SUGAR\",\"timeLabel\":\"점심 식후\",\"detail\":\"10번 중 7번 높음, 평균 165 (목표 140)\",\"tips\":[\"밥 양 20% 줄이기\",\"현미밥으로 변경\",\"식후 15분 걷기\"]}")
    public static class ImprovementDetail {
        @Schema(description = "카테고리", example = "BLOOD_SUGAR")
        private ImprovementCategory category;

        @Schema(description = "개선 대상 라벨 (예: 점심 식후 혈당)", example = "점심 식후")
        private String timeLabel;

        @Schema(description = "상세 설명 (예: 10번 중 7번 높음, 평균 165)", example = "10번 중 7번 높음, 평균 165 (목표 140)")
        private String detail;

        @Schema(description = "개선 방법 3가지", example = "[\"밥 양 20% 줄이기\",\"현미밥으로 변경\",\"식후 15분 걷기\"]")
        private List<String> tips;
    }

    public static ReportResponse of(
            Report report,
            String scoreLabel,
            List<DailyCondition> dailyConditions,
            List<HighlightItem> highlights,
            ImprovementDetail improvement
    ) {
        ImprovementDetail imp = null;
        if (report.getImprovementCategory() != null) {
            imp = improvement;
        }

        return ReportResponse.builder()
                .reportId(report.getId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .recordDays(report.getRecordDays())
                .bloodSugarRecordDays(report.getBloodSugarRecordDays())
                .bloodPressureRecordDays(report.getBloodPressureRecordDays())
                .aiComment(report.getAiComment())
                .overallScore(report.getOverallScore())
                .scoreLabel(scoreLabel)
                .scores(ScoreDetail.builder()
                        .healthManagement(report.getHealthManagementScore())
                        .measurementConsistency(report.getMeasurementConsistencyScore())
                        .lifestyle(report.getLifestyleScore())
                        .build())
                .dailyConditions(dailyConditions)
                .highlights(highlights)
                .improvement(imp)
                .build();
    }
}
