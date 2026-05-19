package com.kangaroo.sparring.domain.record.common;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BloodSugarRecord implements TemporalRecord {
    private final Integer glucoseLevel;
    private final LocalDateTime measurementTime;
    private final String measurementLabel;

    public BloodSugarRecord(Integer glucoseLevel, LocalDateTime measurementTime, String measurementLabel) {
        this.glucoseLevel = glucoseLevel;
        this.measurementTime = measurementTime;
        this.measurementLabel = measurementLabel;
    }

    @Override
    public LocalDateTime occurredAt() {
        return measurementTime;
    }
}
