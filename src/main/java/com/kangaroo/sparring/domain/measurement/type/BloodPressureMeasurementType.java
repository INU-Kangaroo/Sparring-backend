package com.kangaroo.sparring.domain.measurement.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 혈압 측정 타입
 */
@Getter
@RequiredArgsConstructor
public enum BloodPressureMeasurementType {
    MORNING("아침", "기상 후 1시간 이내 측정"),
    BEDTIME("취침 전", "잠들기 전 측정");

    private final String description;
    private final String detail;
}
