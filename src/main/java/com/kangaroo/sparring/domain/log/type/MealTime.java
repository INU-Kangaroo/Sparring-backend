package com.kangaroo.sparring.domain.log.type;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

import java.util.Arrays;

@Getter
@RequiredArgsConstructor
public enum MealTime {
    BREAKFAST("아침"),
    LUNCH("점심"),
    DINNER("저녁"),
    SNACK("간식");

    private final String label;

    @JsonCreator
    public static MealTime from(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        return Arrays.stream(values())
                .filter(mealTime -> mealTime.name().equalsIgnoreCase(normalized) || mealTime.label.equals(normalized))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("유효하지 않은 mealTime입니다"));
    }

    @JsonValue
    public String toValue() {
        return this.name();
    }
}
