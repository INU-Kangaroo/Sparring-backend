package com.kangaroo.sparring.domain.home.dto.res;

import com.kangaroo.sparring.domain.insight.today.type.InsightType;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepTodayResponse;
import com.kangaroo.sparring.domain.user.dto.res.UserHomeCardResponse;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "메인 화면 통합 응답")
public class MainHomeResponse {

    @Schema(description = "상단 프로필 카드")
    private UserHomeCardResponse profileCard;

    @Schema(description = "오늘의 한마디")
    private TodayInsight todayInsight;

    @Schema(description = "최근 7일 혈당 그래프")
    private BloodSugarChart bloodSugarChart;

    @Schema(description = "오늘 걸음수")
    private StepTodayResponse steps;

    @Getter
    @AllArgsConstructor
    @Builder
    public static class TodayInsight {
        private InsightType type;
        private String message;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class BloodSugarChart {
        private LocalDate startDate;
        private LocalDate endDate;
        private List<Point> points;
    }

    @Getter
    @AllArgsConstructor
    @Builder
    public static class Point {
        private LocalDate date;
        private BigDecimal averageBloodSugarMgDl;
    }

}
