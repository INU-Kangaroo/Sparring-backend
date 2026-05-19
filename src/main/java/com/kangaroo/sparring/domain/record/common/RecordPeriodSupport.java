package com.kangaroo.sparring.domain.record.common;

import com.kangaroo.sparring.domain.record.common.type.RecordPeriod;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;

public final class RecordPeriodSupport {

    private RecordPeriodSupport() {
    }

    public static DateTimeRange resolve(
            RecordPeriod period,
            LocalDate date,
            Integer year,
            Integer month,
            LocalDate startDate,
            LocalDate endDate,
            Clock kstClock
    ) {
        RecordPeriod resolvedPeriod = period != null ? period : RecordPeriod.DAILY;
        LocalDate today = LocalDate.now(kstClock);

        return switch (resolvedPeriod) {
            case DAILY -> {
                LocalDate target = date != null ? date : today;
                yield new DateTimeRange(target.atStartOfDay(), target.atTime(LocalTime.MAX));
            }
            case WEEKLY -> {
                LocalDate base = date != null ? date : today;
                LocalDate monday = base.with(DayOfWeek.MONDAY);
                LocalDate sunday = monday.plusDays(6);
                yield new DateTimeRange(monday.atStartOfDay(), sunday.atTime(LocalTime.MAX));
            }
            case MONTHLY -> {
                YearMonth ym;
                if (year != null && month != null) {
                    ym = YearMonth.of(year, month);
                } else if (date != null) {
                    ym = YearMonth.from(date);
                } else {
                    ym = YearMonth.from(today);
                }
                yield new DateTimeRange(ym.atDay(1).atStartOfDay(), ym.atEndOfMonth().atTime(LocalTime.MAX));
            }
            case RANGE -> {
                if (startDate == null || endDate == null) {
                    throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
                }
                LocalDateTime start = startDate.atStartOfDay();
                LocalDateTime end = endDate.atTime(LocalTime.MAX);
                if (start.isAfter(end)) {
                    throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
                }
                yield new DateTimeRange(start, end);
            }
        };
    }

    public record DateTimeRange(LocalDateTime start, LocalDateTime end) {
    }
}
