package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.domain.recommendation.dto.res.SupplementResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.SupplementRecommendationResponse;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.client.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class SupplementRecommendationAiClient {

    private final GeminiApiClient geminiApiClient;
    private final ObjectMapper objectMapper;
    private final RecommendationJsonMappingSupport jsonMappingSupport;

    public SupplementRecommendationResponse recommend(String prompt) {
        String geminiResponse = geminiApiClient.generateContent(prompt);
        return parseSupplementResponse(geminiResponse);
    }

    private SupplementRecommendationResponse parseSupplementResponse(String geminiResponse) {
        try {
            JsonNode root = jsonMappingSupport.readTreeOrThrow(
                    jsonMappingSupport.extractJsonObject(geminiResponse),
                    geminiResponse,
                    "Gemini 영양제 추천 응답"
            );
            List<SupplementResponse> supplements = new ArrayList<>();
            JsonNode supplementsNode = resolveSupplementsNode(root);

            if (supplementsNode.isArray()) {
                for (JsonNode node : supplementsNode) {
                    supplements.add(SupplementResponse.of(
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
            log.error("영양제 추천 처리 실패: body={}", jsonMappingSupport.abbreviate(geminiResponse), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
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
        List<String> benefits = jsonMappingSupport.readStringListFlexible(node.path("benefits"));
        if (!benefits.isEmpty()) {
            return benefits;
        }
        benefits = jsonMappingSupport.readStringListFlexible(node.path("benefit"));
        if (!benefits.isEmpty()) {
            return benefits;
        }
        return List.of();
    }
}
