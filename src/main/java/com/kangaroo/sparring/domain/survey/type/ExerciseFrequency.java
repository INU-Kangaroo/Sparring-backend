package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExerciseFrequency {
    ZERO("0회"),
    ONE_TO_TWO("1~2회"),
    TWO_TO_THREE("2~3회"),
    THREE_TO_FOUR("3~4회"),
    FOUR_TO_FIVE("4~5회");

    private final String description;
}