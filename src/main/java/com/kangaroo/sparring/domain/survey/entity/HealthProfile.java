package com.kangaroo.sparring.domain.survey.entity;

import com.kangaroo.sparring.domain.common.BaseEntity;
import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.survey.type.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;

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
}