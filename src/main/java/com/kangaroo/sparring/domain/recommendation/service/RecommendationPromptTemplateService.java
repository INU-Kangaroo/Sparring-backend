package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;
import java.util.Map;

@Component
public class RecommendationPromptTemplateService {

    private final String exerciseTemplate;
    private final String supplementTemplate;

    public RecommendationPromptTemplateService() {
        this.exerciseTemplate = readTemplate("prompts/recommendation/exercise_prompt.txt");
        this.supplementTemplate = readTemplate("prompts/recommendation/supplement_prompt.txt");
    }

    public String renderExercisePrompt(Map<String, String> variables) {
        return render(exerciseTemplate, variables);
    }

    public String renderSupplementPrompt(Map<String, String> variables) {
        return render(supplementTemplate, variables);
    }

    public String buildUserHealthInfo(
            HealthProfile healthProfile,
            List<BloodSugarRecord> bloodSugars,
            List<BloodPressureRecord> bloodPressures
    ) {
        StringBuilder prompt = new StringBuilder();

        Integer age = calculateAge(healthProfile.getBirthDate());
        if (age != null) {
            prompt.append(String.format("- 나이: %d세%n", age));
        }
        if (healthProfile.getGender() != null) {
            prompt.append(String.format("- 성별: %s%n", healthProfile.getGender().name()));
        }
        appendBigDecimal(prompt, "체중", healthProfile.getWeight(), "kg");
        appendBigDecimal(prompt, "키", healthProfile.getHeight(), "cm");

        if (!bloodSugars.isEmpty()) {
            double avgBloodSugar = bloodSugars.stream()
                    .mapToInt(BloodSugarRecord::getGlucoseLevel)
                    .average()
                    .orElse(0);
            prompt.append(String.format("- 평균 혈당: %.1fmg/dL%n", avgBloodSugar));
        }

        if (!bloodPressures.isEmpty()) {
            double avgSystolic = bloodPressures.stream()
                    .mapToInt(BloodPressureRecord::getSystolic)
                    .average()
                    .orElse(0);
            double avgDiastolic = bloodPressures.stream()
                    .mapToInt(BloodPressureRecord::getDiastolic)
                    .average()
                    .orElse(0);
            prompt.append(String.format("- 평균 혈압: %.0f/%.0fmmHg%n", avgSystolic, avgDiastolic));
        }

        return prompt.length() == 0 ? "- 건강 데이터 없음" : prompt.toString().trim();
    }

    private String render(String template, Map<String, String> variables) {
        String rendered = template;
        for (Map.Entry<String, String> entry : variables.entrySet()) {
            rendered = rendered.replace("{{" + entry.getKey() + "}}", entry.getValue());
        }
        return rendered;
    }

    private Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private void appendBigDecimal(StringBuilder prompt, String label, BigDecimal value, String unit) {
        if (value != null) {
            prompt.append(String.format("- %s: %.1f%s%n", label, value, unit));
        }
    }

    private String readTemplate(String path) {
        try {
            ClassPathResource resource = new ClassPathResource(path);
            return StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("프롬프트 템플릿을 로드할 수 없습니다: " + path, e);
        }
    }
}
