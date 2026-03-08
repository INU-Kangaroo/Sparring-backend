package com.kangaroo.sparring.domain.exercise.dto.res;

import com.kangaroo.sparring.domain.exercise.entity.ExerciseLog;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDateTime;

@Getter
@Builder
@Schema(description = "운동 기록 응답")
public class ExerciseLogResponse {

    @Schema(description = "운동 기록 ID")
    private Long id;

    @Schema(description = "사용자 입력 운동명", example = "런닝")
    private String exerciseName;

    @Schema(description = "매핑된 운동명", example = "달리기")
    private String matchedExerciseName;

    @Schema(description = "MET 값", example = "7.0")
    private Double metValue;

    @Schema(description = "운동 시간 (분)", example = "30")
    private Integer durationMinutes;

    @Schema(description = "소모 칼로리", example = "245.0")
    private Double caloriesBurned;

    @Schema(description = "운동 시작 시간")
    private LocalDateTime loggedAt;

    public static ExerciseLogResponse from(ExerciseLog log) {
        return ExerciseLogResponse.builder()
                .id(log.getId())
                .exerciseName(log.getExerciseName())
                .matchedExerciseName(log.getMatchedExerciseName())
                .metValue(log.getMetValue())
                .durationMinutes(log.getDurationMinutes())
                .caloriesBurned(log.getCaloriesBurned())
                .loggedAt(log.getLoggedAt())
                .build();
    }
}
