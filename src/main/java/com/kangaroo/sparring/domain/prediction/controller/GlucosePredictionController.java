package com.kangaroo.sparring.domain.prediction.controller;

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
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Tag(name = "Prediction", description = "혈당 예측 API")
@RestController
@RequestMapping("/api/predictions")
@RequiredArgsConstructor
public class GlucosePredictionController {

    private final GlucosePredictionService glucosePredictionService;
    private final UserRepository userRepository;

    @Operation(summary = "식후 혈당 예측", description = "음식 섭취 후 시간대별 혈당 변화 예측 (+0/+30/+60/+120분)")
    @PostMapping("/blood-sugar")
    public ResponseEntity<GlucosePredictionResponse> predictBloodSugar(
            @AuthenticationPrincipal UserIdPrincipal principal,
            @Valid @RequestBody GlucosePredictionRequest request
    ) {
        Long userId = PrincipalResolver.resolveUserId(principal);
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        GlucosePredictionResponse response = glucosePredictionService.predictGlucose(user, request.getFoodId());
        return ResponseEntity.ok(response);
    }
}
