package com.kangaroo.sparring.domain.prediction.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@AllArgsConstructor
@Schema(description = "혈당 예측 응답")
public class GlucosePredictionResponse {

    @Schema(description = "예측된 최고 혈당값(mg/dL)", example = "129.0")
    private Double peakGlucose;

    @Schema(description = "최고 혈당 도달 시점(분)", example = "75")
    private Integer peakMinute;

    @Schema(description = "시간대별 혈당 곡선")
    private List<CurvePoint> curve;

    @Getter
    @Builder
    @AllArgsConstructor
    @Schema(description = "시간대별 혈당 포인트")
    public static class CurvePoint {

        @Schema(description = "예측 시점(분)", example = "30")
        private Integer minute;

        @Schema(description = "해당 시점 혈당값(mg/dL)", example = "101.1")
        private Double glucose;
    }
}
