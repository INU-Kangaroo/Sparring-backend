package com.kangaroo.sparring.domain.log.controller;

import com.kangaroo.sparring.domain.log.dto.req.MealLogCreateRequest;
import com.kangaroo.sparring.domain.log.dto.res.MealLogResponse;
import com.kangaroo.sparring.domain.log.service.MealLogService;
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
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.util.List;

@Tag(name = "기록", description = "식사 기록 관리 API")
@RestController
@RequestMapping("/api/logs")
@RequiredArgsConstructor
@Slf4j
public class LogController {

    private final MealLogService mealLogService;

    @Operation(summary = "식사 기록 등록", description = "식사 기록을 등록한다.")
    @PostMapping("/meal")
    public ResponseEntity<MealLogResponse> createMealLog(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody MealLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("식사 기록 등록 API 호출: userId={}", userId);

        MealLogResponse response = mealLogService.createMealLog(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(response);
    }

    @Operation(summary = "식사 기록 일별 조회", description = "특정 날짜의 식사 기록을 조회한다.")
    @GetMapping("/meal/daily")
    public ResponseEntity<List<MealLogResponse>> getDailyMealLogs(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)", example = "2026-03-07")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("식사 기록 일별 조회 API 호출: userId={}, date={}", userId, date);

        List<MealLogResponse> response = mealLogService.getDailyMealLogs(userId, date);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "식사 기록 삭제", description = "식사 기록을 삭제한다.")
    @DeleteMapping("/meal/{mealLogId}")
    public ResponseEntity<Void> deleteMealLog(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "식사 기록 ID", example = "1")
            @PathVariable Long mealLogId
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("식사 기록 삭제 API 호출: userId={}, mealLogId={}", userId, mealLogId);

        mealLogService.deleteMealLog(userId, mealLogId);
        return ResponseEntity.noContent().build();
    }
}
