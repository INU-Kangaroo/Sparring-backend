package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseDuration {
    UNDER_1H("1시간 미만"),
    ONE_TO_TWO_H("1~2시간"),
    TWO_TO_THREE_H("2~3시간"),
    OVER_3H("3시간 이상");

    private final String description;
}