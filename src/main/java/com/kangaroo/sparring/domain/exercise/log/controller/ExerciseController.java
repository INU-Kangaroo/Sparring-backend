package com.kangaroo.sparring.domain.exercise.log.controller;

import com.kangaroo.sparring.domain.exercise.log.dto.req.ExerciseLogRequest;
import com.kangaroo.sparring.domain.exercise.log.dto.res.ExerciseLogCreateResponse;
import com.kangaroo.sparring.domain.exercise.log.dto.res.ExerciseLogListItemResponse;
import com.kangaroo.sparring.domain.exercise.log.service.ExerciseLogService;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.Operation;
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

@Tag(name = "운동", description = "운동 기록 API")
@RestController("exerciseLogController")
@RequestMapping("/api/exercises")
@RequiredArgsConstructor
@Slf4j
public class ExerciseController {

    private final ExerciseLogService exerciseLogService;

    @Operation(description = "운동 기록 저장")
    @PostMapping("/logs")
    public ResponseEntity<ExerciseLogCreateResponse> createExerciseLog(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ExerciseLogRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("운동 기록 저장 API 호출: userId={}", userId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(exerciseLogService.createExerciseLog(userId, request));
    }

    @Operation(description = "운동 기록 일별 조회")
    @GetMapping("/logs/daily")
    public ResponseEntity<List<ExerciseLogListItemResponse>> getDailyExerciseLogs(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Parameter(description = "조회 날짜 (yyyy-MM-dd)", example = "2026-03-09")
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        log.info("운동 기록 일별 조회 API 호출: userId={}, date={}", userId, date);
        return ResponseEntity.ok(exerciseLogService.getDailyExerciseLogs(userId, date));
    }
}
