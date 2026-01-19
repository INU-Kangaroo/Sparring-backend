package com.kangaroo.sparring.domain.survey.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum SurveyType {
    BASIC("기본 설문"),
    DETAILED("상세 설문");

    private final String description;
}