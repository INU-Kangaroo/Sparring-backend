package com.kangaroo.sparring.domain.record.common;

import com.kangaroo.sparring.domain.record.insulin.type.InsulinEventType;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
public class InsulinRecord implements TemporalRecord {
    private final InsulinEventType eventType;
    private final BigDecimal dose;
    private final LocalDateTime usedAt;
    private final String insulinType;
    private final boolean tempBasalActive;
    private final BigDecimal tempBasalValue;

    public InsulinRecord(
            InsulinEventType eventType,
            BigDecimal dose,
            LocalDateTime usedAt,
            String insulinType,
            boolean tempBasalActive,
            BigDecimal tempBasalValue
    ) {
        this.eventType = eventType;
        this.dose = dose;
        this.usedAt = usedAt;
        this.insulinType = insulinType;
        this.tempBasalActive = tempBasalActive;
        this.tempBasalValue = tempBasalValue;
    }

    @Override
    public LocalDateTime occurredAt() {
        return usedAt;
    }
}
