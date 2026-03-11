package com.kangaroo.sparring.domain.food.log.controller;

import com.kangaroo.sparring.domain.food.log.dto.req.FoodLogCreateRequest;
import com.kangaroo.sparring.domain.food.log.dto.res.FoodLogCreateResponse;
import com.kangaroo.sparring.domain.food.log.dto.res.FoodLogListItemResponse;
import com.kangaroo.sparring.domain.food.log.service.FoodLogService;
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
@RequestMapping("/api/foods")
@RequiredArgsConstructor
@Slf4j
public class FoodLogController {

    private final FoodLogService foodLogService;

    @Operation(summary = "식사 기록 등록", description = "사용자의 식사 기록을 등록합니다.")
    @PostMapping("/logs")
    public ResponseEntity<FoodLogCreateResponse> createFoodLog(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody FoodLogCreateRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("식사 기록 등록 API 호출: userId={}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(foodLogService.createFoodLog(userId, request));
    }

    @Operation(summary = "식사 기록 일별 조회", description = "사용자의 특정 일자 식사 기록을 조회합니다.")
    @GetMapping("/logs/daily")
    public ResponseEntity<List<FoodLogListItemResponse>> getDailyFoodLogs(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)", example = "2026-03-07")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("식사 기록 일별 조회 API 호출: userId={}, date={}", userId, date);
        return ResponseEntity.ok(foodLogService.getDailyFoodLogs(userId, date));
    }

    @Operation(summary = "식사 기록 삭제", description = "사용자의 식사 기록을 삭제합니다.")
    @DeleteMapping("/logs/{foodLogId}")
    public ResponseEntity<Void> deleteFoodLog(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "식사 기록 ID", example = "1")
            @PathVariable Long foodLogId
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("식사 기록 삭제 API 호출: userId={}, foodLogId={}", userId, foodLogId);
        foodLogService.deleteFoodLog(userId, foodLogId);
        return ResponseEntity.noContent().build();
    }
}
