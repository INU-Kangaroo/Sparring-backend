package com.kangaroo.sparring.domain.record.steps.service;

import com.kangaroo.sparring.domain.record.steps.dto.req.StepSyncRequest;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepTodayResponse;
import com.kangaroo.sparring.domain.record.steps.entity.StepLog;
import com.kangaroo.sparring.domain.record.steps.repository.StepLogRepository;
import com.kangaroo.sparring.domain.record.steps.type.StepSource;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
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
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class StepLogServiceTest {

    @Mock
    private StepLogRepository stepLogRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private UserLookupService userLookupService;
    @Mock
    private Clock kstClock;
    @InjectMocks
    private StepLogService stepLogService;

    @Test
    void 걸음수_동기화는_같은_날짜와_소스가_있으면_업데이트한다() {
        Long userId = 1L;
        LocalDate stepDate = LocalDate.of(2026, 4, 8);
        User user = User.builder().id(userId).email("a@a.com").password("pw").username("홍길동").build();
        StepLog existing = StepLog.create(user, stepDate, 3200, StepSource.APPLE_HEALTH, LocalDateTime.of(2026, 4, 8, 9, 0));
        StepSyncRequest request = new StepSyncRequest(stepDate, 5600, StepSource.APPLE_HEALTH);

        when(userLookupService.getUserOrThrow(userId)).thenReturn(user);
        when(stepLogRepository.findByUserIdAndStepDateAndSourceAndIsDeletedFalse(userId, stepDate, StepSource.APPLE_HEALTH))
                .thenReturn(Optional.of(existing));
        when(stepLogRepository.save(any(StepLog.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-08T10:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));

        var response = stepLogService.syncStepLog(userId, request);

        assertThat(response.getStepDate()).isEqualTo(stepDate);
        assertThat(response.getSource()).isEqualTo(StepSource.APPLE_HEALTH);
        assertThat(response.getSteps()).isEqualTo(5600);
    }

    @Test
    void 오늘_걸음수_조회는_합계를_반환한다() {
        Long userId = 1L;
        LocalDate today = LocalDate.of(2026, 4, 8);
        LocalDateTime updatedAt = LocalDateTime.of(2026, 4, 8, 20, 12);

        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-08T03:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(stepLogRepository.sumStepsByUserIdAndStepDate(userId, today)).thenReturn(8241);
        when(stepLogRepository.findLatestSyncedAtByUserIdAndStepDate(userId, today)).thenReturn(updatedAt);

        StepTodayResponse response = stepLogService.getTodaySteps(userId);

        assertThat(response.getStepDate()).isEqualTo(today);
        assertThat(response.getTotalSteps()).isEqualTo(8241);
        assertThat(response.getUpdatedAt()).isEqualTo(updatedAt);
    }
}
