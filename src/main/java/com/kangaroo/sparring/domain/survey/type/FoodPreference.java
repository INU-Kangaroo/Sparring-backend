package com.kangaroo.sparring.domain.survey.type;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FoodPreference {
    CARB_HEAVY("탄수화물 위주"),
    PROTEIN_HEAVY("단백질 위주"),
    PROCESSED_FOOD_HEAVY("가공식품 위주"),
    VEGETARIAN("채식");

    private final String description;
}
