package com.kangaroo.sparring.domain.prediction.controller;

import com.kangaroo.sparring.domain.healthprofile.service.HealthProfileGuardService;
import com.kangaroo.sparring.domain.prediction.dto.req.GlucosePredictionRequest;
import com.kangaroo.sparring.domain.prediction.dto.res.GlucosePredictionResponse;
import com.kangaroo.sparring.domain.prediction.service.GlucosePredictionService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.repository.UserRepository;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import com.kangaroo.sparring.global.security.principal.PrincipalResolver;
import com.kangaroo.sparring.global.security.principal.UserIdPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "혈당 예측", description = "혈당 예측 API")
@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class GlucosePredictionController {

    private final GlucosePredictionService glucosePredictionService;
    private final UserRepository userRepository;
    private final HealthProfileGuardService healthProfileGuardService;

    @Operation(summary = "식후 혈당 예측", description = "영양성분(meal) 입력 기반 식후 혈당 곡선 예측")
    @ApiResponses({
            @ApiResponse(
                    responseCode = "200",
                    description = "OK",
                    content = @Content(
                            mediaType = "application/json",
                            examples = @ExampleObject(
                                    name = "Success",
                                    value = """
                                            {
                                              "peakGlucose": 129.0,
                                              "peakMinute": 75,
                                              "curve": [
                                                {"minute": 0, "glucose": 95.0},
                                                {"minute": 30, "glucose": 101.1},
                                                {"minute": 60, "glucose": 121.0},
                                                {"minute": 75, "glucose": 129.0},
                                                {"minute": 120, "glucose": 118.4}
                                              ]
                                            }
                                            """
                            )
                    )
            )
    })
    @PostMapping("/blood-sugar")
    public ResponseEntity<GlucosePredictionResponse> predictBloodSugar(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody GlucosePredictionRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        healthProfileGuardService.ensureProfileComplete(userId);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        GlucosePredictionResponse response = glucosePredictionService.predictGlucose(user, request);
        return ResponseEntity.ok(response);
    }
}
