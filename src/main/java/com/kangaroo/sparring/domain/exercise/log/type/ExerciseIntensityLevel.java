package com.kangaroo.sparring.domain.exercise.log.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseIntensityLevel {
    LOW("저강도"),
    MEDIUM("중강도"),
    HIGH("고강도");

    private final String description;
}
