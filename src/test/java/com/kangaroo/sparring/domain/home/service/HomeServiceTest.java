package com.kangaroo.sparring.domain.home.service;

import com.kangaroo.sparring.domain.insight.today.dto.res.TodayInsightResponse;
import com.kangaroo.sparring.domain.insight.today.service.InsightService;
import com.kangaroo.sparring.domain.insight.today.type.InsightType;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepTodayResponse;
import com.kangaroo.sparring.domain.record.steps.service.StepLogService;
import com.kangaroo.sparring.domain.user.dto.res.UserHomeCardResponse;
import com.kangaroo.sparring.domain.user.service.UserProfileService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HomeServiceTest {

    @Mock
    private UserProfileService userProfileService;
    @Mock
    private InsightService insightService;
    @Mock
    private RecordReadService recordReadService;
    @Mock
    private StepLogService stepLogService;
    @Mock
    private Clock kstClock;
    @InjectMocks
    private HomeService homeService;

    @Test
    void 메인_응답은_카드_인사이트_그래프_기록상태_걸음수를_한번에_반환한다() {
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 4, 8);
        UserHomeCardResponse card = UserHomeCardResponse.builder()
                .name("홍길동")
                .displayDate("2026년 4월 8일 수요일")
                .tags(List.of("23세", "여성"))
                .tagCandidates(List.of(UserHomeCardResponse.TagCandidate.builder().type("AGE").label("23세").build()))
                .build();

        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-08T03:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(userProfileService.getHomeCard(userId)).thenReturn(card);
        when(insightService.getTodayInsight(userId)).thenReturn(TodayInsightResponse.of(InsightType.BOTH_STABLE, "좋은 흐름이에요"));
        when(stepLogService.getTodaySteps(userId)).thenReturn(StepTodayResponse.builder()
                .stepDate(today)
                .totalSteps(8241)
                .updatedAt(LocalDateTime.of(2026, 4, 8, 20, 10))
                .build());
        when(recordReadService.getBloodSugarRecords(eq(userId), any(), any())).thenReturn(List.of(
                new BloodSugarRecord(120, LocalDateTime.of(2026, 4, 7, 8, 0), "공복"),
                new BloodSugarRecord(100, LocalDateTime.of(2026, 4, 8, 8, 0), "공복")
        ));

        var response = homeService.getMainHome(userId);

        assertThat(response.getProfileCard().getName()).isEqualTo("홍길동");
        assertThat(response.getTodayInsight().getType()).isEqualTo(InsightType.BOTH_STABLE);
        assertThat(response.getSteps().getTotalSteps()).isEqualTo(8241);
        assertThat(response.getBloodSugarChart().getPoints()).hasSize(7);
        assertThat(response.getBloodSugarChart().getPoints().stream()
                .filter(point -> point.getAverageBloodSugarMgDl() != null))
                .hasSize(2);
    }
}
