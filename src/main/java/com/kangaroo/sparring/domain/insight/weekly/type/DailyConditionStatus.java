package com.kangaroo.sparring.domain.insight.weekly.type;

/**
 * 하루 종합 컨디션 상태
 */
public enum DailyConditionStatus {
    GOOD,      // 😊 75점 이상
    CAUTION,   // 😐 50~74점
    BAD,       // 😟 50점 미만
    NO_DATA    // 😶 데이터 없음
}
