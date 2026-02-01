package com.kangaroo.sparring.domain.healthprofile.controller;

import com.kangaroo.sparring.domain.healthprofile.dto.req.UpdateHealthProfileRequest;
import com.kangaroo.sparring.domain.healthprofile.dto.res.HealthProfileResponse;
import com.kangaroo.sparring.domain.healthprofile.service.HealthProfileService;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Health Profile", description = "건강 프로필 API")
@RestController
@RequestMapping("/api/health-profile")
@RequiredArgsConstructor
public class HealthProfileController {

    private final HealthProfileService healthProfileService;

    @Operation(summary = "건강 프로필 조회", description = "사용자의 건강 프로필 조회")
    @GetMapping
    public ResponseEntity<HealthProfileResponse> getHealthProfile(
            @AuthenticationPrincipal UserIdPrincipal principal
    ) {
        Long userId = resolveUserId(principal);
        HealthProfileResponse response = healthProfileService.getHealthProfile(userId);
        return ResponseEntity.ok(response);
    }

    @Operation(summary = "건강 프로필 업데이트", description = "사용자의 건강 프로필 부분 업데이트")
    @PatchMapping
    public ResponseEntity<HealthProfileResponse> updateHealthProfile(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody UpdateHealthProfileRequest request
    ) {
        Long userId = resolveUserId(principal);
        HealthProfileResponse response = healthProfileService.updateHealthProfile(userId, request);
        return ResponseEntity.ok(response);
    }

    private Long resolveUserId(UserIdPrincipal principal) {
        if (principal == null) {
            throw new CustomException(ErrorCode.UNAUTHORIZED);
        }
        return principal.getUserId();
    }
}