package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum StressLevel {
    LOW("낮음"),
    MEDIUM("중간"),
    HIGH("높음");

    private final String description;
}