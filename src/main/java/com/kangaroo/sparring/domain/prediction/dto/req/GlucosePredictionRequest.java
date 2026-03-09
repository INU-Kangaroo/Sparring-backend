package com.kangaroo.sparring.domain.prediction.dto.req;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@Schema(description = "혈당 예측 요청")
public class GlucosePredictionRequest {

    @Schema(description = "음식 ID", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "음식 ID는 필수입니다")
    @Positive(message = "음식 ID는 양수여야 합니다")
    private Long foodId;
}
