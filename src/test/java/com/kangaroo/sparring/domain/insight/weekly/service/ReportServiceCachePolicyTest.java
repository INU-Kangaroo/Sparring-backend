package com.kangaroo.sparring.domain.insight.weekly.service;

import com.kangaroo.sparring.domain.insight.weekly.dto.internal.CommentEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ReportEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.internal.ScoreEvidence;
import com.kangaroo.sparring.domain.insight.weekly.dto.res.ReportResponse;
import com.kangaroo.sparring.domain.insight.weekly.entity.Report;
import com.kangaroo.sparring.domain.insight.weekly.repository.ReportRepository;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.Executor;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class ReportServiceCachePolicyTest {
    private static final Executor DIRECT_EXECUTOR = Runnable::run;

    @Test
    void getReportByDate_whenCacheHit_shouldSkipRecalculation() {
        ReportRepository reportRepository = mock(ReportRepository.class);
        ReportPersistenceService reportPersistenceService = mock(ReportPersistenceService.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserLookupService userLookupService = mock(UserLookupService.class);
        RecordReadService recordReadService = mock(RecordReadService.class);
        ReportGeminiService reportGeminiService = mock(ReportGeminiService.class);
        ReportRuleEngine reportRuleEngine = mock(ReportRuleEngine.class);
        ReportCacheService reportCacheService = mock(ReportCacheService.class);

        ReportService service = new ReportService(
                reportRepository,
                reportPersistenceService,
                userRepository,
                userLookupService,
                recordReadService,
                reportGeminiService,
                reportRuleEngine,
                reportCacheService,
                Clock.system(ZoneId.of("Asia/Seoul")),
                DIRECT_EXECUTOR
        );

        User user = User.builder().id(1L).email("a@a.com").password("x").username("u").build();
        Report report = Report.create(
                user,
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 4, 5),
                7, 7, 7,
                80, 75, 70, 65,
                "comment",
                null, null, null, List.of()
        );

        ReportResponse cached = ReportResponse.builder()
                .reportId(99L)
                .overallScore(88)
                .build();

        when(reportRepository.findByUserIdAndStartDate(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.of(report));
        when(reportCacheService.getCached(1L, LocalDate.of(2026, 3, 30)))
                .thenReturn(Optional.of(cached));

        ReportResponse response = service.getReportByDate(1L, LocalDate.of(2026, 4, 1));

        assertThat(response.getOverallScore()).isEqualTo(88);
        verifyNoInteractions(recordReadService, reportRuleEngine);
        verify(reportCacheService, never()).cache(anyLong(), any(LocalDate.class), any(ReportResponse.class));
    }

    @Test
    void getReportByDate_whenCacheMiss_shouldComputeAndCacheResponse() {
        ReportRepository reportRepository = mock(ReportRepository.class);
        ReportPersistenceService reportPersistenceService = mock(ReportPersistenceService.class);
        UserRepository userRepository = mock(UserRepository.class);
        UserLookupService userLookupService = mock(UserLookupService.class);
        RecordReadService recordReadService = mock(RecordReadService.class);
        ReportGeminiService reportGeminiService = mock(ReportGeminiService.class);
        ReportRuleEngine reportRuleEngine = mock(ReportRuleEngine.class);
        ReportCacheService reportCacheService = mock(ReportCacheService.class);

        ReportService service = new ReportService(
                reportRepository,
                reportPersistenceService,
                userRepository,
                userLookupService,
                recordReadService,
                reportGeminiService,
                reportRuleEngine,
                reportCacheService,
                Clock.system(ZoneId.of("Asia/Seoul")),
                DIRECT_EXECUTOR
        );

        User user = User.builder().id(1L).email("a@a.com").password("x").username("u").build();
        Report report = Report.create(
                user,
                LocalDate.of(2026, 3, 30),
                LocalDate.of(2026, 4, 5),
                7, 7, 7,
                80, 75, 70, 65,
                "comment",
                null, null, null, List.of()
        );

        when(reportRepository.findByUserIdAndStartDate(anyLong(), any(LocalDate.class)))
                .thenReturn(Optional.of(report));
        when(reportCacheService.getCached(1L, LocalDate.of(2026, 3, 30)))
                .thenReturn(Optional.empty());
        when(recordReadService.getBloodSugarRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(recordReadService.getBloodPressureRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(recordReadService.getFoodRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(recordReadService.getExerciseRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(reportRuleEngine.evaluate(any(), any(), any(), any(), any()))
                .thenReturn(new ReportEvidence(
                        0, 0, 0,
                        new ScoreEvidence(90, 80, 70, 85),
                        List.of(), List.of(), null,
                        new CommentEvidence(null, null, null, null, 0)
                ));

        service.getReportByDate(1L, LocalDate.of(2026, 4, 1));

        verify(reportCacheService).cache(anyLong(), any(LocalDate.class), any(ReportResponse.class));
    }
}
