package com.kangaroo.sparring.domain.common.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseIntensity {
    LOW("저강도"),
    MEDIUM("중강도"),
    HIGH("고강도");

    private final String description;
}
