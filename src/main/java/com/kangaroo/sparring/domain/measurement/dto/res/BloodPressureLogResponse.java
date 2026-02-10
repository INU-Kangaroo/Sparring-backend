package com.kangaroo.sparring.domain.measurement.dto.res;

import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "혈압 측정 기록 응답")
public class BloodPressureLogResponse {

    @Schema(description = "수축기 혈압 (mmHg)", example = "120")
    private Integer systolic;

    @Schema(description = "이완기 혈압 (mmHg)", example = "80")
    private Integer diastolic;

    @Schema(description = "심박수 (bpm)", example = "72")
    private Integer heartRate;

    @Schema(description = "측정 시간", example = "2026-01-28T08:00:00")
    private LocalDateTime measuredAt;

    @Schema(description = "측정 라벨", example = "아침")
    private String measurementLabel;

    public static BloodPressureLogResponse from(BloodPressureLog log) {
        return BloodPressureLogResponse.builder()
                .systolic(log.getSystolic())
                .diastolic(log.getDiastolic())
                .heartRate(log.getHeartRate())
                .measuredAt(log.getMeasuredAt())
                .measurementLabel(log.getMeasurementLabel())
                .build();
    }
}
