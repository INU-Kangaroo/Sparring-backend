package com.kangaroo.sparring.domain.recommendation.controller;

import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.healthprofile.service.HealthProfileGuardService;
import com.kangaroo.sparring.domain.healthprofile.service.HealthProfileService;
import com.kangaroo.sparring.domain.recommendation.dto.req.ExerciseRecommendationRequest;
import com.kangaroo.sparring.domain.recommendation.dto.res.ExerciseRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.MealRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.dto.res.SupplementRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.service.ExerciseRecommendationService;
import com.kangaroo.sparring.domain.recommendation.service.MealRecommendationService;
import com.kangaroo.sparring.domain.recommendation.service.SupplementRecommendationService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "추천", description = "운동/영양제/식단 추천 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final ExerciseRecommendationService exerciseRecommendationService;
    private final SupplementRecommendationService supplementRecommendationService;
    private final MealRecommendationService mealRecommendationService;
    private final HealthProfileGuardService healthProfileGuardService;
    private final HealthProfileService healthProfileService;
    private final UserRepository userRepository;

    @Operation(summary = "운동 추천 조회", description = "사용자 건강 정보를 기반으로 맞춤 운동 추천")
    @PostMapping("/exercise")
    public ResponseEntity<ExerciseRecommendationResponse> getExerciseRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ExerciseRecommendationRequest request
    ) {
        Long userId = resolveGuardedUserId(principal);
        return ResponseEntity.ok(exerciseRecommendationService.getExerciseRecommendations(userId, request));
    }

    @Operation(summary = "운동 추천 새로고침", description = "캐시를 무시하고 새로운 운동 추천 생성")
    @PostMapping("/exercise/refresh")
    public ResponseEntity<ExerciseRecommendationResponse> refreshExerciseRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ExerciseRecommendationRequest request
    ) {
        Long userId = resolveGuardedUserId(principal);
        return ResponseEntity.ok(exerciseRecommendationService.refreshExerciseRecommendations(userId, request));
    }

    @Operation(summary = "영양제 추천 조회", description = "사용자 건강 정보를 기반으로 맞춤 영양제 추천")
    @PostMapping("/supplement")
    public ResponseEntity<SupplementRecommendationResponse> getSupplementRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = resolveGuardedUserId(principal);
        return ResponseEntity.ok(supplementRecommendationService.getSupplementRecommendations(userId));
    }

    @Operation(summary = "영양제 추천 새로고침", description = "캐시를 무시하고 새로운 영양제 추천 생성")
    @PostMapping("/supplement/refresh")
    public ResponseEntity<SupplementRecommendationResponse> refreshSupplementRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = resolveGuardedUserId(principal);
        return ResponseEntity.ok(supplementRecommendationService.refreshSupplementRecommendations(userId));
    }

    @Operation(summary = "식단 추천 조회", description = "캐시된 추천 반환. 없으면 FastAPI 호출 후 저장")
    @PostMapping("/food")
    public ResponseEntity<MealRecommendationResponse> getMealRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @RequestParam String mealType
    ) {
        MealRequestContext context = resolveMealRequestContext(principal, mealType);
        return ResponseEntity.ok(mealRecommendationService.recommend(context.user(), context.profile(), context.mealTime()));
    }

    @Operation(summary = "식단 추천 새로고침", description = "캐시 무시하고 FastAPI 재호출 후 저장")
    @PostMapping("/food/refresh")
    public ResponseEntity<MealRecommendationResponse> refreshMealRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @RequestParam String mealType
    ) {
        MealRequestContext context = resolveMealRequestContext(principal, mealType);
        return ResponseEntity.ok(mealRecommendationService.refresh(context.user(), context.profile(), context.mealTime(), true));
    }

    private Long resolveGuardedUserId(UserIdPrincipal principal) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        healthProfileGuardService.ensureProfileComplete(userId);
        return userId;
    }

    private MealRequestContext resolveMealRequestContext(UserIdPrincipal principal, String mealType) {
        Long userId = resolveGuardedUserId(principal);
        MealTime resolvedMealType = resolveMealTypeParam(mealType);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));
        HealthProfile profile = healthProfileService.getOrCreateHealthProfile(userId);
        return new MealRequestContext(user, profile, resolvedMealType);
    }

    private MealTime resolveMealTypeParam(String mealType) {
        try {
            return MealTime.from(mealType);
        } catch (IllegalArgumentException e) {
            throw new CustomException(ErrorCode.INVALID_INPUT, "mealType은 breakfast/lunch/dinner/snack 중 하나여야 합니다.");
        }
    }

    private record MealRequestContext(User user, HealthProfile profile, MealTime mealTime) {
    }
}
