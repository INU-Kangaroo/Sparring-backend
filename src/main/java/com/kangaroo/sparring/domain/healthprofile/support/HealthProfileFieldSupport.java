package com.kangaroo.sparring.domain.healthprofile.support;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;

public final class HealthProfileFieldSupport {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    private HealthProfileFieldSupport() {
    }

    public static boolean isSupportedField(String fieldName) {
        if (fieldName == null || fieldName.isBlank()) {
            return false;
        }
        String key = normalizeFieldName(fieldName);
        return switch (key) {
            case "birthDate",
                    "gender",
                    "height",
                    "weight",
                    "bmi",
                    "bloodSugarStatus",
                    "bloodPressureStatus",
                    "hasFamilyHypertension",
                    "medications",
                    "allergies",
                    "healthGoal",
                    "mealFrequency",
                    "foodPreference",
                    "sugarIntakeFreq",
                    "caffeineIntake",
                    "exerciseFrequency",
                    "exercisePlace",
                    "exerciseType",
                    "exerciseDuration",
                    "avgSteps",
                    "sleepHours",
                    "sleepQuality",
                    "smokingStatus",
                    "drinkingFrequency",
                    "stressLevel" -> true;
            default -> false;
        };
    }

    public static String normalizeFieldName(String fieldName) {
        String trimmed = fieldName.trim();
        if (!trimmed.contains("_")) {
            return trimmed;
        }

        String[] parts = trimmed.toLowerCase(Locale.ROOT).split("_");
        if (parts.length == 0) {
            return trimmed;
        }

        StringBuilder builder = new StringBuilder(parts[0]);
        for (int i = 1; i < parts.length; i++) {
            if (parts[i].isEmpty()) {
                continue;
            }
            builder.append(Character.toUpperCase(parts[i].charAt(0)))
                    .append(parts[i].substring(1));
        }
        return builder.toString();
    }

    public static boolean setDate(String rawValue, Consumer<LocalDate> setter) {
        LocalDate value = parseDate(rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    public static boolean setDecimal(String rawValue, Consumer<BigDecimal> setter) {
        BigDecimal value = parseBigDecimal(rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    public static boolean setInteger(String rawValue, Consumer<Integer> setter) {
        Integer value = parseInteger(rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    public static boolean setBoolean(String rawValue, Consumer<Boolean> setter) {
        Boolean value = parseBoolean(rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    public static <E extends Enum<E>> boolean setEnum(Class<E> type, String rawValue, Consumer<E> setter) {
        E value = parseEnum(type, rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    public static <E extends Enum<E>> boolean setJsonCodeArray(
            Class<E> enumType,
            String rawValue,
            Consumer<String> setter
    ) {
        if (rawValue == null || rawValue.isBlank()) {
            return false;
        }
        try {
            List<String> codes = OBJECT_MAPPER.readValue(rawValue, new TypeReference<List<String>>() {});
            if (codes.isEmpty()) {
                return false;
            }
            List<String> normalized = new ArrayList<>();
            for (String code : codes) {
                if (code == null || code.isBlank()) {
                    return false;
                }
                String value = code.trim().toUpperCase(Locale.ROOT);
                try {
                    Enum.valueOf(enumType, value);
                } catch (IllegalArgumentException e) {
                    return false;
                }
                normalized.add(value);
            }
            setter.accept(OBJECT_MAPPER.writeValueAsString(normalized));
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    private static LocalDate parseDate(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(rawValue.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            return null;
        }
    }

    private static BigDecimal parseBigDecimal(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return new BigDecimal(rawValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseInteger(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        try {
            return Integer.parseInt(rawValue.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Boolean parseBoolean(String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim().toLowerCase(Locale.ROOT);
        return switch (normalized) {
            case "true", "1", "yes", "y" -> true;
            case "false", "0", "no", "n" -> false;
            default -> null;
        };
    }

    private static <E extends Enum<E>> E parseEnum(Class<E> type, String rawValue) {
        if (rawValue == null || rawValue.isBlank()) {
            return null;
        }
        String normalized = rawValue.trim().toUpperCase(Locale.ROOT).replace(' ', '_');
        try {
            return Enum.valueOf(type, normalized);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }
}
