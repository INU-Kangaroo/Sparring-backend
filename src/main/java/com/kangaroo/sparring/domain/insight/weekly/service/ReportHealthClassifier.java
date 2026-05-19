package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.common.health.HealthThresholds;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;

import java.util.List;

/**
 * 혈당·혈압·식사 측정값 분류 술어 모음.
 * policy 의존 없이 HealthThresholds 상수만 사용하므로 static 유틸로 관리.
 */
final class ReportHealthClassifier {

    private ReportHealthClassifier() {}

    static boolean isBloodSugarNormal(BloodSugarRecord log) {
        int g = log.getGlucoseLevel();
        String label = log.getMeasurementLabel();
        if (label.contains("공복") || label.contains("식전")) {
            return g <= HealthThresholds.BLOOD_SUGAR_FASTING_NORMAL_MAX;
        }
        return g <= HealthThresholds.BLOOD_SUGAR_POST_MEAL_NORMAL_MAX;
    }

    static boolean isBloodPressureNormal(BloodPressureRecord log) {
        return log.getSystolic() <= HealthThresholds.BLOOD_PRESSURE_SYSTOLIC_NORMAL_MAX
                && log.getDiastolic() <= HealthThresholds.BLOOD_PRESSURE_DIASTOLIC_NORMAL_MAX;
    }

    static boolean isPostMealBloodSugarHigh(BloodSugarRecord log) {
        return log.getGlucoseLevel() > HealthThresholds.BLOOD_SUGAR_POST_MEAL_NORMAL_MAX;
    }

    static long countHypertensionLogs(List<BloodPressureRecord> logs) {
        return logs.stream()
                .filter(l -> l.getSystolic() >= HealthThresholds.BLOOD_PRESSURE_HYPERTENSION_SYSTOLIC
                        || l.getDiastolic() >= HealthThresholds.BLOOD_PRESSURE_HYPERTENSION_DIASTOLIC)
                .count();
    }

    static String toMealTimeSlotLabel(BloodSugarRecord log) {
        int hour = log.getMeasurementTime().getHour();
        if (hour >= 6 && hour < 11) return "아침 식후";
        if (hour >= 11 && hour < 15) return "점심 식후";
        if (hour >= 17 && hour < 22) return "저녁 식후";
        return "기타 식후";
    }

    static double calcAfterMealHighRatio(List<BloodSugarRecord> logs) {
        if (logs.isEmpty()) return 0.0;
        long highCount = logs.stream().filter(ReportHealthClassifier::isPostMealBloodSugarHigh).count();
        return (double) highCount / logs.size();
    }
}
