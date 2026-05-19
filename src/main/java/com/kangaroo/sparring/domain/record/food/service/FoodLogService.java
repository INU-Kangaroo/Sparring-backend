package com.kangaroo.sparring.domain.record.food.service;

import com.kangaroo.sparring.domain.catalog.entity.Food;
import com.kangaroo.sparring.domain.catalog.repository.FoodRepository;
import com.kangaroo.sparring.domain.record.food.dto.req.FoodLogCreateRequest;
import com.kangaroo.sparring.domain.record.food.dto.res.FoodLogCreateResponse;
import com.kangaroo.sparring.domain.record.food.dto.res.FoodLogListItemResponse;
import com.kangaroo.sparring.domain.record.food.entity.FoodLog;
import com.kangaroo.sparring.domain.record.food.repository.FoodLogRepository;
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
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FoodLogService {

    private final FoodLogRepository foodLogRepository;
    private final FoodRepository foodRepository;
    private final UserRepository userRepository;
    private final UserLookupService userLookupService;
    private final Clock kstClock;

    @Transactional
    public FoodLogCreateResponse createFoodLog(Long userId, FoodLogCreateRequest request) {
        log.info("식사 기록 등록 시작: userId={}, foodId={}, eatenAmountGram={}",
                userId, request.getFoodId(), request.getEatenAmountGram());

        User user = userLookupService.getUserOrThrow(userId);
        Food food = foodRepository.findByIdWithNutrition(request.getFoodId())
                .orElseThrow(() -> new CustomException(ErrorCode.FOOD_NOT_FOUND));

        FoodLog foodLog = FoodLog.withFood(user, food, request.getMealTime(), request.getLoggedAt(), request.getEatenAmountGram());
        FoodLog saved = foodLogRepository.save(foodLog);
        log.info("식사 기록 등록 완료: foodLogId={}", saved.getId());

        return FoodLogCreateResponse.from(saved);
    }

    public List<FoodLogListItemResponse> getDailyFoodLogs(Long userId, LocalDate date) {
        LocalDate targetDate = date != null ? date : LocalDate.now(kstClock);
        return getFoodLogs(userId, targetDate.atStartOfDay(), targetDate.atTime(LocalTime.MAX));
    }

    public List<FoodLogListItemResponse> getFoodLogs(Long userId, LocalDateTime start, LocalDateTime end) {
        return foodLogRepository.findByUserIdAndEatenAtBetweenAndIsDeletedFalseOrderByEatenAtAsc(userId, start, end)
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
}
