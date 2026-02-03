package com.kangaroo.sparring.domain.measurement.service;

import com.kangaroo.sparring.domain.measurement.dto.request.BloodPressureLogCreateRequest;
import com.kangaroo.sparring.domain.measurement.dto.response.BloodPressureLogResponse;
import com.kangaroo.sparring.domain.measurement.dto.response.BloodPressurePredictionResponse;
import com.kangaroo.sparring.domain.measurement.dto.response.MonthlyBloodPressureResponse;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressurePrediction;
import com.kangaroo.sparring.domain.measurement.repository.BloodPressureLogRepository;
import com.kangaroo.sparring.domain.measurement.repository.BloodPressurePredictionRepository;
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
public class BloodPressureService {

    private final BloodPressureLogRepository bloodPressureLogRepository;
    private final BloodPressurePredictionRepository bloodPressurePredictionRepository;
    private final UserRepository userRepository;

    @Transactional
    public BloodPressureLogResponse createBloodPressureLog(Long userId, BloodPressureLogCreateRequest request) {
        log.info("혈압 측정 기록 등록 시작: userId={}, systolic={}, diastolic={}",
                userId, request.getSystolic(), request.getDiastolic());

        User user = findUserById(userId);

        // 비즈니스 검증
        validateBloodPressureValues(request);
        validateMeasurementTime(request.getMeasuredAt());

        BloodPressureLog bloodPressureLog = BloodPressureLog.create(
                user,
                request.getSystolic(),
                request.getDiastolic(),
                request.getHeartRate(),
                request.getMeasuredAt(),
                request.getMeasurementType(),
                request.getNote()
        );

        BloodPressureLog savedLog = bloodPressureLogRepository.save(bloodPressureLog);
        log.info("혈압 측정 기록 등록 완료: logId={}", savedLog.getId());

        // TODO: AI 서버에 예측 요청
        // requestPrediction(userId);

        return BloodPressureLogResponse.from(savedLog);
    }

    public List<BloodPressureLogResponse> getBloodPressureLogs(Long userId,
                                                               LocalDateTime startDate,
                                                               LocalDateTime endDate) {
        log.info("혈압 측정 기록 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        validateDateRange(startDate, endDate);

        List<BloodPressureLog> logs = bloodPressureLogRepository.findByUserIdAndDateRange(
                userId, startDate, endDate);

        return logs.stream()
                .map(BloodPressureLogResponse::from)
                .collect(Collectors.toList());
    }

    public List<BloodPressureLogResponse> getBloodPressureLogsByMonth(Long userId, int year, int month) {
        log.info("월별 혈압 측정 기록 조회: userId={}, year={}, month={}", userId, year, month);

        validateMonthRange(month);

        YearMonth yearMonth = YearMonth.of(year, month);
        LocalDateTime startDateTime = yearMonth.atDay(1).atStartOfDay();
        LocalDateTime endDateTime = yearMonth.atEndOfMonth().atTime(LocalTime.MAX);

        List<BloodPressureLog> logs = bloodPressureLogRepository
                .findByUserIdAndMeasuredAtBetweenAndIsDeletedFalseOrderByMeasuredAtAsc(
                        userId, startDateTime, endDateTime);

        return logs.stream()
                .map(BloodPressureLogResponse::from)
                .collect(Collectors.toList());
    }

    public List<BloodPressurePredictionResponse> getBloodPressurePredictions(Long userId,
                                                                             LocalDate startDate,
                                                                             LocalDate endDate) {
        log.info("혈압 예측 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        if (startDate == null || endDate == null) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        validateDateRange(startDateTime, endDateTime);

        List<BloodPressurePrediction> predictions = bloodPressurePredictionRepository
                .findByUserIdAndTargetDatetimeBetweenAndIsDeletedFalseOrderByTargetDatetimeAsc(
                        userId, startDateTime, endDateTime);

        if (predictions.isEmpty()) {
            log.warn("혈압 예측 데이터 없음: userId={}", userId);
        }

        return predictions.stream()
                .map(BloodPressurePredictionResponse::from)
                .collect(Collectors.toList());
    }

    public List<MonthlyBloodPressureResponse> getMonthlyStatistics(Long userId, int year) {
        log.info("월별 혈압 집계 조회: userId={}, year={}", userId, year);

        if (year < 1900 || year > 2100) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }

        LocalDateTime startDateTime = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime endDateTime = LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);

        List<BloodPressureLogRepository.MonthlyBloodPressureStats> stats = bloodPressureLogRepository
                .findMonthlyStatsByUserId(userId, startDateTime, endDateTime);

        if (stats.isEmpty()) {
            log.info("해당 연도 혈압 데이터 없음: userId={}, year={}", userId, year);
            return new ArrayList<>();
        }

        Map<Integer, BloodPressureLogRepository.MonthlyBloodPressureStats> statsByMonth = stats.stream()
                .filter(item -> item.getMonth() != null)
                .collect(Collectors.toMap(BloodPressureLogRepository.MonthlyBloodPressureStats::getMonth, item -> item));

        List<MonthlyBloodPressureResponse> responses = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            BloodPressureLogRepository.MonthlyBloodPressureStats monthStats = statsByMonth.get(month);

            if (monthStats == null || monthStats.getCount() == null || monthStats.getCount() == 0L) {
                responses.add(MonthlyBloodPressureResponse.of(
                        year,
                        month,
                        MonthlyBloodPressureResponse.Stat.of(null, null, null),
                        MonthlyBloodPressureResponse.Stat.of(null, null, null),
                        0L
                ));
                continue;
            }

            BigDecimal avgSystolic = monthStats.getAvgSystolic() == null
                    ? null
                    : BigDecimal.valueOf(monthStats.getAvgSystolic()).setScale(1, RoundingMode.HALF_UP);
            BigDecimal avgDiastolic = monthStats.getAvgDiastolic() == null
                    ? null
                    : BigDecimal.valueOf(monthStats.getAvgDiastolic()).setScale(1, RoundingMode.HALF_UP);

            responses.add(MonthlyBloodPressureResponse.of(
                    year,
                    month,
                    MonthlyBloodPressureResponse.Stat.of(
                            avgSystolic,
                            monthStats.getMaxSystolic(),
                            monthStats.getMinSystolic()
                    ),
                    MonthlyBloodPressureResponse.Stat.of(
                            avgDiastolic,
                            monthStats.getMaxDiastolic(),
                            monthStats.getMinDiastolic()
                    ),
                    monthStats.getCount()
            ));
        }

        return responses;
    }

    /**
     * 혈압 수치 검증
     */
    private void validateBloodPressureValues(BloodPressureLogCreateRequest request) {
        Integer systolic = request.getSystolic();
        Integer diastolic = request.getDiastolic();
        Integer heartRate = request.getHeartRate();

        // 수축기 혈압 범위 검증
        if (systolic < 50 || systolic > 300) {
            throw new CustomException(ErrorCode.INVALID_SYSTOLIC_PRESSURE);
        }

        // 이완기 혈압 범위 검증
        if (diastolic < 30 || diastolic > 200) {
            throw new CustomException(ErrorCode.INVALID_DIASTOLIC_PRESSURE);
        }

        // 수축기 > 이완기 검증
        if (systolic <= diastolic) {
            throw new CustomException(ErrorCode.INVALID_BLOOD_PRESSURE_RANGE);
        }

        // 심박수 범위 검증 (optional이므로 null 체크)
        if (heartRate != null && (heartRate < 30 || heartRate > 250)) {
            throw new CustomException(ErrorCode.INVALID_HEART_RATE);
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

    /**
     * 월 유효성 검증
     */
    private void validateMonthRange(int month) {
        if (month < 1 || month > 12) {
            throw new CustomException(ErrorCode.INVALID_DATE_RANGE);
        }
    }

    private void requestPrediction(Long userId) {
        log.info("AI 예측 요청: userId={}", userId);
    }

    /**
     * 사용자 조회
     */
    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
