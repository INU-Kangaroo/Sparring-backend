package com.kangaroo.sparring.domain.measurement.controller;

import com.kangaroo.sparring.domain.measurement.dto.request.BloodSugarLogCreateRequest;
import com.kangaroo.sparring.domain.measurement.dto.response.BloodSugarLogResponse;
import com.kangaroo.sparring.domain.measurement.dto.response.BloodSugarPredictionResponse;
import com.kangaroo.sparring.domain.measurement.dto.response.MonthlyBloodSugarResponse;
import com.kangaroo.sparring.domain.measurement.service.BloodSugarService;
import com.kangaroo.sparring.global.security.jwt.CustomUserPrincipal;
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
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Tag(name = "혈당 측정", description = "혈당 측정 및 예측 관리 API")
@RestController
@RequestMapping("/api/measurements/blood-sugar")
@RequiredArgsConstructor
@Slf4j
public class BloodSugarController {

    private final BloodSugarService bloodSugarService;

    @Operation(summary = "혈당 측정 기록 등록", description = "혈당 측정 기록을 등록하고 AI 예측을 자동 생성한다")
    @PostMapping("/logs")
    public ResponseEntity<BloodSugarLogResponse> createBloodSugarLog(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody BloodSugarLogCreateRequest request
    ) {
        Long userId = principal.getUserId();
        log.info("혈당 측정 기록 등록 API 호출: userId={}", userId);

        BloodSugarLogResponse response = bloodSugarService.createBloodSugarLog(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "혈당 측정 기록 조회 (기간)", description = "특정 기간의 혈당 측정 기록을 조회한다")
    @GetMapping("/logs")
    public ResponseEntity<List<BloodSugarLogResponse>> getBloodSugarLogs(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)", example = "2026-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = principal.getUserId();

        LocalDateTime startDateTime = startDate.atStartOfDay();
        LocalDateTime endDateTime = endDate.atTime(LocalTime.MAX);

        log.info("혈당 측정 기록 조회 API 호출: userId={}, startDate={}, endDate={}",
                userId, startDateTime, endDateTime);

        List<BloodSugarLogResponse> responses =
                bloodSugarService.getBloodSugarLogs(userId, startDateTime, endDateTime);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "혈당 측정 기록 조회 (월)", description = "특정 연도/월의 혈당 측정 기록을 조회한다")
    @GetMapping("/logs/monthly")
    public ResponseEntity<List<BloodSugarLogResponse>> getBloodSugarLogsByMonth(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "연도", example = "2025")
            @RequestParam int year,
            @Parameter(description = "월 (1-12)", example = "10")
            @RequestParam int month
    ) {
        Long userId = principal.getUserId();
        log.info("월별 혈당 측정 기록 조회 API 호출: userId={}, year={}, month={}", userId, year, month);

        List<BloodSugarLogResponse> responses =
                bloodSugarService.getBloodSugarLogsByMonth(userId, year, month);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "월별 혈당 집계 조회", description = "특정 연도의 월별 혈당 통계를 조회한다 (1월~12월)")
    @GetMapping("/logs/monthly-stats")
    public ResponseEntity<List<MonthlyBloodSugarResponse>> getMonthlyBloodSugar(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "연도", example = "2025")
            @RequestParam int year
    ) {
        Long userId = principal.getUserId();
        log.info("월별 혈당 집계 조회 API 호출: userId={}, year={}", userId, year);

        List<MonthlyBloodSugarResponse> responses =
                bloodSugarService.getMonthlyStatistics(userId, year);
        return ResponseEntity.ok(responses);
    }

    @Operation(summary = "혈당 예측 조회", description = "특정 기간의 혈당 예측 결과를 조회한다")
    @GetMapping("/predictions")
    public ResponseEntity<List<BloodSugarPredictionResponse>> getBloodSugarPredictions(
            @AuthenticationPrincipal CustomUserPrincipal principal,
            @Parameter(description = "시작 날짜 (yyyy-MM-dd)", example = "2026-01-01")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @Parameter(description = "종료 날짜 (yyyy-MM-dd)", example = "2026-01-31")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate
    ) {
        Long userId = principal.getUserId();
        log.info("혈당 예측 조회 API 호출: userId={}, startDate={}, endDate={}", userId, startDate, endDate);

        List<BloodSugarPredictionResponse> responses =
                bloodSugarService.getBloodSugarPredictions(userId, startDate, endDate);
        return ResponseEntity.ok(responses);
    }
}
