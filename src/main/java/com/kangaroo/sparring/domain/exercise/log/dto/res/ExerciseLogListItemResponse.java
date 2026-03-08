package com.kangaroo.sparring.domain.exercise.log.dto.res;

import com.kangaroo.sparring.domain.exercise.log.entity.ExerciseLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "운동 기록 목록 응답")
public class ExerciseLogListItemResponse {

    @Schema(description = "운동 기록 ID")
    private Long id;

    @Schema(description = "운동명", example = "런닝")
    private String exerciseName;

    @Schema(description = "운동 시간 (분)", example = "30")
    private Integer durationMinutes;

    @Schema(description = "소모 칼로리", example = "245.0")
    private Double caloriesBurned;

    @Schema(description = "운동 시작 시간")
    private LocalDateTime loggedAt;

    public static ExerciseLogListItemResponse from(ExerciseLog log) {
        return ExerciseLogListItemResponse.builder()
                .id(log.getId())
                .exerciseName(log.getExerciseName())
                .durationMinutes(log.getDurationMinutes())
                .caloriesBurned(log.getCaloriesBurned())
                .loggedAt(log.getLoggedAt())
                .build();
    }
}
