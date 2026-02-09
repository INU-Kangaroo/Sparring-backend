package com.kangaroo.sparring.domain.measurement.dto.res;

import com.kangaroo.sparring.domain.measurement.entity.BloodSugarPrediction;
import com.kangaroo.sparring.domain.measurement.type.RiskLevel;
import com.kangaroo.sparring.domain.measurement.type.TrendLabel;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "혈당 예측 응답")
public class BloodSugarPredictionResponse {

    @Schema(description = "예측 ID", example = "1")
    private Long id;

    @Schema(description = "예측 날짜", example = "2026-01-29")
    private LocalDate predictionDate;

    @Schema(description = "예측 혈당 수치 (mg/dL)", example = "125.50")
    private BigDecimal predictedValue;

    @Schema(description = "신뢰도 점수 (0-100)", example = "85.5")
    private BigDecimal confidenceScore;

    @Schema(description = "추세 라벨", example = "STABLE")
    private TrendLabel trendLabel;

    @Schema(description = "추세 라벨 설명", example = "안정")
    private String trendLabelDescription;

    @Schema(description = "위험도", example = "LOW")
    private RiskLevel riskLevel;

    @Schema(description = "위험도 설명", example = "낮음")
    private String riskLevelDescription;

    @Schema(description = "데이터 부족 여부", example = "false")
    private Boolean isDataInsufficient;

    public static BloodSugarPredictionResponse from(BloodSugarPrediction prediction) {
        return BloodSugarPredictionResponse.builder()
                .id(prediction.getId())
                .predictionDate(prediction.getPredictionDate())
                .predictedValue(prediction.getPredictedValue())
                .confidenceScore(prediction.getConfidenceScore())
                .trendLabel(prediction.getTrendLabel())
                .trendLabelDescription(prediction.getTrendLabel() != null ?
                        prediction.getTrendLabel().getDescription() : null)
                .riskLevel(prediction.getRiskLevel())
                .riskLevelDescription(prediction.getRiskLevel() != null ?
                        prediction.getRiskLevel().getDescription() : null)
                .isDataInsufficient(prediction.getIsDataInsufficient())
                .build();
    }
}