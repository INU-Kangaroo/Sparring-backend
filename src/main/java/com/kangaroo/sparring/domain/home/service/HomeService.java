package com.kangaroo.sparring.domain.home.service;

import com.kangaroo.sparring.domain.home.dto.res.MainHomeResponse;
import com.kangaroo.sparring.domain.insight.today.dto.res.TodayInsightResponse;
import com.kangaroo.sparring.domain.insight.today.service.InsightService;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepTodayResponse;
import com.kangaroo.sparring.domain.record.steps.service.StepLogService;
import com.kangaroo.sparring.domain.user.dto.res.UserHomeCardResponse;
import com.kangaroo.sparring.domain.user.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class HomeService {

    private final UserProfileService userProfileService;
    private final InsightService insightService;
    private final RecordReadService recordReadService;
    private final StepLogService stepLogService;

    @Qualifier("kstClock")
    private final Clock kstClock;

    public MainHomeResponse getMainHome(Long userId) {
        long startedAt = System.currentTimeMillis();
        log.info("메인 홈 조회 시작: userId={}", userId);
        LocalDate today = LocalDate.now(kstClock);
        LocalDate startDate = today.minusDays(6);
        LocalDateTime start = startDate.atStartOfDay();
        LocalDateTime end = today.atTime(LocalTime.MAX);

        UserHomeCardResponse profileCard = userProfileService.getHomeCard(userId);
        TodayInsightResponse insight = insightService.getTodayInsight(userId);
        StepTodayResponse steps = stepLogService.getTodaySteps(userId);

        var bloodSugarRecords = recordReadService.getBloodSugarRecords(userId, start, end);

        MainHomeResponse response = MainHomeResponse.builder()
                .profileCard(profileCard)
                .todayInsight(MainHomeResponse.TodayInsight.builder()
                        .type(insight.getType())
                        .message(insight.getMessage())
                        .build())
                .bloodSugarChart(MainHomeResponse.BloodSugarChart.builder()
                        .startDate(startDate)
                        .endDate(today)
                        .points(buildDailyAveragePoints(startDate, today, bloodSugarRecords))
                        .build())
                .steps(steps)
                .build();
        log.info("메인 홈 조회 완료: userId={}, bloodSugarRecords={}, elapsedMs={}",
                userId, bloodSugarRecords.size(), System.currentTimeMillis() - startedAt);
        return response;
    }

    private List<MainHomeResponse.Point> buildDailyAveragePoints(
            LocalDate startDate,
            LocalDate endDate,
            List<com.kangaroo.sparring.domain.record.common.BloodSugarRecord> records
    ) {
        Map<LocalDate, List<Integer>> dailyValues = new LinkedHashMap<>();
        LocalDate cursor = startDate;
        while (!cursor.isAfter(endDate)) {
            dailyValues.put(cursor, new ArrayList<>());
            cursor = cursor.plusDays(1);
        }

        for (var record : records) {
            LocalDate date = record.getMeasurementTime().toLocalDate();
            List<Integer> values = dailyValues.get(date);
            if (values != null && record.getGlucoseLevel() != null) {
                values.add(record.getGlucoseLevel());
            }
        }

        List<MainHomeResponse.Point> points = new ArrayList<>();
        for (var entry : dailyValues.entrySet()) {
            BigDecimal avg = null;
            if (!entry.getValue().isEmpty()) {
                int sum = entry.getValue().stream().mapToInt(Integer::intValue).sum();
                avg = BigDecimal.valueOf((double) sum / entry.getValue().size()).setScale(1, RoundingMode.HALF_UP);
            }
            points.add(MainHomeResponse.Point.builder()
                    .date(entry.getKey())
                    .averageBloodSugarMgDl(avg)
                    .build());
        }
        return points;
    }
}
