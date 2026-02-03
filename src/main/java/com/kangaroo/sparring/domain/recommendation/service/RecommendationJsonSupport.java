package com.kangaroo.sparring.domain.recommendation.service;

public final class RecommendationJsonSupport {

    private static final int DEFAULT_ABBREVIATE_LENGTH = 500;

    private RecommendationJsonSupport() {
    }

    public static String extractJsonObject(String value) {
        String cleaned = stripMarkdownFence(value);
        int start = cleaned.indexOf('{');
        int end = cleaned.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return cleaned.substring(start, end + 1);
        }
        return cleaned;
    }

    public static String stripMarkdownFence(String value) {
        if (value == null) {
            return "";
        }
        return value.replace("```json", "")
                .replace("```", "")
                .trim();
    }

    public static String abbreviate(String value) {
        return abbreviate(value, DEFAULT_ABBREVIATE_LENGTH);
    }

    public static String abbreviate(String value, int maxLength) {
        if (value == null) {
            return "";
        }
        if (maxLength <= 0) {
            return "";
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength) + "...";
    }
}
