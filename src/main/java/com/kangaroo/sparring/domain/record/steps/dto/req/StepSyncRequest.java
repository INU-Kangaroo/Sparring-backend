package com.kangaroo.sparring.domain.record.steps.dto.req;

import com.kangaroo.sparring.domain.record.steps.type.StepSource;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "걸음수 동기화 요청")
public class StepSyncRequest {

    @Schema(description = "걸음수 일자 (KST)", example = "2026-04-08", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "걸음수 날짜는 필수입니다")
    private LocalDate stepDate;

    @Schema(description = "걸음수", example = "8241", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "걸음수는 필수입니다")
    @Min(value = 0, message = "걸음수는 0 이상이어야 합니다")
    @Max(value = 200000, message = "걸음수는 200000 이하여야 합니다")
    private Integer steps;

    @Schema(
            description = "동기화 소스",
            example = "APPLE_HEALTH",
            allowableValues = {"APPLE_HEALTH", "GOOGLE_FIT", "MANUAL"},
            requiredMode = Schema.RequiredMode.REQUIRED
    )
    @NotNull(message = "동기화 소스는 필수입니다")
    private StepSource source;
}
