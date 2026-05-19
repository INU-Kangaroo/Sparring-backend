package com.kangaroo.sparring.domain.insight.today.service;

import com.kangaroo.sparring.domain.insight.today.service.InsightContextBuilder.InsightContext;
import com.kangaroo.sparring.domain.insight.today.type.InsightType;
import com.kangaroo.sparring.domain.insight.today.type.MealTimeSlot;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InsightPromptSupportTest {

    @Test
    void 프롬프트는_식사기록과_걸음수_요약을_함께_포함한다() {
        InsightPromptSupport support = new InsightPromptSupport();
        support.init();

        InsightContext context = InsightContext.builder()
                .type(InsightType.BLOOD_SUGAR_HIGH)
                .avgGlucose(142.0)
                .latestSystolic(128)
                .mealLogCount(9)
                .todaySteps(8241)
                .averageDailySteps(7012)
                .build();

        String prompt = support.build(context, MealTimeSlot.MORNING);

        assertThat(prompt).contains("식사 기록 9회");
        assertThat(prompt).contains("걸음수 8,241보");
    }

    @Test
    void 오늘_걸음수가_0이면_걸음수_문구는_포함하지_않는다() {
        InsightPromptSupport support = new InsightPromptSupport();
        support.init();

        InsightContext context = InsightContext.builder()
                .type(InsightType.NEEDS_MONITORING)
                .mealLogCount(4)
                .todaySteps(0)
                .averageDailySteps(7012)
                .build();

        String prompt = support.build(context, MealTimeSlot.AFTERNOON);

        assertThat(prompt).contains("식사 기록 4회");
        assertThat(prompt).doesNotContain("걸음수");
    }
}
