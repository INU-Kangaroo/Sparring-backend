package com.kangaroo.sparring.domain.exercise.catalog.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseCategory {
    AEROBIC("유산소"),
    STRENGTH("근력"),
    FLEXIBILITY("유연성");

    private final String description;
}
