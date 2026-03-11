package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.measurement.entity.BloodPressureLog;
import com.kangaroo.sparring.domain.measurement.entity.BloodSugarLog;
import com.kangaroo.sparring.domain.recommendation.dto.res.SupplementDto;
import com.kangaroo.sparring.domain.recommendation.dto.res.SupplementRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.entity.SupplementRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.Recommendation;
import com.kangaroo.sparring.domain.recommendation.repository.SupplementRecommendationRepository;
import com.kangaroo.sparring.domain.recommendation.repository.RecommendationRepository;
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

@Slf4j
@Service
@RequiredArgsConstructor
public class SupplementRecommendationService {

    private static final int CACHE_HOURS = 168;
    private static final int RECENT_MEASUREMENT_COUNT = 7;

    private final RecommendationRepository recommendationRepository;
    private final SupplementRecommendationRepository supplementRecommendationRepository;
    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;
    private final RecommendationPersistenceService recommendationPersistenceService;
    private final RecommendationPromptTemplateService promptTemplateService;
    private final RecommendationContextService recommendationContextService;
    private final RecommendationJsonMappingSupport jsonMappingSupport;
    private final RecommendationParsingSupport parsingSupport;

    public SupplementRecommendationResponse getSupplementRecommendations(Long userId) {
        User user = recommendationContextService.getUser(userId);
        LocalDateTime cacheThreshold = LocalDateTime.now().minusHours(CACHE_HOURS);

        return recommendationRepository
                .findTopByUserAndTypeAndIsDeletedFalseAndCreatedAtAfterOrderByCreatedAtDesc(
                        user,
                        RecommendationType.SUPPLEMENT,
                        cacheThreshold
                )
                .map(this::buildSupplementRecommendationResponse)
                .orElseGet(() -> generateNewSupplementRecommendations(user));
    }

    public SupplementRecommendationResponse refreshSupplementRecommendations(Long userId) {
        User user = recommendationContextService.getUser(userId);
        return generateNewSupplementRecommendations(user);
    }

    private SupplementRecommendationResponse generateNewSupplementRecommendations(User user) {
        HealthProfile healthProfile = recommendationContextService.getOrCreateHealthProfile(user.getId());
        List<BloodSugarLog> recentBloodSugars =
                recommendationContextService.getRecentBloodSugars(user.getId(), RECENT_MEASUREMENT_COUNT);
        List<BloodPressureLog> recentBloodPressures =
                recommendationContextService.getRecentBloodPressures(user.getId(), RECENT_MEASUREMENT_COUNT);

        String prompt = buildSupplementPrompt(healthProfile, recentBloodSugars, recentBloodPressures);
        String geminiResponse = geminiApiClient.generateContent(prompt);
        SupplementRecommendationResponse response = parseSupplementResponse(geminiResponse);

        Recommendation recommendation = Recommendation.createSupplementRecommendation(user);
        recommendationPersistenceService.persistSupplementRecommendation(
                recommendation,
                buildSupplementEntities(recommendation, response)
        );

        log.info("새로운 영양제 추천 생성 완료: userId={}", user.getId());
        return response;
    }

    private String buildSupplementPrompt(HealthProfile healthProfile,
                                         List<BloodSugarLog> bloodSugars,
                                         List<BloodPressureLog> bloodPressures) {
        String userHealthInfo = RecommendationPromptSupport.buildUserHealthInfo(healthProfile, bloodSugars, bloodPressures);
        return promptTemplateService.renderSupplementPrompt(Map.of(
                "USER_HEALTH_INFO", userHealthInfo
        ));
    }

    private SupplementRecommendationResponse parseSupplementResponse(String geminiResponse) {
        try {
            JsonNode root = parsingSupport.readTreeOrThrow(
                    RecommendationJsonSupport.extractJsonObject(geminiResponse),
                    geminiResponse,
                    "Gemini 영양제 추천 응답"
            );
            List<SupplementDto> supplements = new ArrayList<>();
            JsonNode supplementsNode = resolveSupplementsNode(root);

            if (supplementsNode.isArray()) {
                for (JsonNode node : supplementsNode) {
                    supplements.add(SupplementDto.of(
                            node.path("name").asText(),
                            node.path("dosage").asText(),
                            node.path("frequency").asText(),
                            readBenefits(node),
                            jsonMappingSupport.readPrecautions(node)
                    ));
                }
            }

            return SupplementRecommendationResponse.of(supplements);
        } catch (CustomException e) {
            throw e;
        } catch (Exception e) {
            log.error("영양제 추천 처리 실패: body={}", RecommendationJsonSupport.abbreviate(geminiResponse), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    private List<SupplementRecommendation> buildSupplementEntities(Recommendation recommendation, SupplementRecommendationResponse response) {
        List<SupplementRecommendation> supplements = new ArrayList<>();

        for (SupplementDto dto : response.getSupplements()) {
            supplements.add(SupplementRecommendation.of(
                    recommendation,
                    dto.getName(),
                    dto.getDosage(),
                    dto.getFrequency(),
                    jsonMappingSupport.writeAsJson(dto.getBenefits()),
                    jsonMappingSupport.writeAsJson(dto.getPrecautions())
            ));
        }

        return supplements;
    }

    private SupplementRecommendationResponse buildSupplementRecommendationResponse(Recommendation recommendation) {
        List<SupplementRecommendation> supplements = supplementRecommendationRepository.findByRecommendationOrderByIdAsc(recommendation);

        List<SupplementDto> supplementDtos = supplements.stream()
                .map(n -> SupplementDto.of(
                        n.getName(),
                        n.getDosage(),
                        n.getFrequency(),
                        readBenefitsText(n.getBenefits()),
                        jsonMappingSupport.readJsonArray(n.getPrecautions())
                ))
                .toList();

        return SupplementRecommendationResponse.of(supplementDtos);
    }

    private JsonNode resolveSupplementsNode(JsonNode root) {
        if (root == null || root.isMissingNode() || root.isNull()) {
            return objectMapper.createArrayNode();
        }
        if (root.isArray()) {
            return root;
        }
        JsonNode supplementsNode = root.path("supplements");
        if (supplementsNode.isArray()) {
            return supplementsNode;
        }
        JsonNode nutrientsNode = root.path("nutrients");
        if (nutrientsNode.isArray()) {
            return nutrientsNode;
        }
        return objectMapper.createArrayNode();
    }

    private List<String> readBenefits(JsonNode node) {
        List<String> benefits = readStringListFlexible(node.path("benefits"));
        if (!benefits.isEmpty()) {
            return benefits;
        }
        benefits = readStringListFlexible(node.path("benefit"));
        if (!benefits.isEmpty()) {
            return benefits;
        }
        return List.of();
    }

    private List<String> readBenefitsText(String raw) {
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
            log.warn("효능 문자열 역직렬화 실패: value={}", raw);
            return List.of(trimmed);
        }
    }

    private List<String> readStringListFlexible(JsonNode node) {
        List<String> values = RecommendationArraySupport.readStringArray(node);
        if (!values.isEmpty()) {
            return values;
        }
        if (node != null && node.isTextual()) {
            String text = node.asText("").trim();
            if (text.isEmpty()) {
                return List.of();
            }
            String[] lines = text.split("\\R");
            List<String> lineValues = new ArrayList<>();
            for (String line : lines) {
                String trimmed = line.trim().replaceFirst("^[\\-•\\d.\\)\\s]+", "");
                if (!trimmed.isBlank()) {
                    lineValues.add(trimmed);
                }
            }
            return lineValues.isEmpty() ? List.of(text) : lineValues;
        }
        return List.of();
    }

}
