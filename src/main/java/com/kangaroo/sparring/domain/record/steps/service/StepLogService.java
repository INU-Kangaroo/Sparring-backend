package com.kangaroo.sparring.domain.record.steps.service;

import com.kangaroo.sparring.domain.record.steps.dto.req.StepSyncRequest;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepDailyResponse;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepSyncResponse;
import com.kangaroo.sparring.domain.record.steps.dto.res.StepTodayResponse;
import com.kangaroo.sparring.domain.record.steps.entity.StepLog;
import com.kangaroo.sparring.domain.record.steps.repository.StepLogRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class StepLogService {

    private final StepLogRepository stepLogRepository;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;

    @Qualifier("kstClock")
    private final Clock kstClock;

    @Transactional
    public StepSyncResponse syncStepLog(Long userId, StepSyncRequest request) {
        long startedAt = System.currentTimeMillis();
        log.info("걸음수 동기화 시작: userId={}, stepDate={}, source={}, steps={}",
                userId, request.getStepDate(), request.getSource(), request.getSteps());
        User user = userLookupService.getUserOrThrow(userId);

        LocalDate today = LocalDate.now(kstClock);
        if (request.getStepDate().isAfter(today)) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "걸음수 날짜는 미래일 수 없습니다.");
        }

        LocalDateTime now = LocalDateTime.now(kstClock);
        StepLog stepLog = stepLogRepository.findByUserIdAndStepDateAndSourceAndIsDeletedFalse(
                        userId,
                        request.getStepDate(),
                        request.getSource()
                )
                .map(existing -> {
                    existing.updateSteps(request.getSteps(), now);
                    return existing;
                })
                .orElseGet(() -> StepLog.create(
                        user,
                        request.getStepDate(),
                        request.getSteps(),
                        request.getSource(),
                        now
                ));

        StepLog saved = stepLogRepository.save(stepLog);
        StepSyncResponse response = StepSyncResponse.from(saved);
        log.info("걸음수 동기화 완료: userId={}, logId={}, elapsedMs={}",
                userId, saved.getId(), System.currentTimeMillis() - startedAt);
        return response;
    }

    public StepTodayResponse getTodaySteps(Long userId) {
        long startedAt = System.currentTimeMillis();
        LocalDate today = LocalDate.now(kstClock);
        Integer totalSteps = stepLogRepository.sumStepsByUserIdAndStepDate(userId, today);
        int resolvedSteps = totalSteps != null ? totalSteps : 0;

        LocalDateTime updatedAt = stepLogRepository.findLatestSyncedAtByUserIdAndStepDate(userId, today);

        StepTodayResponse response = StepTodayResponse.builder()
                .stepDate(today)
                .totalSteps(resolvedSteps)
                .updatedAt(updatedAt)
                .build();
        log.debug("오늘 걸음수 조회 완료: userId={}, totalSteps={}, elapsedMs={}",
                userId, resolvedSteps, System.currentTimeMillis() - startedAt);
        return response;
    }

    public List<StepDailyResponse> getStepLogs(Long userId, LocalDateTime start, LocalDateTime end) {
        long startedAt = System.currentTimeMillis();
        List<StepDailyResponse> logs = stepLogRepository.findDailyStepsByUserIdAndDateRange(
                userId,
                start.toLocalDate(),
                end.toLocalDate()
        );
        log.debug("걸음수 기간 조회 완료: userId={}, start={}, end={}, days={}, elapsedMs={}",
                userId, start, end, logs.size(), System.currentTimeMillis() - startedAt);
        return logs;
    }
}
