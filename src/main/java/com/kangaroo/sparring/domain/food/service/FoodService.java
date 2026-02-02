package com.kangaroo.sparring.domain.food.service;

import com.kangaroo.sparring.domain.food.dto.res.FoodDetailResponse;
import com.kangaroo.sparring.domain.food.dto.res.FoodResponse;
import com.kangaroo.sparring.domain.food.entity.Food;
import com.kangaroo.sparring.domain.food.entity.MealNutrition;
import com.kangaroo.sparring.domain.food.repository.FoodRepository;
import com.kangaroo.sparring.domain.food.repository.MealNutritionRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 음식 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FoodService {

    private final FoodRepository foodRepository;
    private final MealNutritionRepository mealNutritionRepository;
    // 외부 음식 API 연동 시 주입
    // private final ExternalFoodApiClient externalFoodApiClient;

    /**
     * 음식 검색
     * 1. DB에서 먼저 검색 (캐시)
     * 2. 없으면 외부 API 호출
     */
    public List<FoodResponse> searchFood(String keyword) {
        String normalizedKeyword = normalizeRequiredText(keyword);
        log.info("음식 검색 시작: keyword={}", normalizedKeyword);

        // 1. DB에서 먼저 검색
        List<Food> foods = foodRepository.searchActiveByName(normalizedKeyword);

        if (foods.isEmpty()) {
            log.info("DB에 음식 없음, 외부 API 호출 필요: keyword={}", normalizedKeyword);
            // TODO: 외부 API 호출 및 DB 저장
            // foods = externalFoodApiClient.searchAndCache(normalizedKeyword);
        }

        return foods.stream()
                .map(FoodResponse::from)
                .collect(Collectors.toList());
    }

    /**
     * 음식 상세 조회 (영양 정보 포함)
     */
    public FoodDetailResponse getFoodDetail(Long foodId) {
        log.info("음식 상세 조회 시작: foodId={}", foodId);

        Food food = foodRepository.findByIdWithNutrition(foodId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOOD_NOT_FOUND));

        MealNutrition nutrition = food.getMealNutrition();

        return FoodDetailResponse.from(food, nutrition);
    }

    /**
     * 음식 생성 (관리자용 또는 외부 API 캐싱용)
     */
    @Transactional
    public Long createFood(String name, Double servingSize, String servingUnit,
                           Double calories, Double carbs, Double protein, Double fat) {
        String normalizedName = normalizeRequiredText(name);
        String normalizedServingUnit = normalizeRequiredText(servingUnit);
        validateServingSize(servingSize);
        validateNutrition(calories, carbs, protein, fat);

        log.info("음식 생성 시작: name={}", normalizedName);

        // 중복 확인
        foodRepository.findActiveByNormalizedName(normalizedName).ifPresent(food -> {
            throw new CustomException(ErrorCode.DUPLICATE_FOOD);
        });

        // 음식 생성
        Food food = Food.create(normalizedName, servingSize, normalizedServingUnit);
        Food savedFood = foodRepository.save(food);

        // 영양 정보 생성
        MealNutrition nutrition = MealNutrition.create(savedFood, calories, carbs, protein, fat);
        mealNutritionRepository.save(nutrition);

        log.info("음식 생성 완료: foodId={}", savedFood.getId());
        return savedFood.getId();
    }

    /**
     * 음식 삭제 (소프트 삭제)
     */
    @Transactional
    public void deleteFood(Long foodId) {
        log.info("음식 삭제 시작: foodId={}", foodId);

        Food food = foodRepository.findById(foodId)
                .orElseThrow(() -> new CustomException(ErrorCode.FOOD_NOT_FOUND));

        food.delete();

        log.info("음식 삭제 완료: foodId={}", foodId);
    }

    private String normalizeRequiredText(String value) {
        if (value == null) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        String normalized = value.trim();
        if (normalized.isEmpty()) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return normalized;
    }

    private void validateServingSize(Double servingSize) {
        if (servingSize == null || servingSize <= 0d) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
    }

    private void validateNutrition(Double calories, Double carbs, Double protein, Double fat) {
        List<Double> values = java.util.Arrays.asList(calories, carbs, protein, fat);
        for (Double value : values) {
            if (Objects.isNull(value) || value < 0d) {
                throw new CustomException(ErrorCode.INVALID_INPUT);
            }
        }
    }
}
