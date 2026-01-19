package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SleepQuality {
    GOOD("좋음"),
    NORMAL("보통"),
    BAD("나쁨");

    private final String description;
}