package com.kangaroo.sparring.domain.healthprofile.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.survey.type.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Locale;

@Entity
@Table(name = "health_profile")
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor
@Builder
public class HealthProfile extends BaseEntity {

    @Id
    @Column(name = "user_id")
    private Long userId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "user_id")
    private User user;

    // 기본 정보
    @Column(name = "birth_date")
    private LocalDate birthDate;

    @Enumerated(EnumType.STRING)
    @Column(length = 10)
    private Gender gender;

    @Column(precision = 5, scale = 2)
    private BigDecimal height;

    @Column(precision = 5, scale = 2)
    private BigDecimal weight;

    @Column(precision = 4, scale = 2)
    private BigDecimal bmi;

    // 건강 상태
    @Enumerated(EnumType.STRING)
    @Column(name = "blood_sugar_status", length = 20)
    private BloodSugarStatus bloodSugarStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_pressure_status", length = 20)
    private BloodPressureStatus bloodPressureStatus;

    @Column(name = "has_family_hypertension")
    private Boolean hasFamilyHypertension;

    @Column(columnDefinition = "TEXT")
    private String medications;

    @Column(columnDefinition = "TEXT")
    private String allergies;

    @Column(name = "health_goal", length = 100)
    private String healthGoal;

    // 식습관
    @Enumerated(EnumType.STRING)
    @Column(name = "meal_frequency", length = 20)
    private MealFrequency mealFrequency;

    @Column(name = "food_preference", columnDefinition = "JSON")
    private String foodPreference;

    @Enumerated(EnumType.STRING)
    @Column(name = "sugar_intake_freq", length = 30)
    private SugarIntakeFreq sugarIntakeFreq;

    @Column(name = "caffeine_intake")
    private Boolean caffeineIntake;

    // 운동 습관
    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_frequency", length = 20)
    private ExerciseFrequency exerciseFrequency;

    @Column(name = "exercise_place", columnDefinition = "JSON")
    private String exercisePlace;

    @Column(name = "exercise_type", length = 255)
    private String exerciseType;

    @Enumerated(EnumType.STRING)
    @Column(name = "exercise_duration", length = 20)
    private ExerciseDuration exerciseDuration;

    @Column(name = "avg_steps")
    private Integer avgSteps;

    // 생활 습관
    @Column(name = "sleep_hours", precision = 4, scale = 1)
    private BigDecimal sleepHours;

    @Enumerated(EnumType.STRING)
    @Column(name = "sleep_quality", length = 20)
    private SleepQuality sleepQuality;

    @Column(name = "smoking_status")
    private Boolean smokingStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "drinking_frequency", length = 30)
    private DrinkingFrequency drinkingFrequency;

    @Enumerated(EnumType.STRING)
    @Column(name = "stress_level", length = 20)
    private StressLevel stressLevel;

    // BMI 계산 메서드
    public void calculateBmi() {
        if (height != null && weight != null && height.compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal heightInMeters = height.divide(new BigDecimal("100"), 4, RoundingMode.HALF_UP);
            this.bmi = weight.divide(heightInMeters.multiply(heightInMeters), 2, RoundingMode.HALF_UP);
        }
    }

    // 비즈니스 메서드 - 기본 설문 업데이트
    public void updateFromBasicSurvey(
            LocalDate birthDate, Gender gender, BigDecimal height, BigDecimal weight,
            BloodSugarStatus bloodSugarStatus, BloodPressureStatus bloodPressureStatus,
            String medications, String allergies, String healthGoal, Boolean hasFamilyHypertension
    ) {
        this.birthDate = birthDate;
        this.gender = gender;
        this.height = height;
        this.weight = weight;
        this.bloodSugarStatus = bloodSugarStatus;
        this.bloodPressureStatus = bloodPressureStatus;
        this.medications = medications;
        this.allergies = allergies;
        this.healthGoal = healthGoal;
        this.hasFamilyHypertension = hasFamilyHypertension;
        calculateBmi();
    }

    // 비즈니스 메서드 - 상세 설문 업데이트
    public void updateFromDetailedSurvey(
            MealFrequency mealFrequency, String foodPreference, SugarIntakeFreq sugarIntakeFreq,
            Boolean caffeineIntake, ExerciseFrequency exerciseFrequency, String exercisePlace,
            String exerciseType, ExerciseDuration exerciseDuration, Integer avgSteps,
            BigDecimal sleepHours, SleepQuality sleepQuality, Boolean smokingStatus,
            DrinkingFrequency drinkingFrequency, StressLevel stressLevel
    ) {
        this.mealFrequency = mealFrequency;
        this.foodPreference = foodPreference;
        this.sugarIntakeFreq = sugarIntakeFreq;
        this.caffeineIntake = caffeineIntake;
        this.exerciseFrequency = exerciseFrequency;
        this.exercisePlace = exercisePlace;
        this.exerciseType = exerciseType;
        this.exerciseDuration = exerciseDuration;
        this.avgSteps = avgSteps;
        this.sleepHours = sleepHours;
        this.sleepQuality = sleepQuality;
        this.smokingStatus = smokingStatus;
        this.drinkingFrequency = drinkingFrequency;
        this.stressLevel = stressLevel;
    }

    // 비즈니스 메서드 - 사용자 직접 업데이트 (부분 업데이트)
    public void updateProfile(
            LocalDate birthDate, Gender gender, BigDecimal height, BigDecimal weight,
            BloodSugarStatus bloodSugarStatus, BloodPressureStatus bloodPressureStatus,
            Boolean hasFamilyHypertension, String medications, String allergies, String healthGoal
    ) {
        if (birthDate != null) this.birthDate = birthDate;
        if (gender != null) this.gender = gender;
        if (height != null) this.height = height;
        if (weight != null) this.weight = weight;
        if (bloodSugarStatus != null) this.bloodSugarStatus = bloodSugarStatus;
        if (bloodPressureStatus != null) this.bloodPressureStatus = bloodPressureStatus;
        if (hasFamilyHypertension != null) this.hasFamilyHypertension = hasFamilyHypertension;
        if (medications != null) this.medications = medications;
        if (allergies != null) this.allergies = allergies;
        if (healthGoal != null) this.healthGoal = healthGoal;

        // 키 또는 몸무게가 변경되면 BMI 재계산
        if (height != null || weight != null) {
            calculateBmi();
        }
    }

    public boolean applySurveyField(String fieldName, String rawValue) {
        if (fieldName == null || fieldName.isBlank()) {
            return true;
        }

        String key = normalizeFieldName(fieldName);
        boolean applied = true;
        switch (key) {
            case "birthDate" -> applied = setDate(rawValue, value -> this.birthDate = value);
            case "gender" -> applied = setEnum(Gender.class, rawValue, value -> this.gender = value);
            case "height" -> applied = setDecimal(rawValue, value -> this.height = value);
            case "weight" -> applied = setDecimal(rawValue, value -> this.weight = value);
            case "bmi" -> applied = setDecimal(rawValue, value -> this.bmi = value);
            case "bloodSugarStatus" -> applied = setEnum(BloodSugarStatus.class, rawValue, value -> this.bloodSugarStatus = value);
            case "bloodPressureStatus" -> applied = setEnum(BloodPressureStatus.class, rawValue, value -> this.bloodPressureStatus = value);
            case "hasFamilyHypertension" -> applied = setBoolean(rawValue, value -> this.hasFamilyHypertension = value);
            case "medications" -> this.medications = rawValue;
            case "allergies" -> this.allergies = rawValue;
            case "healthGoal" -> this.healthGoal = rawValue;
            case "mealFrequency" -> applied = setEnum(MealFrequency.class, rawValue, value -> this.mealFrequency = value);
            case "foodPreference" -> this.foodPreference = rawValue;
            case "sugarIntakeFreq" -> applied = setEnum(SugarIntakeFreq.class, rawValue, value -> this.sugarIntakeFreq = value);
            case "caffeineIntake" -> applied = setBoolean(rawValue, value -> this.caffeineIntake = value);
            case "exerciseFrequency" -> applied = setEnum(ExerciseFrequency.class, rawValue, value -> this.exerciseFrequency = value);
            case "exercisePlace" -> this.exercisePlace = rawValue;
            case "exerciseType" -> this.exerciseType = rawValue;
            case "exerciseDuration" -> applied = setEnum(ExerciseDuration.class, rawValue, value -> this.exerciseDuration = value);
            case "avgSteps" -> applied = setInteger(rawValue, value -> this.avgSteps = value);
            case "sleepHours" -> applied = setDecimal(rawValue, value -> this.sleepHours = value);
            case "sleepQuality" -> applied = setEnum(SleepQuality.class, rawValue, value -> this.sleepQuality = value);
            case "smokingStatus" -> applied = setBoolean(rawValue, value -> this.smokingStatus = value);
            case "drinkingFrequency" -> applied = setEnum(DrinkingFrequency.class, rawValue, value -> this.drinkingFrequency = value);
            case "stressLevel" -> applied = setEnum(StressLevel.class, rawValue, value -> this.stressLevel = value);
            default -> applied = false;
        }

        if (applied && ("height".equals(key) || "weight".equals(key))) {
            calculateBmi();
        }
        return applied;
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

    private static String normalizeFieldName(String fieldName) {
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

    private static boolean setDate(String rawValue, java.util.function.Consumer<LocalDate> setter) {
        LocalDate value = parseDate(rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private static boolean setDecimal(String rawValue, java.util.function.Consumer<BigDecimal> setter) {
        BigDecimal value = parseBigDecimal(rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private static boolean setInteger(String rawValue, java.util.function.Consumer<Integer> setter) {
        Integer value = parseInteger(rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private static boolean setBoolean(String rawValue, java.util.function.Consumer<Boolean> setter) {
        Boolean value = parseBoolean(rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }

    private static <E extends Enum<E>> boolean setEnum(Class<E> type, String rawValue, java.util.function.Consumer<E> setter) {
        E value = parseEnum(type, rawValue);
        if (value == null) {
            return false;
        }
        setter.accept(value);
        return true;
    }
}
