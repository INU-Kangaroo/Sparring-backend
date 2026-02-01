package com.kangaroo.sparring.domain.healthprofile.dto.res;

import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.survey.type.*;
import com.kangaroo.sparring.domain.user.type.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "건강 프로필 응답")
@Getter
@AllArgsConstructor
@Builder
public class HealthProfileResponse {

    @Schema(description = "사용자 ID", example = "1")
    private Long userId;

    // 기본 정보
    @Schema(description = "생년월일", example = "1990-01-01")
    private LocalDate birthDate;

    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Schema(description = "키 (cm)", example = "175.50")
    private BigDecimal height;

    @Schema(description = "몸무게 (kg)", example = "70.50")
    private BigDecimal weight;

    @Schema(description = "BMI", example = "22.86")
    private BigDecimal bmi;

    // 건강 상태
    @Schema(description = "혈당 상태", example = "NORMAL")
    private BloodSugarStatus bloodSugarStatus;

    @Schema(description = "혈압 상태", example = "NORMAL")
    private BloodPressureStatus bloodPressureStatus;

    @Schema(description = "가족력 고혈압 여부", example = "false")
    private Boolean hasFamilyHypertension;

    @Schema(description = "복용 중인 약물", example = "메트포르민 500mg")
    private String medications;

    @Schema(description = "알레르기", example = "페니실린")
    private String allergies;

    @Schema(description = "건강 목표", example = "혈당 관리")
    private String healthGoal;

    // 식습관
    @Schema(description = "식사 빈도", example = "THREE_MEALS")
    private MealFrequency mealFrequency;

    @Schema(description = "음식 선호도 (JSON)", example = "{\"likes\":[\"채소\"],\"dislikes\":[\"매운음식\"]}")
    private String foodPreference;

    @Schema(description = "당 섭취 빈도", example = "RARELY")
    private SugarIntakeFreq sugarIntakeFreq;

    @Schema(description = "카페인 섭취 여부", example = "true")
    private Boolean caffeineIntake;

    // 운동 습관
    @Schema(description = "운동 빈도", example = "THREE_TO_FOUR")
    private ExerciseFrequency exerciseFrequency;

    @Schema(description = "운동 장소 (JSON)", example = "{\"places\":[\"헬스장\",\"집\"]}")
    private String exercisePlace;

    @Schema(description = "운동 종류", example = "웨이트 트레이닝")
    private String exerciseType;

    @Schema(description = "운동 시간", example = "THIRTY_TO_SIXTY")
    private ExerciseDuration exerciseDuration;

    @Schema(description = "평균 걸음 수", example = "8000")
    private Integer avgSteps;

    // 생활 습관
    @Schema(description = "수면 시간", example = "7.5")
    private BigDecimal sleepHours;

    @Schema(description = "수면 질", example = "GOOD")
    private SleepQuality sleepQuality;

    @Schema(description = "흡연 여부", example = "false")
    private Boolean smokingStatus;

    @Schema(description = "음주 빈도", example = "ONCE_OR_TWICE_A_WEEK")
    private DrinkingFrequency drinkingFrequency;

    @Schema(description = "스트레스 수준", example = "MODERATE")
    private StressLevel stressLevel;

    public static HealthProfileResponse from(HealthProfile entity) {
        return HealthProfileResponse.builder()
                .userId(entity.getUserId())
                .birthDate(entity.getBirthDate())
                .gender(entity.getGender())
                .height(entity.getHeight())
                .weight(entity.getWeight())
                .bmi(entity.getBmi())
                .bloodSugarStatus(entity.getBloodSugarStatus())
                .bloodPressureStatus(entity.getBloodPressureStatus())
                .hasFamilyHypertension(entity.getHasFamilyHypertension())
                .medications(entity.getMedications())
                .allergies(entity.getAllergies())
                .healthGoal(entity.getHealthGoal())
                .mealFrequency(entity.getMealFrequency())
                .foodPreference(entity.getFoodPreference())
                .sugarIntakeFreq(entity.getSugarIntakeFreq())
                .caffeineIntake(entity.getCaffeineIntake())
                .exerciseFrequency(entity.getExerciseFrequency())
                .exercisePlace(entity.getExercisePlace())
                .exerciseType(entity.getExerciseType())
                .exerciseDuration(entity.getExerciseDuration())
                .avgSteps(entity.getAvgSteps())
                .sleepHours(entity.getSleepHours())
                .sleepQuality(entity.getSleepQuality())
                .smokingStatus(entity.getSmokingStatus())
                .drinkingFrequency(entity.getDrinkingFrequency())
                .stressLevel(entity.getStressLevel())
                .build();
    }
}