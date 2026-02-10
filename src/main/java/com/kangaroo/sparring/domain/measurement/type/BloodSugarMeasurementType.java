package com.kangaroo.sparring.domain.measurement.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 혈당 측정 타입
 * @deprecated enum 대신 String measurementLabel 사용으로 변경됨 (2026-02-10)
 */
@Deprecated
@Getter
@RequiredArgsConstructor
public enum BloodSugarMeasurementType {
    FASTING("공복", "아침 식사 전 8시간 이상 금식 후 측정"),
    BEFORE_MEAL("식전", "식사 직전 측정"),
    AFTER_MEAL("식후", "식사 후 2시간 측정"),
    BEFORE_SLEEP("취침 전", "잠들기 전 측정");

    private final String description;
    private final String detail;
}
