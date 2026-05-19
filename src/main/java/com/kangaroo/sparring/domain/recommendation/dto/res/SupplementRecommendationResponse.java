package com.kangaroo.sparring.domain.recommendation.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Builder;
import lombok.Getter;

import java.util.List;

@Getter
@Builder
@Schema(description = "영양제 추천 응답")
public class SupplementRecommendationResponse {

    @Schema(description = "영양제 목록")
    private List<SupplementResponse> supplements;

    public static SupplementRecommendationResponse of(List<SupplementResponse> supplements) {
        return SupplementRecommendationResponse.builder()
                .supplements(supplements)
                .build();
    }
}