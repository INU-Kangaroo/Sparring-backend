package com.kangaroo.sparring.domain.insight.today.type;

import java.time.LocalTime;
import java.time.temporal.ChronoUnit;

public enum MealTimeSlot {
    MORNING,    // 07:00 ~ 11:59
    AFTERNOON,  // 12:00 ~ 17:59
    EVENING,    // 18:00 ~ 23:59
    NIGHT;      // 00:00 ~ 06:59 → MORNING 캐시 재사용

    public static MealTimeSlot from(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 7 && hour < 12) return MORNING;
        if (hour >= 12 && hour < 18) return AFTERNOON;
        if (hour >= 18) return EVENING;
        return NIGHT; // 00~06시는 NIGHT으로 분류, 캐시 키에서 MORNING으로 매핑
    }

    public String cacheKey() {
        // 새벽은 아침 슬롯 재사용
        return this == NIGHT ? MORNING.name() : this.name();
    }

    // 다음 슬롯 전환까지 남은 초 (TTL 계산용)
    public long secondsUntilNext(LocalTime now) {
        LocalTime next = switch (this) {
            case MORNING -> LocalTime.of(12, 0);
            case AFTERNOON -> LocalTime.of(18, 0);
            case EVENING -> LocalTime.MIDNIGHT;
            case NIGHT -> LocalTime.of(7, 0);
        };
        long seconds = now.until(next, ChronoUnit.SECONDS);
        if (seconds > 0) {
            return seconds;
        }
        return seconds + ChronoUnit.DAYS.getDuration().getSeconds();
    }
}
