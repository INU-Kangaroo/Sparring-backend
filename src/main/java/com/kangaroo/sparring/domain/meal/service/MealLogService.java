package com.kangaroo.sparring.domain.meal.service;

import com.kangaroo.sparring.domain.food.entity.Food;
import com.kangaroo.sparring.domain.food.repository.FoodRepository;
import com.kangaroo.sparring.domain.meal.dto.req.MealLogCreateRequest;
import com.kangaroo.sparring.domain.meal.dto.res.MealLogResponse;
import com.kangaroo.sparring.domain.meal.entity.MealLog;
import com.kangaroo.sparring.domain.meal.repository.MealLogRepository;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class MealLogService {

    private final MealLogRepository mealLogRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final Clock kstClock;

    @Transactional
    public MealLogResponse createMealLog(Long userId, MealLogCreateRequest request) {
        log.info("식사 기록 등록 시작: userId={}, foodId={}, eatenAmountGram={}",
                userId, request.getFoodId(), request.getEatenAmountGram());

        User user = findUserById(userId);
        Food food = foodRepository.findByIdWithNutrition(request.getFoodId())
                .orElseThrow(() -> new CustomException(ErrorCode.FOOD_NOT_FOUND));
        LocalDateTime eatenAt = toTodayKstDateTime(request.getEatenTime());

        MealLog mealLog = MealLog.withFood(user, food, request.getMealTime(), eatenAt, request.getEatenAmountGram());
        MealLog saved = mealLogRepository.save(mealLog);
        log.info("식사 기록 등록 완료: mealLogId={}", saved.getId());

        return MealLogResponse.from(saved);
    }

    public List<MealLogResponse> getDailyMealLogs(Long userId, LocalDate date) {
        User user = findUserById(userId);
        LocalDate targetDate = date != null ? date : LocalDate.now(kstClock);

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        return mealLogRepository.findByUserAndEatenAtBetweenAndIsDeletedFalseOrderByEatenAtAsc(user, start, end)
                .stream()
                .map(MealLogResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteMealLog(Long userId, Long mealLogId) {
        MealLog mealLog = mealLogRepository.findByIdAndIsDeletedFalse(mealLogId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEAL_LOG_NOT_FOUND));

        if (!mealLog.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.LOG_ACCESS_DENIED);
        }
        mealLog.delete();
    }

    private LocalDateTime toTodayKstDateTime(LocalTime eatenTime) {
        if (eatenTime == null) throw new CustomException(ErrorCode.INVALID_INPUT);
        return LocalDateTime.of(LocalDate.now(kstClock), eatenTime);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
