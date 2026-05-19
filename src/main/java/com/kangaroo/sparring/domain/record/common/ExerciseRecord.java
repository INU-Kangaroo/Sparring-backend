package com.kangaroo.sparring.domain.record.common;

import lombok.Getter;

import java.time.LocalDateTime;

@Getter
public class ExerciseRecord implements TemporalRecord {
    private final LocalDateTime loggedAt;
    private final Double metValue;

    public ExerciseRecord(LocalDateTime loggedAt, Double metValue) {
        this.loggedAt = loggedAt;
        this.metValue = metValue;
    }

    @Override
    public LocalDateTime occurredAt() {
        return loggedAt;
    }
}
