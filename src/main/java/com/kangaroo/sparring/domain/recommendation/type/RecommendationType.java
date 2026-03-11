package com.kangaroo.sparring.domain.recommendation.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum RecommendationType {
    EXERCISE("운동"),
    SUPPLEMENT("영양제"),
    MEAL("식사");

    private final String description;
}