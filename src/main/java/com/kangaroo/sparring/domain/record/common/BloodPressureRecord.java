package com.kangaroo.sparring.domain.record.common;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class BloodPressureRecord implements TemporalRecord {
    private final Integer systolic;
    private final Integer diastolic;
    private final LocalDateTime measuredAt;
    private final String measurementLabel;

    public BloodPressureRecord(Integer systolic, Integer diastolic, LocalDateTime measuredAt) {
        this(systolic, diastolic, measuredAt, null);
    }

    public BloodPressureRecord(Integer systolic, Integer diastolic, LocalDateTime measuredAt, String measurementLabel) {
        this.systolic = systolic;
        this.diastolic = diastolic;
        this.measuredAt = measuredAt;
        this.measurementLabel = measurementLabel;
    }

    @Override
    public LocalDateTime occurredAt() {
        return measuredAt;
    }
}
