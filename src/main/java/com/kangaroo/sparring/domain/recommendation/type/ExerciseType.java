package com.kangaroo.sparring.domain.recommendation.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseType {
    CARDIAC("유산소"),
    STRENGTH("근력");

    private final String description;
}