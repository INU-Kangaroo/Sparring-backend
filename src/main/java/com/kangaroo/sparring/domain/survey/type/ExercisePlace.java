package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum ExercisePlace {
    GYM_FACILITY("운동시설 위주"),
    HOME("집"),
    OUTDOOR("야외"),
    WORK_SCHOOL("직장/학교");

    private final String description;
}
