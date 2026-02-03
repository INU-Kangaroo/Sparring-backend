package com.kangaroo.sparring.domain.recommendation.service;

import com.fasterxml.jackson.databind.JsonNode;

import java.util.ArrayList;
import java.util.List;

public final class RecommendationArraySupport {

    private RecommendationArraySupport() {
    }

    public static List<String> readStringArray(JsonNode node) {
        List<String> values = new ArrayList<>();
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
