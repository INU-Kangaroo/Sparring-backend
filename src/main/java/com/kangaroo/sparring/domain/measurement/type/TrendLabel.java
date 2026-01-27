package com.kangaroo.sparring.domain.measurement.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * 건강 지표 추세 라벨
 */
@Getter
@RequiredArgsConstructor
public enum TrendLabel {

    INCREASING("상승", "수치가 증가하는 추세입니다"),
    STABLE("안정", "수치가 안정적입니다"),
    DECREASING("하락", "수치가 감소하는 추세입니다");

    private final String description;
    private final String detail;
}