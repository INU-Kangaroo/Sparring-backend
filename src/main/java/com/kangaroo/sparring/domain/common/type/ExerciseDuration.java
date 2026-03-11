package com.kangaroo.sparring.domain.common.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseDuration {
    UNDER_1H("1시간 미만", 60),
    ONE_TO_TWO_H("1~2시간", 120),
    TWO_TO_THREE_H("2~3시간", 180),
    OVER_3H("3시간 이상", null),
    SHORT("30분 이하", 30),
    MEDIUM("30분~1시간", 60),
    LONG("1시간 이상", 120);

    private final String description;
    private final Integer maxMinutes;
}
