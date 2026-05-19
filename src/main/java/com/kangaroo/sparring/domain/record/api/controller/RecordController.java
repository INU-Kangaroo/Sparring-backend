package com.kangaroo.sparring.domain.record.api.controller;

import com.kangaroo.sparring.domain.record.api.dto.req.RecordQueryRequest;
import com.kangaroo.sparring.domain.record.blood.dto.req.BloodPressureLogCreateRequest;
import com.kangaroo.sparring.domain.record.blood.dto.req.BloodSugarLogCreateRequest;
import com.kangaroo.sparring.domain.record.blood.dto.res.BloodPressureLogResponse;
import com.kangaroo.sparring.domain.record.blood.dto.res.BloodSugarLogResponse;
import com.kangaroo.sparring.domain.record.blood.service.BloodPressureService;
import com.kangaroo.sparring.domain.record.blood.service.BloodSugarService;
import com.kangaroo.sparring.domain.record.common.RecordPeriodSupport;
import com.kangaroo.sparring.domain.record.exercise.dto.req.ExerciseLogRequest;
import com.kangaroo.sparring.domain.record.exercise.dto.res.ExerciseLogCreateResponse;
import com.kangaroo.sparring.domain.record.exercise.dto.res.ExerciseLogListItemResponse;
import com.kangaroo.sparring.domain.record.exercise.service.ExerciseLogService;
import com.kangaroo.sparring.domain.record.food.dto.req.FoodLogCreateRequest;
import com.kangaroo.sparring.domain.record.food.dto.res.FoodLogCreateResponse;
import com.kangaroo.sparring.domain.record.food.dto.res.FoodLogListItemResponse;
import com.kangaroo.sparring.domain.record.food.service.FoodLogService;
import com.kangaroo.sparring.domain.record.insulin.dto.req.InsulinLogCreateRequest;
import com.kangaroo.sparring.domain.record.insulin.dto.res.InsulinLogResponse;
import com.kangaroo.sparring.domain.record.insulin.service.InsulinLogService;
import com.kangaroo.sparring.domain.record.steps.dto.req.StepSyncRequest;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepDailyResponse;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepSyncResponse;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepTodayResponse;
import com.kangaroo.sparring.domain.record.steps.service.StepLogService;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Tag(name = "기록", description = "기록 생성/조회 API")
@RestController
@RequestMapping("/api/records")
@RequiredArgsConstructor
public class RecordController {

    private final BloodSugarService bloodSugarService;
    private final BloodPressureService bloodPressureService;
    private final InsulinLogService insulinLogService;
    private final FoodLogService foodLogService;
    private final ExerciseLogService exerciseLogService;
    private final StepLogService stepLogService;
    private final Clock kstClock;

    @Operation(summary = "혈당 기록 등록", description = "혈당 기록 등록")
    @PostMapping("/blood-sugar")
    public ResponseEntity<BloodSugarLogResponse> createBloodSugarRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody BloodSugarLogCreateRequest request
    ) {
        return created(bloodSugarService.createBloodSugarLog(resolveUserId(principal), request));
    }

    @Operation(summary = "혈당 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 혈당 기록 조회")
    @GetMapping("/blood-sugar")
    public ResponseEntity<List<BloodSugarLogResponse>> getBloodSugarRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @ParameterObject RecordQueryRequest query
    ) {
        return queryByRange(principal, query, bloodSugarService::getBloodSugarLogs);
    }

    @Operation(summary = "혈압 기록 등록", description = "혈압 기록 등록")
    @PostMapping("/blood-pressure")
    public ResponseEntity<BloodPressureLogResponse> createBloodPressureRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody BloodPressureLogCreateRequest request
    ) {
        return created(bloodPressureService.createBloodPressureLog(resolveUserId(principal), request));
    }

    @Operation(summary = "혈압 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 혈압 기록 조회")
    @GetMapping("/blood-pressure")
    public ResponseEntity<List<BloodPressureLogResponse>> getBloodPressureRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @ParameterObject RecordQueryRequest query
    ) {
        return queryByRange(principal, query, bloodPressureService::getBloodPressureLogs);
    }

    @Operation(summary = "인슐린 기록 등록")
    @PostMapping("/insulin")
    public ResponseEntity<InsulinLogResponse> createInsulinRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody InsulinLogCreateRequest request
    ) {
        return created(insulinLogService.createInsulinLog(resolveUserId(principal), request));
    }

    @Operation(summary = "인슐린 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 인슐린 기록 조회")
    @GetMapping("/insulin")
    public ResponseEntity<List<InsulinLogResponse>> getInsulinRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @ParameterObject RecordQueryRequest query
    ) {
        return queryByRange(principal, query, insulinLogService::getInsulinLogs);
    }

    @Operation(summary = "식사 기록 등록")
    @PostMapping("/food")
    public ResponseEntity<FoodLogCreateResponse> createFoodRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody FoodLogCreateRequest request
    ) {
        return created(foodLogService.createFoodLog(resolveUserId(principal), request));
    }

    @Operation(summary = "식사 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 식사 기록 조회")
    @GetMapping("/food")
    public ResponseEntity<List<FoodLogListItemResponse>> getFoodRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @ParameterObject RecordQueryRequest query
    ) {
        return queryByRange(principal, query, foodLogService::getFoodLogs);
    }

    @Operation(summary = "식사 기록 삭제")
    @DeleteMapping("/food/{foodLogId}")
    public ResponseEntity<Void> deleteFoodRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @PathVariable Long foodLogId
    ) {
        foodLogService.deleteFoodLog(resolveUserId(principal), foodLogId);
        return ResponseEntity.noContent().build();
    }

    @Operation(summary = "운동 기록 등록", description = "운동 기록 등록")
    @PostMapping("/exercise")
    public ResponseEntity<ExerciseLogCreateResponse> createExerciseRecord(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ExerciseLogRequest request
    ) {
        return created(exerciseLogService.createExerciseLog(resolveUserId(principal), request));
    }

    @Operation(summary = "운동 기록 조회", description = "period(daily/weekly/monthly/range) 기준으로 운동 기록 조회")
    @GetMapping("/exercise")
    public ResponseEntity<List<ExerciseLogListItemResponse>> getExerciseRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @ParameterObject RecordQueryRequest query
    ) {
        return queryByRange(principal, query, exerciseLogService::getExerciseLogs);
    }

    @Operation(summary = "걸음수 동기화", description = "소스(APPLE_HEALTH/GOOGLE_FIT/MANUAL)별 일자 걸음수 업서트")
    @PostMapping("/steps/sync")
    public ResponseEntity<StepSyncResponse> syncSteps(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody StepSyncRequest request
    ) {
        return created(stepLogService.syncStepLog(resolveUserId(principal), request));
    }

    @Operation(summary = "오늘 걸음수 조회", description = "KST 기준 오늘 총 걸음수 조회")
    @GetMapping("/steps/today")
    public ResponseEntity<StepTodayResponse> getTodaySteps(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        return ResponseEntity.ok(stepLogService.getTodaySteps(resolveUserId(principal)));
    }

    @Operation(summary = "걸음수 기록 조회", description = "period(daily/weekly/monthly/range) 기준 일자별 걸음수 합계 조회")
    @GetMapping("/steps")
    public ResponseEntity<List<StepDailyResponse>> getStepRecords(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @ParameterObject RecordQueryRequest query
    ) {
        return queryByRange(principal, query, stepLogService::getStepLogs);
    }

    private Long resolveUserId(UserIdPrincipal principal) {
        return PrincipalResolver.resolveUserId(principal);
    }

    private RecordPeriodSupport.DateTimeRange resolveRange(RecordQueryRequest query) {
        return query.toRange(kstClock);
    }

    private <T> ResponseEntity<T> created(T body) {
        return ResponseEntity.status(HttpStatus.CREATED).body(body);
    }

    private <T> ResponseEntity<List<T>> queryByRange(
            UserIdPrincipal principal,
            RecordQueryRequest query,
            RangeQueryHandler<T> handler
    ) {
        Long userId = resolveUserId(principal);
        RecordPeriodSupport.DateTimeRange range = resolveRange(query);
        return ResponseEntity.ok(handler.get(userId, range.start(), range.end()));
    }

    @FunctionalInterface
    private interface RangeQueryHandler<T> {
        List<T> get(Long userId, LocalDateTime start, LocalDateTime end);
    }
}
