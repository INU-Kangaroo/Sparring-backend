package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum DrinkingFrequency {
    NONE("없음"),
    ONE_TO_TWO_PER_WEEK("주 1~2회"),
    THREE_OR_MORE_PER_WEEK("주 3회 이상");

    private final String description;
}