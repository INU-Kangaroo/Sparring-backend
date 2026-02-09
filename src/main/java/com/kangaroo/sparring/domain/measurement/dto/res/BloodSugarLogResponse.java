package com.kangaroo.sparring.domain.measurement.dto.res;

import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.measurement.type.BloodSugarMeasurementType;
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

    @Schema(description = "혈당 기록 ID", example = "1")
    private Long id;

    @Schema(description = "혈당 수치 (mg/dL)", example = "120")
    private Integer glucoseLevel;

    @Schema(description = "측정 시간", example = "2026-01-28T08:00:00")
    private LocalDateTime measurementTime;

    @Schema(description = "측정 타입", example = "FASTING")
    private BloodSugarMeasurementType measurementType;

    @Schema(description = "측정 타입 설명", example = "공복")
    private String measurementTypeDescription;

    @Schema(description = "메모", example = "아침 식사 전 측정")
    private String note;

    @Schema(description = "생성일시", example = "2026-01-28T08:00:00")
    private LocalDateTime createdAt;

    public static BloodSugarLogResponse from(BloodSugarLog log) {
        return BloodSugarLogResponse.builder()
                .id(log.getId())
                .glucoseLevel(log.getGlucoseLevel())
                .measurementTime(log.getMeasurementTime())
                .measurementType(log.getMeasurementType())
                .measurementTypeDescription(log.getMeasurementType().getDescription())
                .note(log.getNote())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
