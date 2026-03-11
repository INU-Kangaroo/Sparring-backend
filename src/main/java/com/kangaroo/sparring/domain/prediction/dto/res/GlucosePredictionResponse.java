package com.kangaroo.sparring.domain.prediction.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "혈당 예측 응답")
public class GlucosePredictionResponse {

    @Schema(description = "음식명", example = "바질 페스토 샐러드")
    private String foodName;

    @Schema(description = "시간대별 혈당 예측 (0/30/60/120분)")
    private List<ForecastPoint> forecast;

    @Schema(description = "예측 구간 내 최고 혈당 (mg/dL)", example = "148")
    private Double peakGlucose;

    @Schema(description = "최고 혈당 도달 시점 (분)", example = "30")
    private Integer peakOffsetMinutes;

    @Getter
    @Builder
    @Schema(description = "시간대별 혈당 예측 포인트")
    public static class ForecastPoint {

        @Schema(description = "기준 시각으로부터 경과 분", example = "30")
        private Integer offsetMinutes;

        @Schema(description = "예측 혈당 (mg/dL)", example = "148.0")
        private Double predictedGlucose;
    }

    public static GlucosePredictionResponse of(
            String foodName,
            List<ForecastPoint> forecast,
            Double peakGlucose,
            Integer peakOffsetMinutes
    ) {
        return GlucosePredictionResponse.builder()
                .foodName(foodName)
                .forecast(forecast)
                .peakGlucose(peakGlucose)
                .peakOffsetMinutes(peakOffsetMinutes)
                .build();
    }
}
