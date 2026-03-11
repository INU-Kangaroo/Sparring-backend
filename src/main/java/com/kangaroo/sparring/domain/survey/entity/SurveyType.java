package com.kangaroo.sparring.domain.survey.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SurveyType {
    SURVEY("설문");

    private final String description;
}
