package com.kangaroo.sparring.domain.healthprofile.dto.req;

import com.kangaroo.sparring.domain.survey.type.BloodPressureStatus;
import com.kangaroo.sparring.domain.survey.type.BloodSugarStatus;
import com.kangaroo.sparring.domain.user.type.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Schema(description = "건강 프로필 업데이트 요청")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UpdateHealthProfileRequest {

    @Schema(description = "생년월일 (YYYY-MM-DD)", example = "1990-01-01")
    @Past(message = "생년월일은 과거 날짜여야 합니다")
    private LocalDate birthDate;

    @Schema(description = "성별", example = "MALE")
    private Gender gender;

    @Schema(description = "키 (cm)", example = "175.5")
    @DecimalMin(value = "100.0", message = "키는 100cm 이상이어야 합니다")
    @DecimalMax(value = "250.0", message = "키는 250cm 이하여야 합니다")
    @Digits(integer = 3, fraction = 2, message = "키는 소수점 둘째 자리까지 입력 가능합니다")
    private BigDecimal height;

    @Schema(description = "몸무게 (kg)", example = "70.5")
    @DecimalMin(value = "30.0", message = "몸무게는 30kg 이상이어야 합니다")
    @DecimalMax(value = "300.0", message = "몸무게는 300kg 이하여야 합니다")
    @Digits(integer = 3, fraction = 2, message = "몸무게는 소수점 둘째 자리까지 입력 가능합니다")
    private BigDecimal weight;

    @Schema(description = "혈당 상태", example = "NORMAL")
    private BloodSugarStatus bloodSugarStatus;

    @Schema(description = "혈압 상태", example = "NORMAL")
    private BloodPressureStatus bloodPressureStatus;

    @Schema(description = "가족력 고혈압 여부", example = "false")
    private Boolean hasFamilyHypertension;

    @Schema(description = "복용 중인 약물", example = "메트포르민 500mg")
    @Size(max = 500, message = "복용 약물은 500자 이하로 입력해주세요")
    private String medications;

    @Schema(description = "알레르기", example = "페니실린")
    @Size(max = 500, message = "알레르기는 500자 이하로 입력해주세요")
    private String allergies;

    @Schema(description = "건강 목표", example = "혈당 관리")
    @Size(max = 100, message = "건강 목표는 100자 이하로 입력해주세요")
    private String healthGoal;
}