package com.kangaroo.sparring.domain.measurement.controller;

import com.kangaroo.sparring.domain.measurement.dto.req.BloodPressureLogCreateRequest;
import com.kangaroo.sparring.domain.measurement.dto.res.BloodPressureLogResponse;
import com.kangaroo.sparring.domain.measurement.dto.res.BloodPressurePredictionResponse;
import com.kangaroo.sparring.domain.measurement.dto.res.MonthlyBloodPressureResponse;
import com.kangaroo.sparring.domain.measurement.service.BloodPressureService;
import com.kangaroo.sparring.domain.measurement.support.MeasurementValidationSupport;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "혈압 측정", description = "혈압 측정 및 예측 관리 API")
@RestController
@RequestMapping("/api/measurements/blood-pressure")
@RequiredArgsConstructor
@Slf4j
public class BloodPressureController {

    private final BloodPressureService bloodPressureService;

    @Operation(summary = "혈압 측정 기록 등록", description = "혈압 기록 등록 및 AI 예측 자동 생성")
    @PostMapping("/logs")
    public ResponseEntity<BloodPressureLogResponse> createBloodPressureLog(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody BloodPressureLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("혈압 측정 기록 등록 API 호출: userId={}", userId);

        BloodPressureLogResponse response = bloodPressureService.createBloodPressureLog(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "혈압 측정 기록 조회 (기간)", description = "특정 기간 혈압 기록 조회")
    @GetMapping("/logs")
    public ResponseEntity<List<BloodPressureLogResponse>> getBloodPressureLogs(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)", example = "2026-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        MeasurementValidationSupport.DateTimeRange range =
                MeasurementValidationSupport.toDateTimeRange(startDate, endDate);

        log.info("혈압 측정 기록 조회 API 호출: userId={}, startDate={}, endDate={}",
                userId, range.start(), range.end());

        List<BloodPressureLogResponse> responses =
                bloodPressureService.getBloodPressureLogs(userId, range.start(), range.end());
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "혈압 측정 기록 조회 (월)", description = "특정 연도/월 혈압 기록 조회")
    @GetMapping("/logs/monthly")
    public ResponseEntity<List<BloodPressureLogResponse>> getBloodPressureLogsByMonth(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "연도", example = "2025")
            @RequestParam int year,
            @Parameter(description = "월 (1-12)", example = "10")
            @RequestParam int month
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("월별 혈압 측정 기록 조회 API 호출: userId={}, year={}, month={}", userId, year, month);

        List<BloodPressureLogResponse> responses =
                bloodPressureService.getBloodPressureLogsByMonth(userId, year, month);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "혈압 측정 기록 조회 (일)", description = "특정 날짜 혈압 기록 조회")
    @GetMapping("/logs/daily")
    public ResponseEntity<List<BloodPressureLogResponse>> getBloodPressureLogsByDate(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "날짜 (yyyy-MM-dd)", example = "2026-01-28")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("일별 혈압 측정 기록 조회 API 호출: userId={}, date={}", userId, date);

        List<BloodPressureLogResponse> responses = bloodPressureService.getBloodPressureLogsByDate(userId, date);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "혈압 예측 조회", description = "특정 기간 혈압 예측 데이터 조회")
    @GetMapping("/predictions")
    public ResponseEntity<List<BloodPressurePredictionResponse>> getBloodPressurePredictions(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)", example = "2026-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("혈압 예측 조회 API 호출: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        List<BloodPressurePredictionResponse> responses =
                bloodPressureService.getBloodPressurePredictions(userId, startDate, endDate);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "월별 혈압 집계 조회", description = "특정 연도 월별 혈압 통계 조회 (1월~12월)")
    @GetMapping("/logs/monthly-stats")
    public ResponseEntity<List<MonthlyBloodPressureResponse>> getMonthlyBloodPressure(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "연도", example = "2025")
            @RequestParam int year
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("월별 혈압 집계 조회 API 호출: userId={}, year={}", userId, year);

        List<MonthlyBloodPressureResponse> responses =
                bloodPressureService.getMonthlyStatistics(userId, year);
        return ResponseEntity.ok(responses);
    }

    
}
