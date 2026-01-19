package com.kangaroo.sparring.domain.survey.entity;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum QuestionType {
    TEXT("텍스트"),
    NUMBER("숫자"),
    SINGLE_CHOICE("단일 선택"),
    MULTIPLE_CHOICE("다중 선택");

    private final String description;
}