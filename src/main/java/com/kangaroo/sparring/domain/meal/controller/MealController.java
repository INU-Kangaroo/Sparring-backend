package com.kangaroo.sparring.domain.meal.controller;

import com.kangaroo.sparring.domain.meal.dto.req.MealLogCreateRequest;
import com.kangaroo.sparring.domain.meal.dto.res.MealLogResponse;
import com.kangaroo.sparring.domain.meal.service.MealLogService;
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

@Tag(name = "식사", description = "식사 기록 API")
@RestController
@RequestMapping("/api/meal")
@RequiredArgsConstructor
@Slf4j
public class MealController {

    private final MealLogService mealLogService;

    @Operation(description = "식사 기록 등록")
    @PostMapping("/logs")
    public ResponseEntity<MealLogResponse> createMealLog(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody MealLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("식사 기록 등록 API 호출: userId={}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(mealLogService.createMealLog(userId, request));
    }

    @Operation(description = "식사 기록 일별 조회")
    @GetMapping("/logs/daily")
    public ResponseEntity<List<MealLogResponse>> getDailyMealLogs(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)", example = "2026-03-07")
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("식사 기록 일별 조회 API 호출: userId={}, date={}", userId, date);
        return ResponseEntity.ok(mealLogService.getDailyMealLogs(userId, date));
    }

    @Operation(description = "식사 기록 삭제")
    @DeleteMapping("/logs/{mealLogId}")
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
