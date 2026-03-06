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
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Stream;
import java.util.stream.Collectors;

/**
 * 음식 서비스
 */
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class FoodService {
    private static final int DEFAULT_SEARCH_LIMIT = 20;
    private static final int MAX_SEARCH_LIMIT = 50;
    private static final int DEFAULT_SEARCH_PAGE = 0;
    private static final int MAX_SEARCH_PAGE = 1000;
    private static final int EXACT_CANDIDATE_LIMIT = 30;
    private static final int PREFIX_CANDIDATE_LIMIT = 150;
    private static final int CONTAINS_CANDIDATE_LIMIT_PER_TERM = 250;
    private static final int CANDIDATE_HARD_LIMIT = 2000;

    private static final Map<String, List<String>> TOKEN_SYNONYM_MAP = Map.ofEntries(
            Map.entry("닭가슴살", List.of("닭가슴", "닭고기", "치킨브레스트", "chicken breast", "chickenbreast")),
            Map.entry("닭", List.of("치킨", "chicken")),
            Map.entry("고구마", List.of("sweet potato", "sweetpotato")),
            Map.entry("현미", List.of("brown rice", "brownrice")),
            Map.entry("샐러드", List.of("salad"))
    );

    private final FoodRepository foodRepository;
    private final MealNutritionRepository mealNutritionRepository;

    /**
     * 음식 검색
     * DB에서 음식명으로 검색
     */
    public List<FoodResponse> searchFood(String keyword, Integer limit, Integer page) {
        String normalizedKeyword = normalizeRequiredText(keyword);
        int validatedLimit = validateLimit(limit);
        int validatedPage = validatePage(page);
        long offset = (long) validatedPage * validatedLimit;
        Set<String> searchTerms = buildSearchTerms(normalizedKeyword);
        log.info(
                "음식 검색 시작: keyword={}, limit={}, page={}, terms={}",
                normalizedKeyword,
                validatedLimit,
                validatedPage,
                searchTerms.size()
        );

        Map<Long, Food> candidates = new LinkedHashMap<>();
        addCandidates(candidates, foodRepository.searchActiveByExactName(
                normalizedKeyword,
                PageRequest.of(0, EXACT_CANDIDATE_LIMIT)
        ));
        addCandidates(candidates, foodRepository.searchActiveByPrefixName(
                normalizedKeyword,
                PageRequest.of(0, PREFIX_CANDIDATE_LIMIT)
        ));

        for (String searchTerm : searchTerms) {
            if (candidates.size() >= CANDIDATE_HARD_LIMIT) {
                break;
            }
            List<Food> found = foodRepository.searchActiveByName(
                    searchTerm,
                    PageRequest.of(0, CONTAINS_CANDIDATE_LIMIT_PER_TERM)
            );
            addCandidates(candidates, found);
        }

        return candidates.values().stream()
                .sorted(Comparator
                        .comparingInt((Food food) -> scoreFoodName(food.getName(), normalizedKeyword, searchTerms))
                        .reversed()
                        .thenComparing(Food::getName, String.CASE_INSENSITIVE_ORDER))
                .skip(offset)
                .limit(validatedLimit)
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
     * 음식 생성 (관리자용)
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

    private int validateLimit(Integer limit) {
        if (limit == null) {
            return DEFAULT_SEARCH_LIMIT;
        }
        if (limit <= 0 || limit > MAX_SEARCH_LIMIT) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return limit;
    }

    private int validatePage(Integer page) {
        if (page == null) {
            return DEFAULT_SEARCH_PAGE;
        }
        if (page < 0 || page > MAX_SEARCH_PAGE) {
            throw new CustomException(ErrorCode.INVALID_INPUT);
        }
        return page;
    }

    private Set<String> buildSearchTerms(String keyword) {
        String normalizedKeyword = normalizeForMatch(keyword);

        Stream<String> tokenExpandedTerms = Arrays.stream(keyword.split("\\s+"))
                .map(this::normalizeForMatch)
                .filter(token -> !token.isEmpty())
                .flatMap(token -> Stream.concat(
                        Stream.of(token),
                        TOKEN_SYNONYM_MAP.getOrDefault(token, List.of()).stream().map(this::normalizeForMatch)
                ));

        Stream<String> keywordSynonyms = TOKEN_SYNONYM_MAP.getOrDefault(normalizedKeyword, List.of()).stream()
                .map(this::normalizeForMatch);

        return Stream.concat(
                        Stream.of(normalizedKeyword, keyword.toLowerCase(Locale.ROOT).trim()),
                        Stream.concat(keywordSynonyms, tokenExpandedTerms)
                )
                .filter(term -> !term.isBlank())
                .collect(Collectors.toCollection(java.util.LinkedHashSet::new));
    }

    private int scoreFoodName(String foodName, String keyword, Set<String> searchTerms) {
        String normalizedName = normalizeForMatch(foodName);
        String normalizedKeyword = normalizeForMatch(keyword);
        String lowerFoodName = foodName.toLowerCase(Locale.ROOT);
        String lowerKeyword = keyword.toLowerCase(Locale.ROOT).trim();

        int score = 0;

        if (lowerFoodName.equals(lowerKeyword) || normalizedName.equals(normalizedKeyword)) {
            score += 1000;
        }
        if (lowerFoodName.startsWith(lowerKeyword) || normalizedName.startsWith(normalizedKeyword)) {
            score += 600;
        }
        if (lowerFoodName.contains(lowerKeyword) || normalizedName.contains(normalizedKeyword)) {
            score += 400;
        }
        if (normalizedName.equals(normalizedKeyword)) {
            score += 600;
        } else if (normalizedName.startsWith(normalizedKeyword + " ")) {
            score += 220;
        }

        int lengthGap = Math.abs(normalizedName.length() - normalizedKeyword.length());
        score += Math.max(0, 140 - (lengthGap * 4));

        if (foodName.startsWith("[") || foodName.startsWith("(")) {
            score -= 80;
        }
        if (normalizedName.length() - normalizedKeyword.length() >= 12) {
            score -= 50;
        }

        for (String searchTerm : searchTerms) {
            String normalizedTerm = normalizeForMatch(searchTerm);
            if (normalizedTerm.isEmpty() || normalizedTerm.equals(normalizedKeyword)) {
                continue;
            }
            if (normalizedName.startsWith(normalizedTerm)) {
                score += 120;
            } else if (normalizedName.contains(normalizedTerm)) {
                score += 70;
            }
        }

        for (String token : splitTokens(normalizedKeyword)) {
            if (!token.isBlank() && normalizedName.contains(token)) {
                score += 20;
            }
        }

        return score;
    }

    private List<String> splitTokens(String text) {
        List<String> tokens = new ArrayList<>();
        for (String token : text.split("\\s+")) {
            String normalized = normalizeForMatch(token);
            if (!normalized.isBlank()) {
                tokens.add(normalized);
            }
        }
        return tokens;
    }

    private String normalizeForMatch(String value) {
        if (value == null) {
            return "";
        }
        return value.toLowerCase(Locale.ROOT)
                .replaceAll("\\[[^\\]]*\\]", " ")
                .replaceAll("\\([^)]*\\)", " ")
                .replaceAll("[^0-9a-z가-힣\\s]", " ")
                .replaceAll("\\s+", " ")
                .trim();
    }

    private void addCandidates(Map<Long, Food> candidates, List<Food> foods) {
        for (Food food : foods) {
            if (candidates.size() >= CANDIDATE_HARD_LIMIT) {
                break;
            }
            candidates.putIfAbsent(food.getId(), food);
        }
    }
}
