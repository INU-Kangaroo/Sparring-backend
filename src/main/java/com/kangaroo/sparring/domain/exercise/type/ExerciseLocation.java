package com.kangaroo.sparring.domain.exercise.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseLocation {
    INDOOR("실내"),
    OUTDOOR("실외");

    private final String description;
}
