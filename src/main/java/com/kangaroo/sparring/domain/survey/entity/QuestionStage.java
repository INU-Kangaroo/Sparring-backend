package com.kangaroo.sparring.domain.survey.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionStage {
    BASIC("회원가입"),
    DETAILED("설문");

    private final String description;
}
