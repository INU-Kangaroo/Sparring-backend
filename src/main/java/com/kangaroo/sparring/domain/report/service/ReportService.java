package com.kangaroo.sparring.domain.report.service;

import com.kangaroo.sparring.domain.exercise.log.entity.ExerciseLog;
import com.kangaroo.sparring.domain.exercise.log.repository.ExerciseLogRepository;
import com.kangaroo.sparring.domain.meal.entity.MealLog;
import com.kangaroo.sparring.domain.meal.repository.MealLogRepository;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.measurement.repository.BloodPressureLogRepository;
import com.kangaroo.sparring.domain.measurement.repository.BloodSugarLogRepository;
import com.kangaroo.sparring.domain.report.dto.internal.DailyConditionEvidence;
import com.kangaroo.sparring.domain.report.dto.internal.HighlightEvidence;
import com.kangaroo.sparring.domain.report.dto.internal.ImprovementEvidence;
import com.kangaroo.sparring.domain.report.dto.internal.ReportEvidence;
import com.kangaroo.sparring.domain.report.dto.res.ReportListItemResponse;
import com.kangaroo.sparring.domain.report.dto.res.ReportResponse;
import com.kangaroo.sparring.domain.report.dto.res.ReportResponse.DailyCondition;
import com.kangaroo.sparring.domain.report.dto.res.ReportResponse.HighlightItem;
import com.kangaroo.sparring.domain.report.dto.res.ReportResponse.ImprovementDetail;
import com.kangaroo.sparring.domain.report.entity.Report;
import com.kangaroo.sparring.domain.report.repository.ReportRepository;
import com.kangaroo.sparring.domain.report.type.ImprovementCategory;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final UserRepository userRepository;
    private final BloodSugarLogRepository bloodSugarLogRepository;
    private final BloodPressureLogRepository bloodPressureLogRepository;
    private final MealLogRepository mealLogRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final ReportGeminiService reportGeminiService;
    private final ReportRuleEngine reportRuleEngine;

    @Transactional
    public ReportResponse getReportByDate(Long userId, LocalDate date) {
        LocalDate baseDate = date != null ? date : LocalDate.now();
        LocalDate monday = getMonday(baseDate);
        LocalDate sunday = monday.plusDays(6);

        Optional<Report> existing = reportRepository.findByUserIdAndStartDate(userId, monday);
        if (existing.isPresent()) {
            return buildResponse(existing.get(), userId, monday, sunday);
        }

        return generateAndSave(userId, monday, sunday);
    }

    public Page<ReportListItemResponse> getReportHistory(Long userId, Integer year, Integer month, int page, int size) {
        PageRequest pageable = PageRequest.of(
                Math.max(page, 0),
                Math.min(Math.max(size, 1), 100),
                Sort.by(Sort.Direction.DESC, "startDate")
        );

        return reportRepository.findPageByUserIdAndYearMonth(userId, year, month, pageable)
                .map(ReportListItemResponse::from);
    }

    public ReportResponse getReport(Long userId, Long reportId) {
        Report report = reportRepository.findByIdAndUserId(reportId, userId)
                .orElseThrow(() -> new CustomException(ErrorCode.REPORT_NOT_FOUND));

        return buildResponse(report, userId, report.getStartDate(), report.getEndDate());
    }

    @Transactional
    private ReportResponse generateAndSave(Long userId, LocalDate monday, LocalDate sunday) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        WeeklyLogs logs = fetchWeeklyLogs(userId, monday, sunday);

        if (logs.bloodSugarLogs().isEmpty()
                && logs.bloodPressureLogs().isEmpty()
                && logs.mealLogs().isEmpty()
                && logs.exerciseLogs().isEmpty()) {
            throw new CustomException(ErrorCode.REPORT_INSUFFICIENT_DATA);
        }

        ReportEvidence evidence = reportRuleEngine.evaluate(
                monday,
                logs.bloodSugarLogs(),
                logs.bloodPressureLogs(),
                logs.mealLogs(),
                logs.exerciseLogs()
        );

        String aiComment = reportGeminiService.generateComment(evidence);

        String improvementCategory = null;
        String improvementTimeLabel = null;
        String improvementDetail = null;
        List<String> improvementTips = List.of();

        if (evidence.improvement() != null) {
            improvementCategory = evidence.improvement().category().name();
            improvementTimeLabel = evidence.improvement().timeLabel();
            improvementDetail = evidence.improvement().detail();
            String rawTips = reportGeminiService.generateImprovementTips(evidence.improvement());
            improvementTips = splitTips(rawTips);
        }

        Report report = Report.create(
                user,
                monday,
                sunday,
                evidence.recordDays(),
                evidence.bloodSugarMeasurements(),
                evidence.bloodPressureMeasurements(),
                evidence.score().overallScore(),
                evidence.score().healthManagement(),
                evidence.score().measurementConsistency(),
                evidence.score().lifestyle(),
                aiComment,
                improvementCategory,
                improvementTimeLabel,
                improvementDetail,
                improvementTips
        );
        reportRepository.save(report);

        return toResponse(report, evidence);
    }

    private ReportResponse buildResponse(Report report, Long userId, LocalDate monday, LocalDate sunday) {
        WeeklyLogs logs = fetchWeeklyLogs(userId, monday, sunday);

        ReportEvidence evidence = reportRuleEngine.evaluate(
                monday,
                logs.bloodSugarLogs(),
                logs.bloodPressureLogs(),
                logs.mealLogs(),
                logs.exerciseLogs()
        );

        return toResponse(report, evidence);
    }

    private WeeklyLogs fetchWeeklyLogs(Long userId, LocalDate monday, LocalDate sunday) {
        LocalDateTime startDt = monday.atStartOfDay();
        LocalDateTime endDt = sunday.atTime(LocalTime.MAX);

        List<BloodSugarLog> bsLogs = bloodSugarLogRepository
                .findByUserIdAndMeasurementTimeBetweenAndIsDeletedFalseOrderByMeasurementTimeAsc(
                        userId, startDt, endDt);
        List<BloodPressureLog> bpLogs = bloodPressureLogRepository
                .findByUserIdAndMeasuredAtBetweenAndIsDeletedFalse(userId, startDt, endDt);
        List<MealLog> mealLogs = mealLogRepository
                .findByUserIdAndEatenAtBetweenAndIsDeletedFalse(userId, startDt, endDt);
        List<ExerciseLog> exerciseLogs = exerciseLogRepository
                .findByUserIdAndLoggedAtBetweenAndIsDeletedFalse(userId, startDt, endDt);

        return new WeeklyLogs(bsLogs, bpLogs, mealLogs, exerciseLogs);
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

        return ReportResponse.of(
                report,
                buildScoreLabel(report.getOverallScore()),
                dailyConditions,
                highlights,
                improvement
        );
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

    private List<String> splitTips(String tips) {
        if (tips == null || tips.isBlank()) return List.of();
        return Arrays.stream(tips.split("\\n"))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
    }

    private record WeeklyLogs(
            List<BloodSugarLog> bloodSugarLogs,
            List<BloodPressureLog> bloodPressureLogs,
            List<MealLog> mealLogs,
            List<ExerciseLog> exerciseLogs
    ) {
    }
}
