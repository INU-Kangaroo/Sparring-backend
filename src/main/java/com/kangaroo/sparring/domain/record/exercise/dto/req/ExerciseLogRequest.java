package com.kangaroo.sparring.domain.record.exercise.dto.req;

import com.kangaroo.sparring.domain.common.type.ExerciseIntensity;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Schema(description = "운동 기록 요청")
public class ExerciseLogRequest {

    @NotBlank
    @Schema(description = "운동 종류", example = "런닝")
    private String exerciseName;

    @NotNull
    @Min(1)
    @Schema(description = "운동 시간 (분)", example = "30")
    private Integer durationMinutes;

    @NotNull
    @Schema(description = "운동 강도 (LOW/MODERATE/HIGH)")
    private ExerciseIntensity intensity;

    @NotNull
    @Schema(description = "운동 시작 시간", example = "2025-01-01T19:00:00")
    private LocalDateTime loggedAt;
}
