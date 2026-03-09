package com.kangaroo.sparring.domain.insight.today.dto.res;

import com.kangaroo.sparring.domain.insight.today.type.InsightType;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class TodayInsightResponse {

    private InsightType type;
    private String message;  // Gemini가 생성한 1~2문장

    public static TodayInsightResponse of(InsightType type, String message) {
        return TodayInsightResponse.builder()
                .type(type)
                .message(message)
                .build();
    }
}