package com.kangaroo.sparring.domain.measurement.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 건강 지표 위험도
 */
@Getter
@RequiredArgsConstructor
public enum RiskLevel {

    LOW("낮음", "정상 범위 내의 안전한 수치입니다"),
    MEDIUM("보통", "주의가 필요한 수치입니다"),
    HIGH("높음", "위험 수치로 관리가 필요합니다"),
    VERY_HIGH("매우 높음", "매우 위험한 수치로 즉각적인 조치가 필요합니다");

    private final String description;
    private final String detail;
}