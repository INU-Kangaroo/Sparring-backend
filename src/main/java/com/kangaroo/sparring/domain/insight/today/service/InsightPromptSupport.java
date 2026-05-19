package com.kangaroo.sparring.domain.insight.today.service;

import com.kangaroo.sparring.domain.insight.today.service.InsightContextBuilder.InsightContext;
import com.kangaroo.sparring.domain.insight.today.type.MealTimeSlot;
import jakarta.annotation.PostConstruct;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Component
public class InsightPromptSupport {

    private static final String TEMPLATE_PATH = "prompts/insight/insight_prompt.txt";

    private String template;

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(TEMPLATE_PATH);
            this.template = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("인사이트 프롬프트 템플릿 로드 실패: " + TEMPLATE_PATH, e);
        }
    }

    public String build(InsightContext context, MealTimeSlot slot) {
        return template
                .replace("{{timeSlot}}", describeTimeSlot(slot))
                .replace("{{situation}}", describeSituation(context));
    }

    private String describeSituation(InsightContext context) {
        String base = switch (context.getType()) {
            case BLOOD_SUGAR_STABLE ->
                    String.format("최근 공복 혈당이 3일 이상 안정적으로 유지되고 있습니다. (평균 %.0f mg/dL)",
                            context.getAvgGlucose());
            case BLOOD_SUGAR_HIGH ->
                    String.format("최근 2일 이상 혈당이 높은 상태입니다. (평균 %.0f mg/dL)",
                            context.getAvgGlucose());
            case BLOOD_SUGAR_LOW ->
                    "최근 저혈당 기록이 있습니다. 식사 패턴에 주의가 필요합니다.";
            case BLOOD_PRESSURE_STABLE ->
                    String.format("최근 혈압이 안정적으로 유지되고 있습니다. (최근 수축기 %d mmHg)",
                            context.getLatestSystolic());
            case BLOOD_PRESSURE_HIGH ->
                    String.format("최근 혈압이 다소 높은 편입니다. (최근 수축기 %d mmHg)",
                            context.getLatestSystolic());
            case BOTH_STABLE ->
                    "혈당과 혈압 모두 안정적으로 관리되고 있습니다.";
            case NEEDS_MONITORING ->
                    "최근 건강 기록이 존재하지만, 아직 뚜렷한 경향은 확인되지 않습니다. 기록을 조금 더 누적해 추이를 관찰해보세요.";
            case NO_DATA ->
                    "아직 건강 기록 데이터가 없습니다. 첫 측정을 시작해보세요.";
        };
        return appendLifestyleSignals(base, context);
    }

    private String describeTimeSlot(MealTimeSlot slot) {
        return switch (slot) {
            case MORNING, NIGHT -> "아침 (공복 혈당 측정 권장 시간)";
            case AFTERNOON -> "점심 이후";
            case EVENING -> "저녁";
        };
    }

    private String appendLifestyleSignals(String base, InsightContext context) {
        List<String> signals = new ArrayList<>();

        if (context.getMealLogCount() != null && context.getMealLogCount() > 0) {
            signals.add(String.format("최근 7일 식사 기록 %d회", context.getMealLogCount()));
        }

        if (context.getTodaySteps() != null && context.getTodaySteps() > 0) {
            signals.add(String.format(Locale.KOREA, "오늘 걸음수 %,d보", context.getTodaySteps()));
        }

        if (signals.isEmpty()) {
            return base;
        }
        return base + " " + String.join(", ", signals) + "를 함께 참고했어요.";
    }
}
