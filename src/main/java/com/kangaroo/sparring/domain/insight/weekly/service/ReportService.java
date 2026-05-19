package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.record.common.ExerciseRecord;
import com.kangaroo.sparring.domain.record.common.FoodRecord;
import com.kangaroo.sparring.domain.record.common.BloodPressureRecord;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.DailyConditionEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.HighlightEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ImprovementEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ReportEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.res.ReportListItemResponse;
import com.kangaroo.sparring.domain.insight.weekly.dto.res.ReportResponse;
import com.kangaroo.sparring.domain.insight.weekly.dto.res.ReportResponse.DailyCondition;
import com.kangaroo.sparring.domain.insight.weekly.dto.res.ReportResponse.HighlightItem;
import com.kangaroo.sparring.domain.insight.weekly.dto.res.ReportResponse.ImprovementDetail;
import com.kangaroo.sparring.domain.insight.weekly.entity.Report;
import com.kangaroo.sparring.domain.insight.weekly.repository.ReportRepository;
import com.kangaroo.sparring.domain.insight.weekly.type.ImprovementCategory;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final ReportPersistenceService reportPersistenceService;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final RecordReadService recordReadService;
    private final ReportGeminiService reportGeminiService;
    private final ReportRuleEngine reportRuleEngine;
    private final ReportCacheService reportCacheService;
    private final Clock kstClock;
    @Qualifier("reportAiExecutor")
    private final Executor reportAiExecutor;

    public ReportResponse getReportByDate(Long userId, LocalDate date) {
        long startedAt = System.currentTimeMillis();
        log.info("주간 리포트 조회 시작: userId={}, date={}", userId, date);
        LocalDate baseDate = date != null ? date : LocalDate.now(kstClock);
        LocalDate monday = getMonday(baseDate);
        LocalDate sunday = monday.plusDays(6);

        Optional<Report> existing = reportRepository.findByUserIdAndStartDate(userId, monday);
        if (existing.isPresent()) {
            ReportResponse response = getOrBuildCachedResponse(userId, monday, sunday, existing.get());
            log.info("주간 리포트 조회 완료(기존 리포트): userId={}, startDate={}, elapsedMs={}",
                    userId, monday, System.currentTimeMillis() - startedAt);
            return response;
        }

        if (shouldPersistFinalReport(sunday)) {
            ReportResponse response = generateAndSave(userId, monday, sunday);
            log.info("주간 리포트 조회 완료(신규 저장): userId={}, startDate={}, elapsedMs={}",
                    userId, monday, System.currentTimeMillis() - startedAt);
            return response;
        }
        ReportResponse response = generatePreview(userId, monday, sunday);
        log.info("주간 리포트 조회 완료(미리보기): userId={}, startDate={}, elapsedMs={}",
                userId, monday, System.currentTimeMillis() - startedAt);
        return response;
    }

    public Page<ReportListItemResponse> getReportHistory(Long userId, Integer year, Integer month, int page, int size) {
        long startedAt = System.currentTimeMillis();
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "startDate")
        );
        Page<ReportListItemResponse> result = reportRepository.findPageByUserIdAndYearMonth(userId, year, month, pageable)
                .map(ReportListItemResponse::from);
        log.debug("리포트 히스토리 조회 완료: userId={}, year={}, month={}, page={}, size={}, returned={}, elapsedMs={}",
                userId, year, month, page, size, result.getNumberOfElements(), System.currentTimeMillis() - startedAt);
        return result;
    }

    public ReportResponse getReport(Long userId, Long reportId) {
        long startedAt = System.currentTimeMillis();
        log.info("리포트 단건 조회 시작: userId={}, reportId={}", userId, reportId);
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        ReportResponse response = getOrBuildCachedResponse(userId, report.getStartDate(), report.getEndDate(), report);
        log.info("리포트 단건 조회 완료: userId={}, reportId={}, elapsedMs={}",
                userId, reportId, System.currentTimeMillis() - startedAt);
        return response;
    }

    private ReportResponse generateAndSave(Long userId, LocalDate monday, LocalDate sunday) {
        log.info("주간 리포트 생성+저장 시작: userId={}, startDate={}, endDate={}", userId, monday, sunday);
        GeneratedReport generated = generateReport(userId, monday, sunday);
        Report report = generated.report();
        ReportEvidence evidence = generated.evidence();

        try {
            reportPersistenceService.save(report);
            ReportResponse response = toResponse(report, evidence);
            reportCacheService.cache(userId, monday, response);
            return response;
        } catch (DataIntegrityViolationException ex) {
            log.warn("주간 리포트 저장 충돌 감지, 기존 데이터 재사용: userId={}, startDate={}", userId, monday);
            return reportRepository.findByUserIdAndStartDate(userId, monday)
                    .map(existing -> getOrBuildCachedResponse(userId, monday, sunday, existing))
                    .orElseThrow(() -> ex);
        }
    }

    private ReportResponse generatePreview(Long userId, LocalDate monday, LocalDate sunday) {
        log.info("주간 리포트 미리보기 생성 시작: userId={}, startDate={}, endDate={}", userId, monday, sunday);
        GeneratedReport generated = generateReport(userId, monday, sunday);
        ReportResponse response = toResponse(generated.report(), generated.evidence());
        reportCacheService.cache(userId, monday, response);
        return response;
    }

    private GeneratedReport generateReport(Long userId, LocalDate monday, LocalDate sunday) {
        long startedAt = System.currentTimeMillis();
        User user = userLookupService.getUserOrThrow(userId);

        WeeklyLogs logs = fetchWeeklyLogs(userId, monday, sunday);
        validateWeeklyLogs(logs);

        ReportEvidence evidence = reportRuleEngine.evaluate(
                monday,
                logs.bloodSugarLogs(),
                logs.bloodPressureLogs(),
                logs.foodLogs(),
                logs.exerciseLogs()
        );
        GeneratedAiContent generatedAiContent = generateAiContent(evidence);
        ImprovementFields improvementFields = toImprovementFields(evidence.improvement());

        Report report = Report.create(
                user,
                monday,
                sunday,
                evidence.recordDays(),
                evidence.bloodSugarRecordDays(),
                evidence.bloodPressureRecordDays(),
                evidence.score().overallScore(),
                evidence.score().healthManagement(),
                evidence.score().measurementConsistency(),
                evidence.score().lifestyle(),
                generatedAiContent.comment(),
                improvementFields.category(),
                improvementFields.timeLabel(),
                improvementFields.detail(),
                generatedAiContent.tips()
        );
        log.info("주간 리포트 생성 완료: userId={}, startDate={}, score={}, elapsedMs={}",
                userId, monday, evidence.score().overallScore(), System.currentTimeMillis() - startedAt);
        return new GeneratedReport(report, evidence);
    }

    private void validateWeeklyLogs(WeeklyLogs logs) {
        if (logs.bloodSugarLogs().isEmpty()
                && logs.bloodPressureLogs().isEmpty()
                && logs.foodLogs().isEmpty()
                && logs.exerciseLogs().isEmpty()) {
            log.warn("주간 리포트 생성 불가: 기록 데이터 없음");
            throw new CustomException(ErrorCode.REPORT_INSUFFICIENT_DATA);
        }
    }

    private GeneratedAiContent generateAiContent(ReportEvidence evidence) {
        long startedAt = System.currentTimeMillis();
        CompletableFuture<String> commentFuture =
                CompletableFuture.supplyAsync(
                        () -> reportGeminiService.generateComment(evidence),
                        reportAiExecutor
                );

        CompletableFuture<List<String>> tipsFuture = generateTipsFuture(evidence.improvement());
        GeneratedAiContent content = new GeneratedAiContent(commentFuture.join(), tipsFuture.join());
        log.info("주간 리포트 AI 생성 완료: score={}, tips={}, elapsedMs={}",
                evidence.score().overallScore(), content.tips().size(), System.currentTimeMillis() - startedAt);
        return content;
    }

    private CompletableFuture<List<String>> generateTipsFuture(ImprovementEvidence improvement) {
        if (improvement == null) {
            return CompletableFuture.completedFuture(List.of());
        }
        return CompletableFuture.supplyAsync(
                () -> splitTips(reportGeminiService.generateImprovementTips(improvement)),
                reportAiExecutor
        );
    }

    private ImprovementFields toImprovementFields(ImprovementEvidence improvement) {
        if (improvement == null) {
            return ImprovementFields.empty();
        }
        return new ImprovementFields(
                improvement.category().name(),
                improvement.timeLabel(),
                improvement.detail()
        );
    }

    private ReportResponse buildResponse(Report report, Long userId, LocalDate monday, LocalDate sunday) {
        WeeklyLogs logs = fetchWeeklyLogs(userId, monday, sunday);

        ReportEvidence evidence = reportRuleEngine.evaluate(
                monday,
                logs.bloodSugarLogs(),
                logs.bloodPressureLogs(),
                logs.foodLogs(),
                logs.exerciseLogs()
        );

        return toResponse(report, evidence);
    }

    private ReportResponse getOrBuildCachedResponse(Long userId, LocalDate monday, LocalDate sunday, Report report) {
        return reportCacheService.getCached(userId, monday)
                .orElseGet(() -> {
                    ReportResponse response = buildResponse(report, userId, monday, sunday);
                    reportCacheService.cache(userId, monday, response);
                    return response;
                });
    }

    private WeeklyLogs fetchWeeklyLogs(Long userId, LocalDate monday, LocalDate sunday) {
        long startedAt = System.currentTimeMillis();
        LocalDateTime startDt = monday.atStartOfDay();
        LocalDateTime endDt = sunday.atTime(LocalTime.MAX);

        List<BloodSugarRecord> bsLogs = recordReadService.getBloodSugarRecords(userId, startDt, endDt);
        List<BloodPressureRecord> bpLogs = recordReadService.getBloodPressureRecords(userId, startDt, endDt);
        List<FoodRecord> foodLogs = recordReadService.getFoodRecords(userId, startDt, endDt);
        List<ExerciseRecord> exerciseLogs = recordReadService.getExerciseRecords(userId, startDt, endDt);

        WeeklyLogs logs = new WeeklyLogs(bsLogs, bpLogs, foodLogs, exerciseLogs);
        log.debug("주간 리포트 데이터 조회 완료: userId={}, startDate={}, bs={}, bp={}, food={}, exercise={}, elapsedMs={}",
                userId, monday, bsLogs.size(), bpLogs.size(), foodLogs.size(), exerciseLogs.size(),
                System.currentTimeMillis() - startedAt);
        return logs;
    }

    private ReportResponse toResponse(Report report, ReportEvidence evidence) {
        List<DailyCondition> dailyConditions = evidence.dailyConditions().stream()
                .map(this::toDailyCondition)
                .toList();

        List<HighlightItem> highlights = evidence.highlights().stream()
                .map(this::toHighlightItem)
                .toList();

        ImprovementDetail improvement = null;
        if (report.getImprovementCategory() != null) {
            improvement = ImprovementDetail.builder()
                    .category(ImprovementCategory.valueOf(report.getImprovementCategory()))
                    .timeLabel(report.getImprovementTimeLabel())
                    .detail(report.getImprovementDetail())
                    .tips(report.getImprovementTips())
                    .build();
        }

        int overallScore = evidence.score().overallScore();
        return ReportResponse.builder()
                .reportId(report.getId())
                .startDate(report.getStartDate())
                .endDate(report.getEndDate())
                .recordDays(report.getRecordDays())
                .bloodSugarRecordDays(report.getBloodSugarRecordDays())
                .bloodPressureRecordDays(report.getBloodPressureRecordDays())
                .aiComment(report.getAiComment())
                .overallScore(overallScore)
                .scoreLabel(buildScoreLabel(overallScore))
                .scores(ReportResponse.ScoreDetail.builder()
                        .healthManagement(evidence.score().healthManagement())
                        .measurementConsistency(evidence.score().measurementConsistency())
                        .lifestyle(evidence.score().lifestyle())
                        .build())
                .dailyConditions(dailyConditions)
                .highlights(highlights)
                .improvement(improvement)
                .build();
    }

    private DailyCondition toDailyCondition(DailyConditionEvidence evidence) {
        return DailyCondition.builder()
                .dayOfWeek(evidence.dayOfWeek())
                .status(evidence.status())
                .build();
    }

    private HighlightItem toHighlightItem(HighlightEvidence evidence) {
        return HighlightItem.builder()
                .type(evidence.type())
                .message(evidence.message())
                .build();
    }

    private LocalDate getMonday(LocalDate date) {
        return date.with(DayOfWeek.MONDAY);
    }

    private String buildScoreLabel(int score) {
        if (score >= 90) return "완벽해요! 정말 잘하고 있어요 🌟";
        if (score >= 75) return "잘하고 있어요! 조금만 더 💪";
        if (score >= 60) return "괜찮아요. 조금 더 신경 써볼까요?";
        return "이번 주는 조금 아쉬웠어요. 다음 주엔 더 잘할 수 있어요! 🌱";
    }

    private boolean shouldPersistFinalReport(LocalDate sunday) {
        LocalDate today = LocalDate.now(kstClock);
        return today.isAfter(sunday);
    }

    private List<String> splitTips(String tips) {
        if (tips == null || tips.isBlank()) return List.of();
        return Arrays.stream(tips.split("\\n"))
                .map(String::trim)
                .map(s -> s.replaceFirst("^[\\-•\\d.\\)\\s]+", "").trim())
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private record WeeklyLogs(
            List<BloodSugarRecord> bloodSugarLogs,
            List<BloodPressureRecord> bloodPressureLogs,
            List<FoodRecord> foodLogs,
            List<ExerciseRecord> exerciseLogs
    ) {
    }

    private record GeneratedReport(
            Report report,
            ReportEvidence evidence
    ) {
    }

    private record ImprovementFields(
            String category,
            String timeLabel,
            String detail
    ) {
        private static ImprovementFields empty() {
            return new ImprovementFields(null, null, null);
        }
    }

    private record GeneratedAiContent(
            String comment,
            List<String> tips
    ) {
    }
}
