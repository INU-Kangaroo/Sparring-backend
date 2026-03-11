package com.kangaroo.sparring.domain.prediction.service;

import com.kangaroo.sparring.domain.exercise.log.entity.ExerciseLog;
import com.kangaroo.sparring.domain.exercise.log.repository.ExerciseLogRepository;
import com.kangaroo.sparring.domain.food.catalog.entity.Food;
import com.kangaroo.sparring.domain.food.catalog.entity.MealNutrition;
import com.kangaroo.sparring.domain.food.catalog.repository.FoodRepository;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.measurement.repository.BloodSugarLogRepository;
import com.kangaroo.sparring.domain.prediction.client.MlServerClient;
import com.kangaroo.sparring.domain.prediction.dto.res.GlucosePredictionResponse;
import com.kangaroo.sparring.domain.survey.type.DrinkingFrequency;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlucosePredictionService {

    private static final int GLUCOSE_HISTORY_COUNT = 3;

    private final BloodSugarLogRepository bloodSugarLogRepository;
    private final ExerciseLogRepository exerciseLogRepository;
    private final FoodRepository foodRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final MlServerClient mlServerClient;

    public GlucosePredictionResponse predictGlucose(User user, Long foodId) {
        // 음식 + 영양정보 조회
        Food food = foodRepository.findByIdWithNutrition(foodId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOOD_NOT_FOUND));

        MealNutrition nutrition = food.getMealNutrition();
        double carbIntake = (nutrition != null) ? nutrition.getCarbs() : 0.0;

        // 건강 프로필 조회
        HealthProfile profile = healthProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new CustomException(ErrorCode.HEALTH_PROFILE_NOT_FOUND));

        // 최근 혈당 기록 3개 조회
        List<BloodSugarLog> recentLogs = bloodSugarLogRepository
                .findRecentByUserId(user.getId(), PageRequest.of(0, GLUCOSE_HISTORY_COUNT));

        if (recentLogs.isEmpty()) {
            throw new CustomException(ErrorCode.INSUFFICIENT_DATA_FOR_PREDICTION);
        }

        List<Double> glucoseHistory = recentLogs.stream()
                .map(log -> (double) log.getGlucoseLevel())
                .toList();

        // ML 요청 필드 조립
        int mealType = resolveMealType(LocalTime.now());
        double age = resolveAge(profile);
        int sex = resolveSex(profile.getGender());
        double weight = (profile.getWeight() != null) ? profile.getWeight().doubleValue() : 60.0;
        int caffeine = Boolean.TRUE.equals(profile.getCaffeineIntake()) ? 1 : 0;
        int medication = (profile.getMedications() != null && !profile.getMedications().isBlank()) ? 1 : 0;
        int alcohol = resolveAlcohol(profile.getDrinkingFrequency());
        double intensity = resolveTodayIntensity(user.getId());

        // ML 서버 호출
        GlucosePredictionResponse.ForecastPoint[] points = mlServerClient.predictGlucose(
                glucoseHistory, carbIntake, mealType, age, sex, weight,
                caffeine, medication, alcohol, intensity
        );
        if (points.length == 0) {
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "예측 포인트가 없습니다.");
        }

        // peak 계산
        GlucosePredictionResponse.ForecastPoint peak = Arrays.stream(points)
                .max(Comparator.comparingDouble(GlucosePredictionResponse.ForecastPoint::getPredictedGlucose))
                .orElse(points[0]);

        return GlucosePredictionResponse.of(
                food.getName(),
                Arrays.asList(points),
                peak.getPredictedGlucose(),
                peak.getOffsetMinutes()
        );
    }

    // 현재 시각 기준 식사 타입 (1:아침, 2:점심, 3:저녁, 4:간식)
    private int resolveMealType(LocalTime time) {
        int hour = time.getHour();
        if (hour >= 7 && hour < 12) return 1;
        if (hour >= 12 && hour < 18) return 2;
        if (hour >= 18 && hour < 24) return 3;
        return 4; // 0~6시 간식
    }

    private double resolveAge(HealthProfile profile) {
        if (profile.getBirthDate() == null) return 30.0;
        return LocalDate.now().getYear() - profile.getBirthDate().getYear();
    }

    // FEMALE=0, MALE=1
    private int resolveSex(Gender gender) {
        if (gender == null) return 0;
        return gender == Gender.MALE ? 1 : 0;
    }

    private int resolveAlcohol(DrinkingFrequency frequency) {
        if (frequency == null || frequency == DrinkingFrequency.NONE) return 0;
        return 1;
    }

    // 오늘 운동 기록에서 intensity 계산 (metValue 기준, 없으면 0)
    private double resolveTodayIntensity(Long userId) {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        LocalDateTime endOfDay = LocalDate.now().atTime(LocalTime.MAX);

        List<ExerciseLog> todayLogs = exerciseLogRepository
                .findByUserIdAndLoggedAtBetweenAndIsDeletedFalse(userId, startOfDay, endOfDay);

        return todayLogs.stream()
                .mapToDouble(ExerciseLog::getMetValue)
                .map(met -> Math.min(met / 12.0, 1.0))
                .max()
                .orElse(0.0);
    }
}
