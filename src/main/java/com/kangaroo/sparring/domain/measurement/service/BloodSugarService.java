package com.kangaroo.sparring.domain.measurement.service;

import com.kangaroo.sparring.domain.measurement.dto.request.BloodSugarLogCreateRequest;
import com.kangaroo.sparring.domain.measurement.dto.response.BloodSugarLogResponse;
import com.kangaroo.sparring.domain.measurement.dto.response.BloodSugarPredictionResponse;
import com.kangaroo.sparring.domain.measurement.dto.response.MonthlyBloodSugarResponse;
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

        BloodSugarLog bloodSugarLog = BloodSugarLog.create(
                user,
                request.getGlucoseLevel(),
                request.getMeasurementTime(),
                request.getMeasurementType(),
                request.getNote()
        );

        BloodSugarLog savedLog = bloodSugarLogRepository.save(bloodSugarLog);
        log.info("혈당 측정 기록 등록 완료: logId={}", savedLog.getId());

        // TODO: AI 서버에 예측 요청
        // requestPrediction(userId);

        return BloodSugarLogResponse.from(savedLog);
    }

    public List<BloodSugarLogResponse> getBloodSugarLogs(Long userId, LocalDateTime startDate, LocalDateTime endDate) {
        log.info("혈당 측정 기록 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

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

    public List<BloodSugarPredictionResponse> getBloodSugarPredictions(Long userId,
                                                                       LocalDate startDate,
                                                                       LocalDate endDate) {
        log.info("혈당 예측 조회: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

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

        LocalDateTime startDateTime = LocalDate.of(year, 1, 1).atStartOfDay();
        LocalDateTime endDateTime = LocalDate.of(year, 12, 31).atTime(LocalTime.MAX);

        List<BloodSugarLog> logs = bloodSugarLogRepository
                .findByUserIdAndMeasurementTimeBetweenAndIsDeletedFalseOrderByMeasurementTimeAsc(
                        userId, startDateTime, endDateTime);

        if (logs.isEmpty()) {
            log.info("해당 연도 혈당 데이터 없음: userId={}, year={}", userId, year);
            return new ArrayList<>();
        }

        Map<Integer, List<BloodSugarLog>> logsByMonth = logs.stream()
                .collect(Collectors.groupingBy(log -> log.getMeasurementTime().getMonthValue()));

        List<MonthlyBloodSugarResponse> responses = new ArrayList<>();
        for (int month = 1; month <= 12; month++) {
            List<BloodSugarLog> monthLogs = logsByMonth.get(month);

            if (monthLogs == null || monthLogs.isEmpty()) {
                responses.add(MonthlyBloodSugarResponse.builder()
                        .year(year)
                        .month(month)
                        .averageValue(null)
                        .maxValue(null)
                        .minValue(null)
                        .count(0L)
                        .build());
            } else {
                double average = monthLogs.stream()
                        .mapToInt(BloodSugarLog::getGlucoseLevel)
                        .average()
                        .orElse(0.0);

                int max = monthLogs.stream()
                        .mapToInt(BloodSugarLog::getGlucoseLevel)
                        .max()
                        .orElse(0);

                int min = monthLogs.stream()
                        .mapToInt(BloodSugarLog::getGlucoseLevel)
                        .min()
                        .orElse(0);

                responses.add(MonthlyBloodSugarResponse.of(
                        year,
                        month,
                        BigDecimal.valueOf(average).setScale(1, RoundingMode.HALF_UP),
                        max,
                        min,
                        (long) monthLogs.size()
                ));
            }
        }

        return responses;
    }

    private void requestPrediction(Long userId) {
        log.info("AI 예측 요청: userId={}", userId);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
