package com.kangaroo.sparring.domain.record.exercise.service;

import com.kangaroo.sparring.domain.record.exercise.dto.req.ExerciseLogRequest;
import com.kangaroo.sparring.domain.record.exercise.dto.res.ExerciseLogCreateResponse;
import com.kangaroo.sparring.domain.record.exercise.dto.res.ExerciseLogListItemResponse;
import com.kangaroo.sparring.domain.record.exercise.entity.ExerciseLog;
import com.kangaroo.sparring.domain.record.exercise.repository.ExerciseLogRepository;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.domain.user.service.UserLookupService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

@Slf4j
@Service
@Transactional(readOnly = true)
@RequiredArgsConstructor
public class ExerciseLogService {

    private final ExerciseLogRepository exerciseLogRepository;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final HealthProfileRepository healthProfileRepository;
    private final ExerciseMatcher exerciseMatcher;
    private final Clock kstClock;

    @Transactional
    public ExerciseLogCreateResponse createExerciseLog(Long userId, ExerciseLogRequest request) {
        User user = userLookupService.getUserOrThrow(userId);
        HealthProfile healthProfile = findHealthProfile(userId);

        double weightKg = healthProfile.getWeight() != null
                ? healthProfile.getWeight().doubleValue()
                : 65.0; // 체중 미입력 시 기본값

        // MET 매핑
        ExerciseMatcher.MatchResult matchResult = exerciseMatcher.match(
                request.getExerciseName(),
                request.getIntensity()
        );

        // 칼로리 계산: MET × 체중(kg) × 시간(h)
        double durationHours = request.getDurationMinutes() / 60.0;
        double caloriesBurned = matchResult.metValue() * weightKg * durationHours;
        caloriesBurned = Math.round(caloriesBurned * 10.0) / 10.0; // 소수점 1자리

        ExerciseLog exerciseLog = ExerciseLog.of(
                user,
                request.getExerciseName(),
                matchResult.matchedName(),
                matchResult.metValue(),
                request.getDurationMinutes(),
                caloriesBurned,
                request.getLoggedAt()
        );

        ExerciseLog saved = exerciseLogRepository.save(exerciseLog);
        log.info("운동 기록 저장: userId={}, exercise={}, calories={}kcal",
                userId, request.getExerciseName(), caloriesBurned);

        return ExerciseLogCreateResponse.from(saved);
    }

    public List<ExerciseLogListItemResponse> getDailyExerciseLogs(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(kstClock);
        return getExerciseLogs(userId, targetDate.atStartOfDay(), targetDate.atTime(LocalTime.MAX));
    }

    public List<ExerciseLogListItemResponse> getExerciseLogs(Long userId, LocalDateTime start, LocalDateTime end) {
        return exerciseLogRepository.findByUserIdAndLoggedAtBetweenAndIsDeletedFalseOrderByLoggedAtDesc(userId, start, end)
                .stream()
                .map(ExerciseLogListItemResponse::from)
                .toList();
    }

    private HealthProfile findHealthProfile(Long userId) {
        return healthProfileRepository.findByUserId(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.HEALTH_PROFILE_NOT_FOUND));
    }
}
