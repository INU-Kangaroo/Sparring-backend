package com.kangaroo.sparring.domain.measurement.dto.req;

import com.kangaroo.sparring.domain.measurement.type.BloodSugarMeasurementType;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "혈당 측정 기록 등록 요청")
public class BloodSugarLogCreateRequest {

    @Schema(description = "혈당 수치 (mg/dL)", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "혈당 수치는 필수입니다")
    @Positive(message = "혈당 수치는 양수여야 합니다")
    private Integer glucoseLevel;

    @Schema(
            description = "측정 시간",
            example = "2026-01-28T08:00:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "측정 시간은 필수입니다")
    private LocalDateTime measurementTime;

    @Schema(
            description = """
            측정 타입
            
            - FASTING: 공복 (아침 식사 전 8시간 이상 금식 후)
            - BEFORE_MEAL: 식전 (식사 직전)
            - AFTER_MEAL: 식후 (식사 후 2시간)
            - BEFORE_SLEEP: 취침 전 (잠들기 전)
            """,
            example = "FASTING",
            allowableValues = {"FASTING", "BEFORE_MEAL", "AFTER_MEAL", "BEFORE_SLEEP"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "측정 타입은 필수입니다")
    private BloodSugarMeasurementType measurementType;

    @Schema(description = "메모", example = "아침 식사 전 측정")
    @Size(max = 500, message = "메모는 500자 이내로 입력해주세요")
    private String note;
}
