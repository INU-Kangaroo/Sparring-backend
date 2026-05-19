package com.kangaroo.sparring.domain.prediction.service;

import com.kangaroo.sparring.domain.prediction.client.MlServerClient;
import com.kangaroo.sparring.domain.prediction.dto.req.GlucosePredictionRequest;
import com.kangaroo.sparring.domain.prediction.dto.res.GlucosePredictionResponse;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class GlucosePredictionService {

    private static final int GLUCOSE_HISTORY_COUNT = 3;

    private final RecordReadService recordReadService;
    private final MlServerClient mlServerClient;

    public GlucosePredictionResponse predictGlucose(User user, GlucosePredictionRequest request) {
        long startedAt = System.currentTimeMillis();
        List<BloodSugarRecord> recentLogs = recordReadService.getRecentBloodSugarRecords(user.getId(), GLUCOSE_HISTORY_COUNT);
        if (recentLogs.isEmpty()) {
            log.warn("혈당 예측 실패: userId={}, reason=insufficient_data", user.getId());
            throw new CustomException(ErrorCode.INSUFFICIENT_DATA_FOR_PREDICTION);
        }

        BloodSugarRecord latest = recentLogs.stream()
                .max(Comparator.comparing(BloodSugarRecord::getMeasurementTime))
                .orElseThrow(() -> new CustomException(ErrorCode.INSUFFICIENT_DATA_FOR_PREDICTION));

        double baselineGlucose = latest.getGlucoseLevel();
        String sex = resolveSex(user.getGender());

        GlucosePredictionRequest.Meal meal = request.getMeal();
        log.info("혈당 예측 요청 시작: userId={}, mealType={}", user.getId(), meal.getMealType());
        MlServerClient.PredictionResult mlResult = mlServerClient.predictGlucose(
                baselineGlucose,
                sex,
                MlServerClient.MealPayload.builder()
                        .carbs(meal.getCarbs())
                        .protein(meal.getProtein())
                        .fat(meal.getFat())
                        .fiber(meal.getFiber())
                        .kcal(meal.getKcal())
                        .mealType(meal.getMealType())
                        .build()
        );

        List<GlucosePredictionResponse.CurvePoint> curve = mlResult.getCurve().stream()
                .map(point -> GlucosePredictionResponse.CurvePoint.builder()
                        .minute(point.getMinute())
                        .glucose(baselineGlucose + point.getDelta())
                        .build())
                .toList();

        GlucosePredictionResponse response = GlucosePredictionResponse.builder()
                .peakGlucose(baselineGlucose + mlResult.getPeakDelta())
                .peakMinute(mlResult.getPeakMinute())
                .curve(curve)
                .build();

        log.info("혈당 예측 요청 완료: userId={}, peakMinute={}, curveSize={}, elapsedMs={}",
                user.getId(), mlResult.getPeakMinute(), curve.size(), System.currentTimeMillis() - startedAt);
        return response;
    }

    private String resolveSex(Gender gender) {
        if (gender == Gender.MALE) {
            return "M";
        }
        if (gender == Gender.FEMALE) {
            return "F";
        }
        throw new CustomException(ErrorCode.INVALID_INPUT, "성별 정보가 올바르지 않습니다.");
    }
}
