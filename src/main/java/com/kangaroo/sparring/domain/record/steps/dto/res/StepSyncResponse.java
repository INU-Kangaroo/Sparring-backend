package com.kangaroo.sparring.domain.record.steps.dto.res;

import com.kangaroo.sparring.domain.record.steps.entity.StepLog;
import com.kangaroo.sparring.domain.record.steps.type.StepSource;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "걸음수 동기화 응답")
public class StepSyncResponse {

    @Schema(description = "걸음수 날짜", example = "2026-04-08")
    private LocalDate stepDate;

    @Schema(description = "걸음수", example = "8241")
    private Integer steps;

    @Schema(description = "동기화 소스", example = "APPLE_HEALTH")
    private StepSource source;

    @Schema(description = "동기화 시각", example = "2026-04-08T20:12:00")
    private LocalDateTime syncedAt;

    public static StepSyncResponse from(StepLog stepLog) {
        return StepSyncResponse.builder()
                .stepDate(stepLog.getStepDate())
                .steps(stepLog.getSteps())
                .source(stepLog.getSource())
                .syncedAt(stepLog.getSyncedAt())
                .build();
    }
}
