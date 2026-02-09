package com.kangaroo.sparring.domain.measurement.dto.req;

import com.kangaroo.sparring.domain.measurement.type.BloodPressureMeasurementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "혈압 측정 기록 등록 요청")
public class BloodPressureLogCreateRequest {

    @Schema(description = "수축기 혈압 (mmHg)", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "수축기 혈압은 필수입니다")
    @Min(value = 50, message = "수축기 혈압은 50 이상이어야 합니다")
    @Max(value = 300, message = "수축기 혈압은 300 이하여야 합니다")
    private Integer systolic;

    @Schema(description = "이완기 혈압 (mmHg)", example = "80", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "이완기 혈압은 필수입니다")
    @Min(value = 30, message = "이완기 혈압은 30 이상이어야 합니다")
    @Max(value = 200, message = "이완기 혈압은 200 이하여야 합니다")
    private Integer diastolic;

    @Schema(description = "심박수 (bpm)", example = "72")
    @Min(value = 30, message = "심박수는 30 이상이어야 합니다")
    @Max(value = 250, message = "심박수는 250 이하여야 합니다")
    private Integer heartRate;

    @Schema(description = "메모", example = "아침 기상 후 측정")
    @Size(max = 500, message = "메모는 500자 이내로 입력해주세요")
    private String note;

    @Schema(
            description = "측정 시간",
            example = "2026-01-28T08:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "측정 시간은 필수입니다")
    private LocalDateTime measuredAt;

    @Schema(
            description = """
            측정 시점
            
            - MORNING: 아침 (기상 후 1시간 이내)
            - BEDTIME: 취침 전 (잠들기 전)
            """,
            example = "MORNING",
            allowableValues = {"MORNING", "BEDTIME"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "측정 시점은 필수입니다")
    private BloodPressureMeasurementType measurementType;
}
