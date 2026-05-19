package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.recommendation.dto.ml.MealRecommendationMlRequest;
import com.kangaroo.sparring.domain.recommendation.dto.ml.MealRecommendationMlResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.MealRecommendationResponse;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MealRecommendationAiClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ml.recommendation.url}")
    private String serverUrl;

    @Value("${ml.recommendation.path:/api/v1/recommendations}")
    private String recommendPath;

    public AiRecommendResult recommend(MealRecommendationMlRequest requestBody) {
        long startedAt = System.currentTimeMillis();
        String endpoint = serverUrl + recommendPath;
        log.info("FastAPI 식단 추천 호출 시작: endpoint={}", endpoint);

        try {
            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(endpoint)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody)
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            AiRecommendResult result = parseResponse(responseBody);
            log.info("FastAPI 식단 추천 호출 성공: endpoint={}, elapsedMs={}, cards={}",
                    endpoint, System.currentTimeMillis() - startedAt, result.cards().size());
            return result;
        } catch (WebClientResponseException e) {
            log.error("FastAPI 식단 추천 HTTP 오류: endpoint={}, status={}, elapsedMs={}, body={}",
                    endpoint, e.getStatusCode(), System.currentTimeMillis() - startedAt, e.getResponseBodyAsString(), e);
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED);
        } catch (Exception e) {
            log.error("FastAPI 식단 추천 호출 실패: endpoint={}, elapsedMs={}",
                    endpoint, System.currentTimeMillis() - startedAt, e);
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED);
        }
    }

    private AiRecommendResult parseResponse(String responseBody) {
        try {
            MealRecommendationMlResponse response = objectMapper.readValue(responseBody, MealRecommendationMlResponse.class);

            List<MealRecommendationResponse.RecommendationCardDto> cards = new ArrayList<>();
            response.recommendationsOrEmpty().forEach(item -> {
                List<MealRecommendationResponse.MenuItemDto> menus = new ArrayList<>();
                item.recipesOrEmpty().forEach(r -> {
                    menus.add(MealRecommendationResponse.MenuItemDto.builder()
                            .id(r.recipeIdOrZero())
                            .name(r.recipeNameOrEmpty())
                            .kcal(r.kcalOrZero())
                            .carbs(r.carbsOrZero())
                            .protein(r.proteinOrZero())
                            .fat(r.fatOrZero())
                            .sodium(r.sodium())
                            .build());
                });

                cards.add(MealRecommendationResponse.RecommendationCardDto.builder()
                        .rank(item.rankOrZero())
                        .title(item.titleOrEmpty())
                        .nutrients(MealRecommendationResponse.NutrientsDto.builder()
                                .kcal(item.totalKcalOrZero())
                                .carbs(item.totalCarbsOrZero())
                                .protein(item.totalProteinOrZero())
                                .fat(item.totalFatOrZero())
                                .sodium(item.totalSodium())
                                .build())
                        .reasons(item.reasonsOrEmpty())
                        .menus(menus)
                        .build());
            });

            return new AiRecommendResult(response.mealType(), response.mealTargetKcalOrZero(), cards);

        } catch (Exception e) {
            log.error("FastAPI 응답 파싱 실패: {}", responseBody, e);
            throw new CustomException(ErrorCode.AI_RECOMMENDATION_FAILED);
        }
    }

    public record AiRecommendResult(
            String mealType,
            double mealTargetKcal,
            List<MealRecommendationResponse.RecommendationCardDto> cards
    ) {}
}
