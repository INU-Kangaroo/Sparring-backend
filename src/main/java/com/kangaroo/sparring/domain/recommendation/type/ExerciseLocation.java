package com.kangaroo.sparring.domain.recommendation.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseLocation {
    INDOOR("실내"),
    OUTDOOR("야외"),
    GYM("헬스장");

    private final String description;
}