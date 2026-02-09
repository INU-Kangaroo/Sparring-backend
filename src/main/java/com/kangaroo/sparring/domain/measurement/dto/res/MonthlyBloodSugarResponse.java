package com.kangaroo.sparring.domain.measurement.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "월별 혈당 집계 응답")
public class MonthlyBloodSugarResponse {

    @Schema(description = "연도", example = "2025")
    private Integer year;

    @Schema(description = "월", example = "1")
    private Integer month;

    @Schema(description = "평균 혈당 (mg/dL)", example = "115.5")
    private BigDecimal averageValue;

    @Schema(description = "최고 혈당 (mg/dL)", example = "145")
    private Integer maxValue;

    @Schema(description = "최저 혈당 (mg/dL)", example = "95")
    private Integer minValue;

    @Schema(description = "측정 횟수", example = "62")
    private Long count;

    public static MonthlyBloodSugarResponse of(Integer year, Integer month,
                                               BigDecimal average,
                                               Integer max, Integer min, Long count) {
        return MonthlyBloodSugarResponse.builder()
                .year(year)
                .month(month)
                .averageValue(average)
                .maxValue(max)
                .minValue(min)
                .count(count)
                .build();
    }
}