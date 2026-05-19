package com.kangaroo.sparring.domain.recommendation.dto.res;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@Builder
@Schema(description = "식단 추천 응답")
public class MealRecommendationResponse {

    @Schema(description = "식단 추천 ID", example = "101")
    private Long recommendationId;

    @Schema(description = "식사 시간대", example = "LUNCH")
    private String mealType;

    @Schema(description = "추천 카드 목록")
    private List<RecommendationCardDto> recommendations;

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "추천 카드")
    public static class RecommendationCardDto {

        @Schema(description = "식단 세트(카드) ID", example = "1001")
        private Long recommendationCardId;

        @Schema(description = "순위", example = "1")
        private int rank;

        @Schema(description = "세트 제목", example = "저당 점심 한식 세트")
        private String title;

        @Schema(description = "세트 영양성분 합계")
        private NutrientsDto nutrients;

        @Schema(description = "추천 이유")
        private List<String> reasons;

        @Schema(description = "구성 메뉴 목록")
        private List<MenuItemDto> menus;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "영양성분 합계")
    public static class NutrientsDto {

        @Schema(description = "칼로리 (kcal)", example = "507.0")
        private double kcal;

        @Schema(description = "탄수화물 (g)", example = "58.8")
        private double carbs;

        @Schema(description = "단백질 (g)", example = "53.0")
        private double protein;

        @Schema(description = "지방 (g)", example = "9.9")
        private double fat;

        @Schema(description = "나트륨 (mg)", example = "976.0")
        private Double sodium;
    }

    @Getter
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @Schema(description = "메뉴 항목")
    public static class MenuItemDto {

        @Schema(description = "레시피 ID (혈당 예측 연동용)", example = "1")
        private long id;

        @Schema(description = "메뉴명", example = "현미밥")
        private String name;

        @Schema(description = "칼로리 (kcal)", example = "200.0")
        private double kcal;

        @Schema(description = "탄수화물 (g)", example = "46.0")
        private double carbs;

        @Schema(description = "단백질 (g)", example = "4.0")
        private double protein;

        @Schema(description = "지방 (g)", example = "1.6")
        private double fat;

        @Schema(description = "나트륨 (mg)", example = "4.0")
        private Double sodium;
    }
}
