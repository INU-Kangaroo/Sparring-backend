package com.kangaroo.sparring.domain.measurement.dto.req;

import com.fasterxml.jackson.annotation.JsonFormat;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalTime;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "혈당 측정 기록 등록 요청 (모든 시간은 KST 기준)")
public class BloodSugarLogCreateRequest {

    @Schema(description = "혈당 수치 (mg/dL)", example = "120", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "혈당 수치는 필수입니다")
    @Positive(message = "혈당 수치는 양수여야 합니다")
    private Integer glucoseLevel;

    @Schema(
            description = "측정 날짜 (KST)",
            example = "2026-01-28",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "측정 날짜는 필수입니다")
    private LocalDate measurementDate;

    @Schema(
            description = "측정 시간 (KST, HH:mm)",
            example = "08:00",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @JsonFormat(pattern = "HH:mm")
    @NotNull(message = "측정 시간은 필수입니다")
    private LocalTime measurementTime;

    @Schema(
            description = "측정 라벨 (예: 공복, 식전, 식후, 취침 전, 운동 후 등)",
            example = "공복",
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotBlank(message = "측정 라벨은 필수입니다")
    @Size(max = 50, message = "측정 라벨은 50자 이내로 입력해주세요")
    private String measurementLabel;

}
