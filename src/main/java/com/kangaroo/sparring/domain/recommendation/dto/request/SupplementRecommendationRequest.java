package com.kangaroo.sparring.domain.recommendation.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;

@Getter
@Schema(description = "영양제 추천 요청")
public class SupplementRecommendationRequest {
    // 건강프로필에서 자동 조회하므로 추가 필드 불필요
}