package com.kangaroo.sparring.domain.record.exercise.service;

import com.kangaroo.sparring.domain.catalog.entity.Exercise;
import com.kangaroo.sparring.domain.catalog.repository.ExerciseRepository;
import com.kangaroo.sparring.domain.common.type.ExerciseIntensity;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExerciseMatcher {

    private static final double MET_LOW    = 3.5;
    private static final double MET_MODERATE = 5.0;
    private static final double MET_HIGH   = 8.0;

    private final ExerciseRepository exerciseRepository;

    /**
     * 운동명 + 강도로 MET 값 조회
     * 1단계: 정확히 일치
     * 2단계: 부분 문자열 + 강도 매핑
     * 3단계: 키워드 분해 후 재시도
     * 4단계: 강도 기반 기본값 폴백
     */
    public MatchResult match(String exerciseName, ExerciseIntensity intensity) {
        String trimmed = exerciseName.trim();

        // 1단계: 정확히 일치
        Optional<Exercise> exact = exerciseRepository.findByExerciseName(trimmed);
        if (exact.isPresent()) {
            log.debug("운동 정확 매핑: {} → MET {}", trimmed, exact.get().getMetValue());
            return MatchResult.matched(exact.get().getExerciseName(), exact.get().getMetValue());
        }

        // 2단계: 부분 일치 + 강도 필터
        List<Exercise> partials = exerciseRepository.findByExerciseNameContaining(trimmed);
        if (!partials.isEmpty()) {
            Optional<Exercise> withIntensity = filterByIntensity(partials, intensity);
            Exercise selected = withIntensity.orElse(partials.get(0));
            log.debug("운동 부분 매핑: {} → {} (MET {})", trimmed, selected.getExerciseName(), selected.getMetValue());
            return MatchResult.matched(selected.getExerciseName(), selected.getMetValue());
        }

        // 3단계: 첫 단어로 키워드 재검색
        String keyword = trimmed.contains(" ") ? trimmed.split(" ")[0] : trimmed;
        if (!keyword.equals(trimmed)) {
            List<Exercise> byKeyword = exerciseRepository.findByExerciseNameContaining(keyword);
            if (!byKeyword.isEmpty()) {
                Optional<Exercise> withIntensity = filterByIntensity(byKeyword, intensity);
                Exercise selected = withIntensity.orElse(byKeyword.get(0));
                log.debug("운동 키워드 매핑: {} → {} (MET {})", trimmed, selected.getExerciseName(), selected.getMetValue());
                return MatchResult.matched(selected.getExerciseName(), selected.getMetValue());
            }
        }

        // 4단계: 강도 기반 폴백
        double fallbackMet = getFallbackMet(intensity);
        log.warn("운동 매핑 실패, 강도 기반 폴백: {} → intensity={}, MET={}", trimmed, intensity, fallbackMet);
        return MatchResult.fallback(fallbackMet);
    }

    private Optional<Exercise> filterByIntensity(List<Exercise> candidates, ExerciseIntensity intensity) {
        return candidates.stream()
                .filter(e -> e.getIntensity() == intensity)
                .findFirst();
    }

    private double getFallbackMet(ExerciseIntensity intensity) {
        return switch (intensity) {
            case LOW    -> MET_LOW;
            case MODERATE -> MET_MODERATE;
            case HIGH   -> MET_HIGH;
        };
    }

    public record MatchResult(String matchedName, double metValue, boolean isFallback) {
        public static MatchResult matched(String name, double met) {
            return new MatchResult(name, met, false);
        }
        public static MatchResult fallback(double met) {
            return new MatchResult(null, met, true);
        }
    }
}
