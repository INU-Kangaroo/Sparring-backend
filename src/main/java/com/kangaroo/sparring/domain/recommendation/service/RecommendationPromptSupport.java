package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.List;

public final class RecommendationPromptSupport {

    private RecommendationPromptSupport() {
    }

    public static String buildUserHealthInfo(HealthProfile healthProfile,
                                             List<BloodSugarLog> bloodSugars,
                                             List<BloodPressureLog> bloodPressures) {
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
                    .mapToInt(BloodSugarLog::getGlucoseLevel)
                    .average()
                    .orElse(0);
            prompt.append(String.format("- 평균 혈당: %.1fmg/dL%n", avgBloodSugar));
        }

        if (!bloodPressures.isEmpty()) {
            double avgSystolic = bloodPressures.stream()
                    .mapToInt(BloodPressureLog::getSystolic)
                    .average()
                    .orElse(0);
            double avgDiastolic = bloodPressures.stream()
                    .mapToInt(BloodPressureLog::getDiastolic)
                    .average()
                    .orElse(0);
            prompt.append(String.format("- 평균 혈압: %.0f/%.0fmmHg%n", avgSystolic, avgDiastolic));
        }

        if (prompt.length() == 0) {
            return "- 건강 데이터 없음";
        }

        return prompt.toString().trim();
    }

    private static Integer calculateAge(LocalDate birthDate) {
        if (birthDate == null) {
            return null;
        }
        return Period.between(birthDate, LocalDate.now()).getYears();
    }

    private static void appendBigDecimal(StringBuilder prompt, String label, BigDecimal value, String unit) {
        if (value != null) {
            prompt.append(String.format("- %s: %.1f%s%n", label, value, unit));
        }
    }
}
