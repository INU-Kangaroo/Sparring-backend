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

    private static final int DEFAULT_ABBREVIATE_LENGTH = 500;

    private final ObjectMapper objectMapper;

    public String extractJsonObject(String value) {
        String cleaned = stripMarkdownFence(value);
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    public String stripMarkdownFence(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("```json", "")
                .replace("```", "")
                .trim();
    }

    public String abbreviate(String value) {
        return abbreviate(value, DEFAULT_ABBREVIATE_LENGTH);
    }

    public String abbreviate(String value, int maxLength) {
        if (value == null || maxLength <= 0) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }

    public JsonNode readTreeOrThrow(String json, String rawResponse, String context) {
        try {
            return objectMapper.readTree(json);
        } catch (Exception e) {
            log.error("{} 파싱 실패: body={}", context, abbreviate(rawResponse), e);
            throw new CustomException(ErrorCode.INTERNAL_SERVER_ERROR);
        }
    }

    public List<String> readPrecautions(JsonNode node) {
        List<String> precautions = readStringListFlexible(node.path("precautions"));
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
            return readStringArray(node);
        } catch (Exception e) {
            log.warn("문자열 배열 역직렬화 실패: value={}", raw);
            return List.of(trimmed);
        }
    }

    public List<String> readStringListFlexible(JsonNode node) {
        List<String> values = readStringArray(node);
        if (!values.isEmpty()) {
            return values;
        }
        if (node == null || !node.isTextual()) {
            return List.of();
        }

        String text = node.asText("").trim();
        if (text.isEmpty()) {
            return List.of();
        }

        String[] lines = text.split("\\R");
        List<String> lineValues = new java.util.ArrayList<>();
        for (String line : lines) {
            String trimmed = line.trim().replaceFirst("^[\\-•\\d.\\)\\s]+", "");
            if (!trimmed.isBlank()) {
                lineValues.add(trimmed);
            }
        }
        return lineValues.isEmpty() ? List.of(text) : lineValues;
    }

    private List<String> readStringArray(JsonNode node) {
        List<String> values = new java.util.ArrayList<>();
        if (node != null && node.isArray()) {
            for (JsonNode value : node) {
                String text = value.asText("");
                if (!text.isBlank()) {
                    values.add(text.trim());
                }
            }
        }
        return values;
    }
}
