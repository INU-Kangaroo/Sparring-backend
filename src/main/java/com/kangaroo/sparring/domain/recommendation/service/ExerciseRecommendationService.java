package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.repository.HealthProfileRepository;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.measurement.repository.BloodPressureLogRepository;
import com.kangaroo.sparring.domain.measurement.repository.BloodSugarLogRepository;
import com.kangaroo.sparring.domain.recommendation.dto.request.ExerciseRecommendationRequest;
import com.kangaroo.sparring.domain.recommendation.dto.response.CardiacExerciseDto;
import com.kangaroo.sparring.domain.recommendation.dto.response.ExerciseRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.dto.response.StrengthExerciseDto;
import com.kangaroo.sparring.domain.recommendation.entity.ExerciseRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import com.kangaroo.sparring.domain.recommendation.repository.ExerciseRecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.repository.RecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.type.ExerciseType;
import com.kangaroo.sparring.domain.recommendation.type.RecommendationType;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
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
    private final UserRepository userRepository;
    private final HealthProfileRepository healthProfileRepository;
    private final BloodSugarLogRepository bloodSugarLogRepository;
    private final BloodPressureLogRepository bloodPressureLogRepository;
    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;
    private final RecommendationPersistenceService recommendationPersistenceService;
    private final RecommendationPromptTemplateService promptTemplateService;

    public ExerciseRecommendationResponse getExerciseRecommendations(Long userId, ExerciseRecommendationRequest request) {
        User user = findUserById(userId);
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
        User user = findUserById(userId);
        return generateNewExerciseRecommendations(user, request);
    }

    private ExerciseRecommendationResponse generateNewExerciseRecommendations(User user, ExerciseRecommendationRequest request) {
        HealthProfile healthProfile = findHealthProfileByUserId(user.getId());
        List<BloodSugarLog> recentBloodSugars = bloodSugarLogRepository.findRecentByUserId(
                user.getId(),
                PageRequest.of(0, RECENT_MEASUREMENT_COUNT)
        );
        List<BloodPressureLog> recentBloodPressures = bloodPressureLogRepository.findRecentByUserId(
                user.getId(),
                PageRequest.of(0, RECENT_MEASUREMENT_COUNT)
        );

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
            JsonNode root = objectMapper.readTree(sanitizedJson);

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
                            readPrecautions(node)
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
                            readPrecautions(node)
                    ));
                }
            }

            return ExerciseRecommendationResponse.of(cardiacExercises, strengthExercises);
        } catch (Exception e) {
            log.error("Gemini 응답 파싱 실패: body={}", RecommendationJsonSupport.abbreviate(geminiResponse), e);
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
                    writeAsJson(dto.getPrecautions())
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
                    writeAsJson(dto.getPrecautions())
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
                        readJsonArray(e.getPrecautions())
                ))
                .toList();

        List<StrengthExerciseDto> strengthExercises = exercises.stream()
                .filter(e -> e.getExerciseType() == ExerciseType.STRENGTH)
                .map(e -> StrengthExerciseDto.of(
                        e.getName(),
                        e.getDuration(),
                        e.getFrequency(),
                        readJsonArray(e.getPrecautions())
                ))
                .toList();

        return ExerciseRecommendationResponse.of(cardiacExercises, strengthExercises);
    }

    private User findUserById(Long userId) {
        return userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
    }

    private HealthProfile findHealthProfileByUserId(Long userId) {
        return healthProfileRepository.findByUserId(userId)
                .orElseGet(() -> {
                    User user = findUserById(userId);
                    HealthProfile healthProfile = HealthProfile.builder()
                            .user(user)
                            .build();
                    return healthProfileRepository.save(healthProfile);
                });
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

    private List<String> readPrecautions(JsonNode node) {
        List<String> precautions = RecommendationArraySupport.readStringArray(node.path("precautions"));
        if (!precautions.isEmpty()) {
            return precautions;
        }
        String single = node.path("precaution").asText("");
        if (!single.isBlank()) {
            return List.of(single);
        }
        return List.of();
    }

    private String writeAsJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private List<String> readJsonArray(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        String trimmed = raw.trim();
        if (!trimmed.startsWith("[")) {
            return List.of(trimmed);
        }
        try {
            JsonNode node = objectMapper.readTree(trimmed);
            return RecommendationArraySupport.readStringArray(node);
        } catch (Exception e) {
            log.warn("운동 주의사항 역직렬화 실패: value={}", raw);
            return List.of();
        }
    }

    private record CaloriesRange(Integer minCalories, Integer maxCalories) {
    }
}
