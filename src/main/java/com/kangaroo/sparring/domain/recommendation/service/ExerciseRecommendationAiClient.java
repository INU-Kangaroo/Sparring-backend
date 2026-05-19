package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.kangaroo.sparring.domain.recommendation.dto.res.CardiacExerciseResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.ExerciseRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.StrengthExerciseResponse;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.client.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class ExerciseRecommendationAiClient {

    private static final Pattern RANGE_NUMBER_FIELD_PATTERN =
            Pattern.compile("(\"[^\"]+\"\\s*:\\s*)(\\d+\\s*-\\s*\\d+)(\\s*[,}])");

    private final GeminiApiClient geminiApiClient;
    private final RecommendationJsonMappingSupport jsonMappingSupport;

    public ExerciseRecommendationResponse recommend(String prompt) {
        String geminiResponse = geminiApiClient.generateContent(prompt);
        return parseExerciseResponse(geminiResponse);
    }

    private ExerciseRecommendationResponse parseExerciseResponse(String geminiResponse) {
        try {
            String sanitizedJson = sanitizeRangeNumberFields(jsonMappingSupport.extractJsonObject(geminiResponse));
            JsonNode root = jsonMappingSupport.readTreeOrThrow(sanitizedJson, geminiResponse, "Gemini 운동 추천 응답");

            List<CardiacExerciseResponse> cardiacExercises = new ArrayList<>();
            JsonNode cardiacNode = root.path("cardiacExercises");
            if (cardiacNode.isArray()) {
                for (JsonNode node : cardiacNode) {
                    CaloriesRange caloriesRange = parseCaloriesRange(node);
                    cardiacExercises.add(CardiacExerciseResponse.of(
                            node.path("name").asText(),
                            node.path("duration").asText(),
                            caloriesRange.minCalories(),
                            caloriesRange.maxCalories(),
                            jsonMappingSupport.readPrecautions(node)
                    ));
                }
            }

            List<StrengthExerciseResponse> strengthExercises = new ArrayList<>();
            JsonNode strengthNode = root.path("strengthExercises");
            if (strengthNode.isArray()) {
                for (JsonNode node : strengthNode) {
                    strengthExercises.add(StrengthExerciseResponse.of(
                            node.path("name").asText(),
                            node.path("duration").asText(),
                            node.path("frequency").asText(),
                            jsonMappingSupport.readPrecautions(node)
                    ));
                }
            }

            return ExerciseRecommendationResponse.of(cardiacExercises, strengthExercises);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("운동 추천 처리 실패: body={}", jsonMappingSupport.abbreviate(geminiResponse), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private String sanitizeRangeNumberFields(String json) {
        Matcher matcher = RANGE_NUMBER_FIELD_PATTERN.matcher(json);
        StringBuffer result = new StringBuffer();
        while (matcher.find()) {
            String replacement = matcher.group(1) + "\"" + matcher.group(2).replace(" ", "") + "\"" + matcher.group(3);
            matcher.appendReplacement(result, Matcher.quoteReplacement(replacement));
        }
        matcher.appendTail(result);
        return result.toString();
    }

    private CaloriesRange parseCaloriesRange(JsonNode node) {
        Integer min = parseNullableInt(node.path("minCalories"));
        Integer max = parseNullableInt(node.path("maxCalories"));

        if (min == null && max == null) {
            String rawCalories = node.path("calories").asText("");
            List<Integer> numbers = extractNumbers(rawCalories);
            if (numbers.size() >= 2) {
                min = numbers.get(0);
                max = numbers.get(1);
            } else if (numbers.size() == 1) {
                min = numbers.get(0);
                max = numbers.get(0);
            }
        }

        if (min == null && max != null) {
            min = max;
        } else if (min != null && max == null) {
            max = min;
        }

        return new CaloriesRange(min, max);
    }

    private Integer parseNullableInt(JsonNode node) {
        if (node == null || node.isMissingNode() || node.isNull()) {
            return null;
        }
        if (node.isInt() || node.isLong()) {
            return node.asInt();
        }
        List<Integer> numbers = extractNumbers(node.asText(""));
        return numbers.isEmpty() ? null : numbers.get(0);
    }

    private List<Integer> extractNumbers(String raw) {
        List<Integer> numbers = new ArrayList<>();
        if (raw == null || raw.isBlank()) {
            return numbers;
        }
        Matcher matcher = Pattern.compile("\\d+").matcher(raw);
        while (matcher.find()) {
            numbers.add(Integer.parseInt(matcher.group()));
        }
        return numbers;
    }

    private record CaloriesRange(Integer minCalories, Integer maxCalories) {
    }
}
