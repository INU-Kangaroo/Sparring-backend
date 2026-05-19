package com.kangaroo.sparring.domain.record.blood.service;

import com.kangaroo.sparring.domain.record.blood.dto.req.BloodSugarLogCreateRequest;
import com.kangaroo.sparring.domain.record.blood.dto.res.BloodSugarLogResponse;
import com.kangaroo.sparring.domain.record.blood.dto.res.BloodSugarPredictionResponse;
import com.kangaroo.sparring.domain.record.blood.dto.res.MonthlyBloodSugarResponse;
import com.kangaroo.sparring.domain.common.health.HealthThresholds;
import com.kangaroo.sparring.domain.record.blood.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.record.blood.entity.BloodSugarPrediction;
import com.kangaroo.sparring.domain.record.blood.repository.BloodSugarLogRepository;
import com.kangaroo.sparring.domain.record.blood.repository.BloodSugarPredictionRepository;
import com.kangaroo.sparring.domain.record.blood.support.MeasurementValidationSupport;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class BloodSugarService {

    private final BloodSugarLogRepository bloodSugarLogRepository;
    private final BloodSugarPredictionRepository bloodSugarPredictionRepository;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final Clock kstClock;

    @Transactional
    public BloodSugarLogResponse createBloodSugarLog(Long userId, BloodSugarLogCreateRequest request) {
        log.info("혈당 측정 기록 등록 시작: userId={}, glucoseLevel={}", userId, request.getGlucoseLevel());

        User user = userLookupService.getUserOrThrow(userId);

        validateGlucoseLevel(request.getGlucoseLevel());

        LocalDateTime measurementTime = LocalDateTime.of(
                request.getMeasurementDate(),
                request.getMeasurementTime()
        );
        MeasurementValidationSupport.validateMeasurementTime(measurementTime, kstClock);

        BloodSugarLog bloodSugarLog = BloodSugarLog.create(
                user,
                request.getGlucoseLevel(),
                measurementTime,
                request.getMeasurementLabel()
        );

        BloodSugarLog savedLog = bloodSugarLogRepository.save(bloodSugarLog);
        log.info("혈당 측정 기록 등록 완료: logId={}", savedLog.getId());

        // AI 예측 연동 전까지는 예측 요청 생략

        return BloodSugarLogResponse.from(savedLog);
    }

    public List<BloodSugarLogResponse> getBloodSugarLogs(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.debug("혈당 측정 기록 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        MeasurementValidationSupport.validateDateRange(startDate, endDate);

        List<BloodSugarLog> logs = bloodSugarLogRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        return logs.stream()
                .map(BloodSugarLogResponse::from)
                .collect(Collectors.toList());
    }

    public List<BloodSugarLogResponse> getBloodSugarLogsByMonth(Long userId, int year, int month) {
        log.debug("월별 혈당 측정 기록 조회: userId={}, year={}, month={}", userId, year, month);

        // 월 유효성 검증
        MeasurementValidationSupport.validateMonthRange(month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        List<BloodSugarLog> logs = bloodSugarLogRepository
                .findByUserIdAndMeasurementTimeBetweenAndIsDeletedFalseOrderByMeasurementTimeAsc(
                        userId, startDateTime, endDateTime);

        return logs.stream()
                .map(BloodSugarLogResponse::from)
                .collect(Collectors.toList());
    }

    public List<BloodSugarLogResponse> getBloodSugarLogsByDate(Long userId, LocalDate date) {
        log.debug("일별 혈당 측정 기록 조회: userId={}, date={}", userId, date);
        MeasurementValidationSupport.DateTimeRange range = MeasurementValidationSupport.toDateTimeRange(date);

        List<BloodSugarLog> logs = bloodSugarLogRepository
                .findByUserIdAndMeasurementTimeBetweenAndIsDeletedFalseOrderByMeasurementTimeAsc(
                        userId, range.start(), range.end());

        return logs.stream()
                .map(BloodSugarLogResponse::from)
                .collect(Collectors.toList());
    }

    public List<BloodSugarPredictionResponse> getBloodSugarPredictions(Long userId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate) {
        log.debug("혈당 예측 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);
        MeasurementValidationSupport.toDateTimeRange(startDate, endDate);

        List<BloodSugarPrediction> predictions =
                bloodSugarPredictionRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        if (predictions.isEmpty()) {
            log.debug("혈당 예측 데이터 없음: userId={}", userId);
        }

        return predictions.stream()
                .map(BloodSugarPredictionResponse::from)
                .collect(Collectors.toList());
    }

    public List<MonthlyBloodSugarResponse> getMonthlyStatistics(Long userId, int year) {
        log.debug("월별 혈당 집계 조회: userId={}, year={}", userId, year);

        MeasurementValidationSupport.DateTimeRange yearRange =
                MeasurementValidationSupport.toYearDateTimeRange(year);

        List<BloodSugarLogRepository.MonthlyBloodSugarStats> stats = bloodSugarLogRepository
                .findMonthlyStatsByUserId(userId, yearRange.start(), yearRange.end());

        if (stats.isEmpty()) {
            log.debug("해당 연도 혈당 데이터 없음: userId={}, year={}", userId, year);
            return new ArrayList<>();
        }

        Map<Integer, BloodSugarLogRepository.MonthlyBloodSugarStats> statsByMonth = stats.stream()
                .filter(item -> item.getMonth() != null)
                .collect(Collectors.toMap(BloodSugarLogRepository.MonthlyBloodSugarStats::getMonth, item -> item));

        List<MonthlyBloodSugarResponse> responses = new ArrayList<>();
        MeasurementValidationSupport.monthsOfYear().forEach(month -> {
            BloodSugarLogRepository.MonthlyBloodSugarStats monthStats = statsByMonth.get(month);

            if (monthStats == null || monthStats.getCount() == null || monthStats.getCount() == 0L) {
                responses.add(MonthlyBloodSugarResponse.builder()
                        .year(year)
                        .month(month)
                        .averageValue(null)
                        .maxValue(null)
                        .minValue(null)
                        .count(0L)
                        .build());
                return;
            }

            BigDecimal avg = monthStats.getAvgValue() == null
                    ? null
                    : BigDecimal.valueOf(monthStats.getAvgValue()).setScale(1, RoundingMode.HALF_UP);

            responses.add(MonthlyBloodSugarResponse.of(
                    year,
                    month,
                    avg,
                    monthStats.getMaxValue(),
                    monthStats.getMinValue(),
                    monthStats.getCount()
            ));
        });

        return responses;
    }

    private void requestPrediction(Long userId) {
        log.debug("AI 예측 요청: userId={}", userId);
    }

    /**
     * 혈당 수치 검증
     */
    private void validateGlucoseLevel(Integer glucoseLevel) {
        if (glucoseLevel == null
                || glucoseLevel < HealthThresholds.BLOOD_SUGAR_MIN
                || glucoseLevel > HealthThresholds.BLOOD_SUGAR_MAX) {
            throw new CustomException(ErrorCode.INVALID_GLUCOSE_LEVEL);
        }
    }
}
