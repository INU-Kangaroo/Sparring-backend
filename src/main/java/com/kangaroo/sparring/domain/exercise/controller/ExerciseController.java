package com.kangaroo.sparring.domain.exercise.controller;

import com.kangaroo.sparring.domain.exercise.dto.req.ExerciseLogRequest;
import com.kangaroo.sparring.domain.exercise.dto.res.ExerciseLogResponse;
import com.kangaroo.sparring.domain.exercise.service.ExerciseLogService;
import com.kangaroo.sparring.global.auth.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Tag(name = "운동", description = "운동 기록 API")
@RestController
@RequestMapping("/api/v1/exercise")
@RequiredArgsConstructor
public class ExerciseController {

    private final ExerciseLogService exerciseLogService;

    @Operation(description = "운동 기록 저장")
    @PostMapping("/logs")
    public ResponseEntity<ExerciseLogResponse> createExerciseLog(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ExerciseLogRequest request
    ) {
        return ResponseEntity.ok(exerciseLogService.createExerciseLog(principal.getUserId(), request));
    }

    @Operation(description = "운동 기록 목록 조회")
    @GetMapping("/logs")
    public ResponseEntity<List<ExerciseLogResponse>> getExerciseLogs(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        return ResponseEntity.ok(exerciseLogService.getExerciseLogs(principal.getUserId()));
    }
}
