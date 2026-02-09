package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.recommendation.dto.req.ExerciseRecommendationRequest;
import com.kangaroo.sparring.domain.recommendation.dto.res.CardiacExerciseDto;
import com.kangaroo.sparring.domain.recommendation.dto.res.ExerciseRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.StrengthExerciseDto;
import com.kangaroo.sparring.domain.recommendation.entity.ExerciseRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import com.kangaroo.sparring.domain.recommendation.repository.ExerciseRecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.repository.RecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.type.ExerciseType;
import com.kangaroo.sparring.domain.recommendation.type.RecommendationType;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class ExerciseRecommendationService {
    private static final Pattern RANGE_NUMBER_FIELD_PATTERN =
            Pattern.compile("(\"[^\"]+\"\\s*:\\s*)(\\d+\\s*-\\s*\\d+)(\\s*[,}])");

    private static final int CACHE_HOURS = 24;
    private static final int RECENT_MEASUREMENT_COUNT = 7;

    private final RecommendationRepository recommendationRepository;
    private final ExerciseRecommendationRepository exerciseRecommendationRepository;
    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;
    private final RecommendationPersistenceService recommendationPersistenceService;
    private final RecommendationPromptTemplateService promptTemplateService;
    private final RecommendationContextService recommendationContextService;
    private final RecommendationJsonMappingSupport jsonMappingSupport;
    private final RecommendationParsingSupport parsingSupport;

    public ExerciseRecommendationResponse getExerciseRecommendations(Long userId, ExerciseRecommendationRequest request) {
        User user = recommendationContextService.getUser(userId);
        LocalDateTime cacheThreshold = LocalDateTime.now().minusHours(CACHE_HOURS);

        return recommendationRepository
                .findTopByUserAndTypeAndFilterDurationAndFilterIntensityAndFilterLocationAndIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                        user,
                        RecommendationType.EXERCISE,
                        request.getDuration().name(),
                        request.getIntensity().name(),
                        request.getLocation().name(),
                        cacheThreshold
                )
                .map(this::buildExerciseRecommendationResponse)
                .orElseGet(() -> generateNewExerciseRecommendations(user, request));
    }

    public ExerciseRecommendationResponse refreshExerciseRecommendations(Long userId, ExerciseRecommendationRequest request) {
        User user = recommendationContextService.getUser(userId);
        return generateNewExerciseRecommendations(user, request);
    }

    private ExerciseRecommendationResponse generateNewExerciseRecommendations(User user, ExerciseRecommendationRequest request) {
        HealthProfile healthProfile = recommendationContextService.getOrCreateHealthProfile(user.getId());
        List<BloodSugarLog> recentBloodSugars =
                recommendationContextService.getRecentBloodSugars(user.getId(), RECENT_MEASUREMENT_COUNT);
        List<BloodPressureLog> recentBloodPressures =
                recommendationContextService.getRecentBloodPressures(user.getId(), RECENT_MEASUREMENT_COUNT);

        String prompt = buildExercisePrompt(healthProfile, recentBloodSugars, recentBloodPressures, request);
        String geminiResponse = geminiApiClient.generateContent(prompt);
        ExerciseRecommendationResponse response = parseExerciseResponse(geminiResponse);

        Recommendation recommendation = Recommendation.createExerciseRecommendation(
                user,
                request.getDuration().name(),
                request.getIntensity().name(),
                request.getLocation().name()
        );
        recommendationPersistenceService.persistExerciseRecommendation(
                recommendation,
                buildExerciseEntities(recommendation, response)
        );

        log.info("새로운 운동 추천 생성 완료: userId={}", user.getId());
        return response;
    }

    private String buildExercisePrompt(HealthProfile healthProfile,
                                       List<BloodSugarLog> bloodSugars,
                                       List<BloodPressureLog> bloodPressures,
                                       ExerciseRecommendationRequest request) {
        String userHealthInfo = RecommendationPromptSupport.buildUserHealthInfo(healthProfile, bloodSugars, bloodPressures);
        return promptTemplateService.renderExercisePrompt(Map.of(
                "USER_HEALTH_INFO", userHealthInfo,
                "DURATION", request.getDuration().getDescription(),
                "INTENSITY", request.getIntensity().getDescription(),
                "LOCATION", request.getLocation().getDescription()
        ));
    }

    private ExerciseRecommendationResponse parseExerciseResponse(String geminiResponse) {
        try {
            String sanitizedJson = sanitizeRangeNumberFields(RecommendationJsonSupport.extractJsonObject(geminiResponse));
            JsonNode root = parsingSupport.readTreeOrThrow(sanitizedJson, geminiResponse, "Gemini 운동 추천 응답");

            List<CardiacExerciseDto> cardiacExercises = new ArrayList<>();
            JsonNode cardiacNode = root.path("cardiacExercises");
            if (cardiacNode.isArray()) {
                for (JsonNode node : cardiacNode) {
                    CaloriesRange caloriesRange = parseCaloriesRange(node);
                    cardiacExercises.add(CardiacExerciseDto.of(
                            node.path("name").asText(),
                            node.path("duration").asText(),
                            caloriesRange.minCalories(),
                            caloriesRange.maxCalories(),
                            jsonMappingSupport.readPrecautions(node)
                    ));
                }
            }

            List<StrengthExerciseDto> strengthExercises = new ArrayList<>();
            JsonNode strengthNode = root.path("strengthExercises");
            if (strengthNode.isArray()) {
                for (JsonNode node : strengthNode) {
                    strengthExercises.add(StrengthExerciseDto.of(
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
            log.error("운동 추천 처리 실패: body={}", RecommendationJsonSupport.abbreviate(geminiResponse), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private List<ExerciseRecommendation> buildExerciseEntities(Recommendation recommendation, ExerciseRecommendationResponse response) {
        List<ExerciseRecommendation> exercises = new ArrayList<>();

        for (CardiacExerciseDto dto : response.getCardiacExercises()) {
            exercises.add(ExerciseRecommendation.of(
                    recommendation,
                    ExerciseType.CARDIAC,
                    dto.getName(),
                    dto.getDuration(),
                    dto.getMinCalories(),
                    dto.getMaxCalories(),
                    null,
                    jsonMappingSupport.writeAsJson(dto.getPrecautions())
            ));
        }

        for (StrengthExerciseDto dto : response.getStrengthExercises()) {
            exercises.add(ExerciseRecommendation.of(
                    recommendation,
                    ExerciseType.STRENGTH,
                    dto.getName(),
                    dto.getDuration(),
                    null,
                    null,
                    dto.getFrequency(),
                    jsonMappingSupport.writeAsJson(dto.getPrecautions())
            ));
        }

        return exercises;
    }

    private ExerciseRecommendationResponse buildExerciseRecommendationResponse(Recommendation recommendation) {
        List<ExerciseRecommendation> exercises = exerciseRecommendationRepository.findByRecommendationOrderByIdAsc(recommendation);

        List<CardiacExerciseDto> cardiacExercises = exercises.stream()
                .filter(e -> e.getExerciseType() == ExerciseType.CARDIAC)
                .map(e -> CardiacExerciseDto.of(
                        e.getName(),
                        e.getDuration(),
                        e.getMinCalories(),
                        e.getMaxCalories(),
                        jsonMappingSupport.readJsonArray(e.getPrecautions())
                ))
                .toList();

        List<StrengthExerciseDto> strengthExercises = exercises.stream()
                .filter(e -> e.getExerciseType() == ExerciseType.STRENGTH)
                .map(e -> StrengthExerciseDto.of(
                        e.getName(),
                        e.getDuration(),
                        e.getFrequency(),
                        jsonMappingSupport.readJsonArray(e.getPrecautions())
                ))
                .toList();

        return ExerciseRecommendationResponse.of(cardiacExercises, strengthExercises);
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
