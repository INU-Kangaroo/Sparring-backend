package com.kangaroo.sparring.domain.measurement.dto.res;

import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.type.BloodPressureMeasurementType;
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

    @Schema(description = "혈압 기록 ID", example = "1")
    private Long id;

    @Schema(description = "수축기 혈압 (mmHg)", example = "120")
    private Integer systolic;

    @Schema(description = "이완기 혈압 (mmHg)", example = "80")
    private Integer diastolic;

    @Schema(description = "심박수 (bpm)", example = "72")
    private Integer heartRate;

    @Schema(description = "메모", example = "아침 기상 후 측정")
    private String note;

    @Schema(description = "측정 시간", example = "2026-01-28T08:00:00")
    private LocalDateTime measuredAt;

    @Schema(description = "측정 시점", example = "MORNING")
    private BloodPressureMeasurementType measurementType;

    @Schema(description = "측정 시점 설명", example = "아침")
    private String measurementTypeDescription;

    @Schema(description = "생성일시", example = "2026-01-28T08:00:00")
    private LocalDateTime createdAt;

    public static BloodPressureLogResponse from(BloodPressureLog log) {
        return BloodPressureLogResponse.builder()
                .id(log.getId())
                .systolic(log.getSystolic())
                .diastolic(log.getDiastolic())
                .heartRate(log.getHeartRate())
                .note(log.getNote())
                .measuredAt(log.getMeasuredAt())
                .measurementType(log.getMeasurementType())
                .measurementTypeDescription(log.getMeasurementType().getDescription())
                .createdAt(log.getCreatedAt())
                .build();
    }
}
