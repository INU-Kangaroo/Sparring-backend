package com.kangaroo.sparring.domain.measurement.dto.res;

import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "혈당 측정 기록 응답")
public class BloodSugarLogResponse {

    @Schema(description = "혈당 수치 (mg/dL)", example = "120")
    private Integer glucoseLevel;

    @Schema(description = "측정 시간", example = "2026-01-28T08:00:00")
    private LocalDateTime measuredAt;

    @Schema(description = "측정 라벨", example = "공복")
    private String measurementLabel;

    public static BloodSugarLogResponse from(BloodSugarLog log) {
        return BloodSugarLogResponse.builder()
                .glucoseLevel(log.getGlucoseLevel())
                .measuredAt(log.getMeasurementTime())
                .measurementLabel(log.getMeasurementLabel())
                .build();
    }
}
