package com.kangaroo.sparring.domain.recommendation.controller;

import com.kangaroo.sparring.domain.recommendation.dto.request.ExerciseRecommendationRequest;
import com.kangaroo.sparring.domain.recommendation.dto.response.ExerciseRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.dto.response.SupplementRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.service.ExerciseRecommendationService;
import com.kangaroo.sparring.domain.recommendation.service.SupplementRecommendationService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "추천", description = "운동 및 영양제 추천 API")
@RestController
@RequestMapping("/api/recommendations")
@RequiredArgsConstructor
public class RecommendationController {

    private final ExerciseRecommendationService exerciseRecommendationService;
    private final SupplementRecommendationService supplementRecommendationService;

    @Operation(summary = "운동 추천 조회", description = "사용자의 건강 정보를 기반으로 맞춤 운동 추천")
    @PostMapping("/exercise")
    public ResponseEntity<ExerciseRecommendationResponse> getExerciseRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ExerciseRecommendationRequest request
    ) {
        Long userId = resolveUserId(principal);
        ExerciseRecommendationResponse response = exerciseRecommendationService
                .getExerciseRecommendations(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "운동 추천 새로고침", description = "캐시를 무시하고 새로운 운동 추천 생성")
    @PostMapping("/exercise/refresh")
    public ResponseEntity<ExerciseRecommendationResponse> refreshExerciseRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody ExerciseRecommendationRequest request
    ) {
        Long userId = resolveUserId(principal);
        ExerciseRecommendationResponse response = exerciseRecommendationService
                .refreshExerciseRecommendations(userId, request);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "영양제 추천 조회", description = "사용자의 건강 정보를 기반으로 맞춤 영양제 추천")
    @PostMapping("/supplement")
    public ResponseEntity<SupplementRecommendationResponse> getSupplementRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = resolveUserId(principal);
        SupplementRecommendationResponse response = supplementRecommendationService
                .getSupplementRecommendations(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "영양제 추천 새로고침", description = "캐시를 무시하고 새로운 영양제 추천 생성")
    @PostMapping("/supplement/refresh")
    public ResponseEntity<SupplementRecommendationResponse> refreshSupplementRecommendations(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = resolveUserId(principal);
        SupplementRecommendationResponse response = supplementRecommendationService
                .refreshSupplementRecommendations(userId);
        return ResponseEntity.ok(response);
    }

    private Long resolveUserId(UserIdPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUserId();
    }
}
