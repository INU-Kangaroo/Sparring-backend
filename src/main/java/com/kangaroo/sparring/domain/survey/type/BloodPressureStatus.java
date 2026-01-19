package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum BloodPressureStatus {
    NORMAL("정상"),
    BORDERLINE("경계성"),
    STAGE1("1차 고혈압"),
    STAGE2("2차 고혈압"),
    UNKNOWN("모름");

    private final String description;
}