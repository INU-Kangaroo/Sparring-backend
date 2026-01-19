package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BloodSugarStatus {
    NORMAL("정상"),
    BORDERLINE("경계성"),
    TYPE1("제1형"),
    TYPE2("제2형"),
    UNKNOWN("모름");

    private final String description;
}