package com.kangaroo.sparring.domain.record.steps.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.LocalDate;

@Getter
@AllArgsConstructor
@Schema(description = "일자별 걸음수 응답")
public class StepDailyResponse {

    @Schema(description = "날짜", example = "2026-04-08")
    private LocalDate stepDate;

    @Schema(description = "총 걸음수", example = "8241")
    private Long totalSteps;
}
