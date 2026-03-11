package com.kangaroo.sparring.domain.food.log.service;

import com.kangaroo.sparring.domain.food.catalog.entity.Food;
import com.kangaroo.sparring.domain.food.catalog.repository.FoodRepository;
import com.kangaroo.sparring.domain.food.log.dto.req.FoodLogCreateRequest;
import com.kangaroo.sparring.domain.food.log.dto.res.FoodLogCreateResponse;
import com.kangaroo.sparring.domain.food.log.dto.res.FoodLogListItemResponse;
import com.kangaroo.sparring.domain.food.log.entity.FoodLog;
import com.kangaroo.sparring.domain.food.log.repository.FoodLogRepository;
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
public class FoodLogService {

    private final FoodLogRepository foodLogRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final Clock kstClock;

    @Transactional
    public FoodLogCreateResponse createFoodLog(Long userId, FoodLogCreateRequest request) {
        log.info("식사 기록 등록 시작: userId={}, foodId={}, eatenAmountGram={}",
                userId, request.getFoodId(), request.getEatenAmountGram());

        User user = findUserById(userId);
        Food food = foodRepository.findByIdWithNutrition(request.getFoodId())
                .orElseThrow(() -> new CustomException(ErrorCode.FOOD_NOT_FOUND));

        FoodLog foodLog = FoodLog.withFood(user, food, request.getMealTime(), request.getLoggedAt(), request.getEatenAmountGram());
        FoodLog saved = foodLogRepository.save(foodLog);
        log.info("식사 기록 등록 완료: foodLogId={}", saved.getId());

        return FoodLogCreateResponse.from(saved);
    }

    public List<FoodLogListItemResponse> getDailyFoodLogs(Long userId, LocalDate date) {
        User user = findUserById(userId);
        LocalDate targetDate = date != null ? date : LocalDate.now(kstClock);

        LocalDateTime start = targetDate.atStartOfDay();
        LocalDateTime end = targetDate.atTime(LocalTime.MAX);

        return foodLogRepository.findByUserAndEatenAtBetweenAndIsDeletedFalseOrderByEatenAtAsc(user, start, end)
                .stream()
                .map(FoodLogListItemResponse::from)
                .collect(Collectors.toList());
    }

    @Transactional
    public void deleteFoodLog(Long userId, Long foodLogId) {
        FoodLog foodLog = foodLogRepository.findByIdAndIsDeletedFalse(foodLogId)
                .orElseThrow(() -> new CustomException(ErrorCode.MEAL_LOG_NOT_FOUND));

        if (!foodLog.getUser().getId().equals(userId)) {
            throw new CustomException(ErrorCode.LOG_ACCESS_DENIED);
        }
        foodLog.delete();
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }
}
