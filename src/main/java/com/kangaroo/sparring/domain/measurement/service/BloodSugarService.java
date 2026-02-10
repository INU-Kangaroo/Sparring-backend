package com.kangaroo.sparring.domain.measurement.service;

import com.kangaroo.sparring.domain.measurement.dto.req.BloodSugarLogCreateRequest;
import com.kangaroo.sparring.domain.measurement.dto.res.BloodSugarLogResponse;
import com.kangaroo.sparring.domain.measurement.dto.res.BloodSugarPredictionResponse;
import com.kangaroo.sparring.domain.measurement.dto.res.MonthlyBloodSugarResponse;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarPrediction;
import com.kangaroo.sparring.domain.measurement.repository.BloodSugarLogRepository;
import com.kangaroo.sparring.domain.measurement.repository.BloodSugarPredictionRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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

    @Transactional
    public BloodSugarLogResponse createBloodSugarLog(Long userId, BloodSugarLogCreateRequest request) {
        log.info("혈당 측정 기록 등록 시작: userId={}, glucoseLevel={}", userId, request.getGlucoseLevel());

        User user = findUserById(userId);

        validateGlucoseLevel(request.getGlucoseLevel());
        validateMeasurementTime(request.getMeasurementTime());

        BloodSugarLog bloodSugarLog = BloodSugarLog.create(
                user,
                request.getGlucoseLevel(),
                request.getMeasurementTime(),
                request.getMeasurementType(),
                request.getNote()
        );

        BloodSugarLog savedLog = bloodSugarLogRepository.save(bloodSugarLog);
        log.info("혈당 측정 기록 등록 완료: logId={}", savedLog.getId());

        // AI 예측 연동 전까지는 예측 요청 생략

        return BloodSugarLogResponse.from(savedLog);
    }

    public List<BloodSugarLogResponse> getBloodSugarLogs(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("혈당 측정 기록 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        validateDateRange(startDate, endDate);

        List<BloodSugarLog> logs = bloodSugarLogRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        return logs.stream()
                .map(BloodSugarLogResponse::from)
                .collect(Collectors.toList());
    }

    public List<BloodSugarLogResponse> getBloodSugarLogsByMonth(Long userId, int year, int month) {
        log.info("월별 혈당 측정 기록 조회: userId={}, year={}, month={}", userId, year, month);

        // 월 유효성 검증
        if (month < 1 || month > 12) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

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
        log.info("일별 혈당 측정 기록 조회: userId={}, date={}", userId, date);

        if (date == null) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime startDateTime = date.atStartOfDay();
        LocalDateTime endDateTime = date.atTime(LocalTime.MAX);
        validateDateRange(startDateTime, endDateTime);

        List<BloodSugarLog> logs = bloodSugarLogRepository
                .findByUserIdAndMeasurementTimeBetweenAndIsDeletedFalseOrderByMeasurementTimeAsc(
                        userId, startDateTime, endDateTime);

        return logs.stream()
                .map(BloodSugarLogResponse::from)
                .collect(Collectors.toList());
    }

    public List<BloodSugarPredictionResponse> getBloodSugarPredictions(Long userId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate) {
        log.info("혈당 예측 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        if (startDate == null || endDate == null) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);
        validateDateRange(startDateTime, endDateTime);

        List<BloodSugarPrediction> predictions =
                bloodSugarPredictionRepository.findByUserIdAndDateRange(userId, startDate, endDate);

        if (predictions.isEmpty()) {
            log.info("혈당 예측 데이터 없음: userId={}", userId);
        }

        return predictions.stream()
                .map(BloodSugarPredictionResponse::from)
                .collect(Collectors.toList());
    }

    public List<MonthlyBloodSugarResponse> getMonthlyStatistics(Long userId, int year) {
        log.info("월별 혈당 집계 조회: userId={}, year={}", userId, year);

        if (year < 1900 || year > 2100) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime startDateTime = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime endDateTime = LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);

        List<BloodSugarLogRepository.MonthlyBloodSugarStats> stats = bloodSugarLogRepository
                .findMonthlyStatsByUserId(userId, startDateTime, endDateTime);

        if (stats.isEmpty()) {
            log.info("해당 연도 혈당 데이터 없음: userId={}, year={}", userId, year);
            return new ArrayList<>();
        }

        Map<Integer, BloodSugarLogRepository.MonthlyBloodSugarStats> statsByMonth = stats.stream()
                .filter(item -> item.getMonth() != null)
                .collect(Collectors.toMap(BloodSugarLogRepository.MonthlyBloodSugarStats::getMonth, item -> item));

        List<MonthlyBloodSugarResponse> responses = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
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
                continue;
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
        }

        return responses;
    }

    private void requestPrediction(Long userId) {
        log.info("AI 예측 요청: userId={}", userId);
    }

    /**
     * 혈당 수치 검증
     */
    private void validateGlucoseLevel(Integer glucoseLevel) {
        if (glucoseLevel == null || glucoseLevel < 20 || glucoseLevel > 600) {
            throw new CustomException(ErrorCode.INVALID_GLUCOSE_LEVEL);
        }
    }

    /**
     * 측정 시간 검증
     */
    private void validateMeasurementTime(LocalDateTime measurementTime) {
        if (measurementTime.isAfter(LocalDateTime.now())) {
            throw new CustomException(ErrorCode.MEASUREMENT_TIME_FUTURE);
        }
    }

    /**
     * 날짜 범위 검증
     */
    private void validateDateRange(LocalDateTime startDate, LocalDateTime endDate) {
        if (startDate.isAfter(endDate)) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
