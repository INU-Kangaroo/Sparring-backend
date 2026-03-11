package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SugarIntakeFreq {
    NONE("주 0회"),
    ONE_TO_TWO_PER_WEEK("주 1~2회"),
    THREE_TO_FOUR_PER_WEEK("주 3~4회"),
    FIVE_TO_SIX_PER_WEEK("주 5~6회"),
    DAILY("매일");

    private final String description;
}