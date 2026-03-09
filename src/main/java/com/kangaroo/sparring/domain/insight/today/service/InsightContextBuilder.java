package com.kangaroo.sparring.domain.insight.today.service;

import com.kangaroo.sparring.domain.insight.today.type.InsightType;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.measurement.repository.BloodPressureLogRepository;
import com.kangaroo.sparring.domain.measurement.repository.BloodSugarLogRepository;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

@Component
@RequiredArgsConstructor
public class InsightContextBuilder {

    private final BloodSugarLogRepository bloodSugarLogRepository;
    private final BloodPressureLogRepository bloodPressureLogRepository;

    // 정상 범위 상수
    private static final int FASTING_SUGAR_NORMAL_MAX = 100;
    private static final int FASTING_SUGAR_HIGH = 126;
    private static final int SYSTOLIC_HIGH = 130;
    private static final int DAYS_LOOKBACK = 7;
    private static final int STABLE_CONSECUTIVE_DAYS = 3;

    public InsightContext build(Long userId) {
        LocalDateTime start = LocalDate.now().minusDays(DAYS_LOOKBACK).atStartOfDay();
        LocalDateTime end = LocalDateTime.now();

        List<BloodSugarLog> sugarLogs = bloodSugarLogRepository
                .findByUserIdAndDateRange(userId, start, end);
        List<BloodPressureLog> pressureLogs = bloodPressureLogRepository
                .findByUserIdAndDateRange(userId, start, end);

        if (sugarLogs.isEmpty() && pressureLogs.isEmpty()) {
            return InsightContext.of(InsightType.NO_DATA, sugarLogs, pressureLogs);
        }

        InsightType type = classify(sugarLogs, pressureLogs);
        return InsightContext.of(type, sugarLogs, pressureLogs);
    }

    private InsightType classify(List<BloodSugarLog> sugarLogs,
                                 List<BloodPressureLog> pressureLogs) {
        boolean sugarStable = isConsecutiveNormalFasting(sugarLogs);
        boolean sugarHigh = isConsecutiveHighSugar(sugarLogs);
        boolean pressureStable = isRecentPressureStable(pressureLogs);
        boolean pressureHigh = isRecentPressureHigh(pressureLogs);

        // 우선순위: 위험 신호 먼저
        if (sugarHigh) return InsightType.BLOOD_SUGAR_HIGH;
        if (pressureHigh) return InsightType.BLOOD_PRESSURE_HIGH;
        if (sugarStable && pressureStable) return InsightType.BOTH_STABLE;
        if (sugarStable) return InsightType.BLOOD_SUGAR_STABLE;
        if (pressureStable) return InsightType.BLOOD_PRESSURE_STABLE;

        // 데이터는 존재하지만 뚜렷한 안정/위험 패턴이 없는 경우
        return InsightType.NEEDS_MONITORING;
    }

    // 최근 N일 중 공복 혈당이 연속으로 정상(70~99)인지
    private boolean isConsecutiveNormalFasting(List<BloodSugarLog> logs) {
        if (logs.isEmpty()) return false;
        long normalStreakDays = consecutiveDays(logs.stream()
                .filter(l -> l.getMeasurementLabel().contains("공복"))
                .filter(l -> l.getGlucoseLevel() >= 70
                        && l.getGlucoseLevel() < FASTING_SUGAR_NORMAL_MAX)
                .map(l -> l.getMeasurementTime().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList());
        return normalStreakDays >= STABLE_CONSECUTIVE_DAYS;
    }

    private boolean isConsecutiveHighSugar(List<BloodSugarLog> logs) {
        if (logs.isEmpty()) return false;
        long highStreakDays = consecutiveDays(logs.stream()
                .filter(l -> l.getGlucoseLevel() >= FASTING_SUGAR_HIGH)
                .map(l -> l.getMeasurementTime().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList());
        return highStreakDays >= 2;
    }

    private boolean isRecentPressureStable(List<BloodPressureLog> logs) {
        if (logs.isEmpty()) return false;
        long stableStreakDays = consecutiveDays(logs.stream()
                .filter(l -> l.getSystolic() < SYSTOLIC_HIGH)
                .map(l -> l.getMeasuredAt().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList());
        return stableStreakDays >= STABLE_CONSECUTIVE_DAYS;
    }

    private boolean isRecentPressureHigh(List<BloodPressureLog> logs) {
        if (logs.isEmpty()) return false;
        long highStreakDays = consecutiveDays(logs.stream()
                .filter(l -> l.getSystolic() >= SYSTOLIC_HIGH)
                .map(l -> l.getMeasuredAt().toLocalDate())
                .distinct()
                .sorted(Comparator.reverseOrder())
                .toList());
        return highStreakDays >= 2;
    }

    private long consecutiveDays(List<LocalDate> datesDesc) {
        if (datesDesc.isEmpty()) {
            return 0;
        }
        long streak = 1;
        LocalDate prev = datesDesc.get(0);
        for (int i = 1; i < datesDesc.size(); i++) {
            LocalDate current = datesDesc.get(i);
            if (current.equals(prev.minusDays(1))) {
                streak++;
                prev = current;
                continue;
            }
            break;
        }
        return streak;
    }

    @Getter
    @Builder(builderClassName = "InsightContextLombokBuilder")
    public static class InsightContext {
        private InsightType type;
        private List<BloodSugarLog> sugarLogs;
        private List<BloodPressureLog> pressureLogs;

        // 프롬프트용 요약 데이터
        private Double avgGlucose;
        private Integer latestSystolic;

        public static InsightContext of(InsightType type,
                                        List<BloodSugarLog> sugarLogs,
                                        List<BloodPressureLog> pressureLogs) {
            Double avg = sugarLogs.isEmpty() ? null :
                    sugarLogs.stream().mapToInt(BloodSugarLog::getGlucoseLevel)
                            .average().orElse(0);
            Integer systolic = pressureLogs.isEmpty() ? null :
                    pressureLogs.get(pressureLogs.size() - 1).getSystolic();

            return InsightContext.builder()
                    .type(type)
                    .sugarLogs(sugarLogs)
                    .pressureLogs(pressureLogs)
                    .avgGlucose(avg)
                    .latestSystolic(systolic)
                    .build();
        }
    }
}
