package com.kangaroo.sparring.domain.user.dto.req;

import com.kangaroo.sparring.domain.survey.type.BloodPressureStatus;
import com.kangaroo.sparring.domain.survey.type.BloodSugarStatus;
import com.kangaroo.sparring.domain.user.type.Gender;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDate;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "소셜 회원가입 완료 요청 (기본 프로필 입력)")
public class SocialSignupCompleteRequest {

    @Schema(description = "생년월일 (YYYY-MM-DD)", example = "1990-01-01", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "생년월일은 필수입니다")
    @Past(message = "생년월일은 과거 날짜여야 합니다")
    private LocalDate birthDate;

    @Schema(description = "성별", example = "MALE", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "성별은 필수입니다")
    private Gender gender;

    @Schema(description = "키 (cm)", example = "175.5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "키는 필수입니다")
    @DecimalMin(value = "100.0", message = "키는 100cm 이상이어야 합니다")
    @DecimalMax(value = "250.0", message = "키는 250cm 이하여야 합니다")
    @Digits(integer = 3, fraction = 2, message = "키는 소수점 둘째 자리까지 입력 가능합니다")
    private BigDecimal height;

    @Schema(description = "몸무게 (kg)", example = "70.5", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "몸무게는 필수입니다")
    @DecimalMin(value = "30.0", message = "몸무게는 30kg 이상이어야 합니다")
    @DecimalMax(value = "300.0", message = "몸무게는 300kg 이하여야 합니다")
    @Digits(integer = 3, fraction = 2, message = "몸무게는 소수점 둘째 자리까지 입력 가능합니다")
    private BigDecimal weight;

    @Schema(description = "혈당 상태", example = "NORMAL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "혈당 상태는 필수입니다")
    private BloodSugarStatus bloodSugarStatus;

    @Schema(description = "혈압 상태", example = "NORMAL", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "혈압 상태는 필수입니다")
    private BloodPressureStatus bloodPressureStatus;

    @Schema(description = "복용 중인 약물", example = "메트포르민 500mg", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "복용 중인 약물은 필수입니다")
    @Size(max = 500, message = "복용 약물은 500자 이하로 입력해주세요")
    private String medications;

    @Schema(description = "알레르기 음식", example = "땅콩", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "알레르기 음식은 필수입니다")
    @Size(max = 500, message = "알레르기는 500자 이하로 입력해주세요")
    private String allergies;

    @Schema(description = "주요 건강 목표", example = "혈당 관리", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotBlank(message = "주요 건강 목표는 필수입니다")
    @Size(max = 100, message = "건강 목표는 100자 이하로 입력해주세요")
    private String healthGoal;

    @Schema(description = "가족력 중 고혈압 여부", example = "false", requiredMode = Schema.RequiredMode.REQUIRED)
    @NotNull(message = "가족력 중 고혈압 여부는 필수입니다")
    private Boolean hasFamilyHypertension;
}
