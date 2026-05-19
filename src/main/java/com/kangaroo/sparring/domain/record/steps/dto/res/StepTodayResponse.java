package com.kangaroo.sparring.domain.record.steps.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@AllArgsConstructor
@Builder
@Schema(description = "오늘 걸음수 응답")
public class StepTodayResponse {

    @Schema(description = "기준 날짜 (KST)", example = "2026-04-08")
    private LocalDate stepDate;

    @Schema(description = "오늘 총 걸음수", example = "8241")
    private Integer totalSteps;

    @Schema(description = "마지막 동기화 시각", example = "2026-04-08T20:12:00", nullable = true)
    private LocalDateTime updatedAt;
}
