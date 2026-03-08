package com.kangaroo.sparring.domain.report.dto.res;

import com.kangaroo.sparring.domain.report.entity.Report;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.DayOfWeek;
import java.time.temporal.ChronoUnit;
import java.time.temporal.TemporalAdjusters;

@Getter
@Builder
@Schema(
        description = "지난 보고서 목록 항목",
        example = """
                {
                  "reportId": 12,
                  "weekLabel": "3월 1주",
                  "startDate": "2026-03-02",
                  "endDate": "2026-03-08",
                  "overallScore": 78,
                  "bloodSugarRecordDays": 6,
                  "bloodPressureRecordDays": 7
                }
                """
)
public class ReportListItemResponse {

    @Schema(description = "보고서 ID", example = "12")
    private Long reportId;

    @Schema(description = "주 라벨 (예: 3월 1주)", example = "3월 1주")
    private String weekLabel;

    @Schema(description = "시작일", example = "2026-03-02")
    private LocalDate startDate;

    @Schema(description = "종료일", example = "2026-03-08")
    private LocalDate endDate;

    @Schema(description = "종합 점수", example = "78")
    private Integer overallScore;

    @Schema(description = "혈당 기록한 날 수", example = "6")
    private Integer bloodSugarRecordDays;

    @Schema(description = "혈압 기록한 날 수", example = "7")
    private Integer bloodPressureRecordDays;

    public static ReportListItemResponse from(Report report) {
        return ReportListItemResponse.builder()
                .reportId(report.getId())
                .weekLabel(buildWeekLabel(report.getStartDate()))
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .overallScore(report.getOverallScore())
                .bloodSugarRecordDays(report.getBloodSugarRecordDays())
                .bloodPressureRecordDays(report.getBloodPressureRecordDays())
                .build();
    }

    // "3월 1주" 형식 생성
    private static String buildWeekLabel(LocalDate startDate) {
        int month = startDate.getMonthValue();
        LocalDate firstMonday = startDate.withDayOfMonth(1)
                .with(TemporalAdjusters.nextOrSame(DayOfWeek.MONDAY));
        int weekOfMonth;
        if (startDate.isBefore(firstMonday)) {
            weekOfMonth = 1;
        } else {
            long weeks = ChronoUnit.WEEKS.between(firstMonday, startDate);
            weekOfMonth = (int) weeks + 1;
        }
        return month + "월 " + weekOfMonth + "주";
    }
}
