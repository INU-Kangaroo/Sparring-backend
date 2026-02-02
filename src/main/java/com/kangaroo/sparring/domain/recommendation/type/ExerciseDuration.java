package com.kangaroo.sparring.domain.recommendation.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseDuration {
    SHORT("30분 이하", 30),
    MEDIUM("30분~1시간", 60),
    LONG("1시간 이상", 120);

    private final String description;
    private final int maxMinutes;
}