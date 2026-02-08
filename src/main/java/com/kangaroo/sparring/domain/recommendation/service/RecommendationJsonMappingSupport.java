package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class RecommendationJsonMappingSupport {

    private final ObjectMapper objectMapper;

    public List<String> readPrecautions(JsonNode node) {
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

    public String writeAsJson(List<String> values) {
        try {
            return objectMapper.writeValueAsString(values == null ? List.of() : values);
        } catch (JsonProcessingException e) {
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public List<String> readJsonArray(String raw) {
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
            log.warn("문자열 배열 역직렬화 실패: value={}", raw);
            return List.of();
        }
    }
}
