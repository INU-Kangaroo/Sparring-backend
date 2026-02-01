package com.kangaroo.sparring.domain.measurement.dto.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "월별 혈압 집계 응답")
public class MonthlyBloodPressureResponse {

    @Schema(description = "연도", example = "2025")
    private Integer year;

    @Schema(description = "월", example = "1")
    private Integer month;

    @Schema(description = "수축기 혈압 통계")
    private Stat systolic;

    @Schema(description = "이완기 혈압 통계")
    private Stat diastolic;

    @Schema(description = "측정 횟수", example = "40")
    private Long count;

    public static MonthlyBloodPressureResponse of(Integer year, Integer month,
                                                  Stat systolic,
                                                  Stat diastolic,
                                                  Long count) {
        return MonthlyBloodPressureResponse.builder()
                .year(year)
                .month(month)
                .systolic(systolic)
                .diastolic(diastolic)
                .count(count)
                .build();
    }

    @Getter
    @AllArgsConstructor
    @Builder
    @Schema(description = "혈압 통계")
    public static class Stat {

        @Schema(description = "평균값", example = "118.2")
        private BigDecimal averageValue;

        @Schema(description = "최고값", example = "140")
        private Integer maxValue;

        @Schema(description = "최저값", example = "100")
        private Integer minValue;

        public static Stat of(BigDecimal averageValue, Integer maxValue, Integer minValue) {
            return Stat.builder()
                    .averageValue(averageValue)
                    .maxValue(maxValue)
                    .minValue(minValue)
                    .build();
        }
    }
}
