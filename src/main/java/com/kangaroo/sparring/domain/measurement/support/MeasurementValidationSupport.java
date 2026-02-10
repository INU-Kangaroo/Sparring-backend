package com.kangaroo.sparring.domain.measurement.support;

import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;

import java.time.LocalDateTime;
import java.time.ZoneId;

public final class MeasurementValidationSupport {

    private static final ZoneId KOREA_ZONE = ZoneId.of("Asia/Seoul");

    private MeasurementValidationSupport() {
    }

    public static void validateMeasurementTime(LocalDateTime measurementTime) {
        if (measurementTime.isAfter(LocalDateTime.now(KOREA_ZONE))) {
            throw new CustomException(ErrorCode.MEASUREMENT_TIME_FUTURE);
        }
    }

    public static void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    public static void validateMonthRange(int month) {
        if (month < 1 || month > 12) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    public static void validateYearRange(int year) {
        if (year < 1900 || year > 2100) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }
}
