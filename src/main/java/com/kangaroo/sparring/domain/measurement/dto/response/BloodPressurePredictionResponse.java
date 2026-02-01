package com.kangaroo.sparring.domain.measurement.dto.response;

import com.kangaroo.sparring.domain.measurement.entity.BloodPressurePrediction;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "혈압 예측 응답")
public class BloodPressurePredictionResponse {

    @Schema(description = "예측 ID", example = "1")
    private Long id;

    @Schema(description = "예측 시간 (모델 실행 시점)", example = "2026-01-28T08:00:00")
    private LocalDateTime predictedAt;

    @Schema(description = "예측 대상 시각", example = "2026-01-29T08:00:00")
    private LocalDateTime targetDatetime;

    @Schema(description = "수축기 혈압 예측 (mmHg)", example = "125")
    private Integer predictedSystolic;

    @Schema(description = "이완기 혈압 예측 (mmHg)", example = "82")
    private Integer predictedDiastolic;

    public static BloodPressurePredictionResponse from(BloodPressurePrediction prediction) {
        return BloodPressurePredictionResponse.builder()
                .id(prediction.getId())
                .predictedAt(prediction.getPredictedAt())
                .targetDatetime(prediction.getTargetDatetime())
                .predictedSystolic(prediction.getPredictedSystolic())
                .predictedDiastolic(prediction.getPredictedDiastolic())
                .build();
    }
}