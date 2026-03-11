package com.kangaroo.sparring.domain.exercise.catalog.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseImpactLevel {
    LOW_IMPACT("저충격"),
    HIGH_IMPACT("고충격");

    private final String description;
}
