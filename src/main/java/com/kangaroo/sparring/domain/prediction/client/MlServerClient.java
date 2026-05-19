package com.kangaroo.sparring.domain.prediction.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MlServerClient {

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ml.prediction.url}")
    private String mlServerUrl;

    @Value("${ml.prediction.path:/predict-glucose}")
    private String predictPath;

    public PredictionResult predictGlucose(
            Double baselineGlucose,
            String sex,
            MealPayload meal
    ) {
        long startedAt = System.currentTimeMillis();
        String endpoint = mlServerUrl + predictPath;
        log.info("ML 혈당 예측 호출 시작: endpoint={}, mealType={}", endpoint, meal.getMealType());

        try {
            ObjectNode root = objectMapper.createObjectNode();
            root.put("baselineGlucose", baselineGlucose);
            root.put("sex", sex);

            ObjectNode mealNode = root.putObject("meal");
            mealNode.put("carbs", meal.getCarbs());
            mealNode.put("protein", meal.getProtein());
            mealNode.put("fat", meal.getFat());
            mealNode.put("fiber", meal.getFiber());
            mealNode.put("kcal", meal.getKcal());
            mealNode.put("mealType", meal.getMealType());

            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(mlServerUrl + predictPath)
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(root.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode response = objectMapper.readTree(responseBody);
            List<CurveDeltaPoint> curve = parseCurve(response, baselineGlucose);
            Double peakDelta = readOptionalDouble(response, "peakDelta", "peak_delta");
            Integer peakMinute = readOptionalInt(response, "peakMinute", "peak_minute");

            if (peakDelta == null) {
                Double peakGlucose = readOptionalDouble(response, "peakGlucose", "peak_glucose");
                if (peakGlucose != null) {
                    peakDelta = peakGlucose - baselineGlucose;
                }
            }
            if (peakMinute == null) {
                peakMinute = readOptionalInt(response, "peakOffsetMinutes", "peak_offset_minutes");
            }
            if (peakDelta == null || peakMinute == null) {
                throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "peak 응답 필드가 누락되었습니다.");
            }

            PredictionResult result = PredictionResult.builder()
                    .peakDelta(peakDelta)
                    .peakMinute(peakMinute)
                    .curve(curve)
                    .build();
            log.info("ML 혈당 예측 호출 성공: endpoint={}, elapsedMs={}, curveSize={}, peakMinute={}",
                    endpoint, System.currentTimeMillis() - startedAt, curve.size(), peakMinute);
            return result;
        } catch (WebClientResponseException e) {
            log.error("ML 서버 혈당 예측 HTTP 오류 endpoint={}, status={}, elapsedMs={}, body={}",
                    endpoint, e.getStatusCode(), System.currentTimeMillis() - startedAt, e.getResponseBodyAsString(), e);
            throw new CustomException(
                    ErrorCode.AI_PREDICTION_FAILED,
                    "ML 서버 응답 오류(" + e.getStatusCode().value() + "): " + e.getResponseBodyAsString()
            );
        } catch (Exception e) {
            log.error("ML 서버 혈당 예측 호출 실패 endpoint={}, elapsedMs={}",
                    endpoint, System.currentTimeMillis() - startedAt, e);
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, e.getMessage());
        }
    }

    private List<CurveDeltaPoint> parseCurve(JsonNode response, Double baselineGlucose) {
        JsonNode curveNode = response.get("curve");
        if (curveNode == null || !curveNode.isArray() || curveNode.isEmpty()) {
            return parseForecastFallback(response, baselineGlucose);
        }

        List<CurveDeltaPoint> points = new ArrayList<>();
        for (JsonNode point : curveNode) {
            points.add(CurveDeltaPoint.builder()
                    .minute(readInt(point, "minute", "minute"))
                    .delta(readDouble(point, "delta", "delta"))
                    .build());
        }

        points.sort(Comparator.comparingInt(CurveDeltaPoint::getMinute));
        return points;
    }

    private List<CurveDeltaPoint> parseForecastFallback(JsonNode response, Double baselineGlucose) {
        JsonNode forecastNode = response.get("forecast");
        if (forecastNode == null || !forecastNode.isArray() || forecastNode.isEmpty()) {
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "curve/forecast 응답이 비어 있습니다.");
        }

        List<CurveDeltaPoint> points = new ArrayList<>();
        for (JsonNode point : forecastNode) {
            Integer minute = readOptionalInt(point, "offsetMinutes", "offset_minutes");
            Double predictedGlucose = readOptionalDouble(point, "predictedGlucose", "predicted_glucose");
            if (minute == null || predictedGlucose == null) {
                continue;
            }
            points.add(CurveDeltaPoint.builder()
                    .minute(minute)
                    .delta(predictedGlucose - baselineGlucose)
                    .build());
        }

        if (points.isEmpty()) {
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "forecast 포맷에서 유효한 포인트를 찾지 못했습니다.");
        }
        points.sort(Comparator.comparingInt(CurveDeltaPoint::getMinute));
        return points;
    }

    private int readInt(JsonNode node, String camelCaseField, String snakeCaseField) {
        JsonNode target = node.get(camelCaseField);
        if (target == null) {
            target = node.get(snakeCaseField);
        }
        if (target == null || target.isNull()) {
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "예측 응답 필드 누락: " + camelCaseField);
        }
        return target.asInt();
    }

    private double readDouble(JsonNode node, String camelCaseField, String snakeCaseField) {
        JsonNode target = node.get(camelCaseField);
        if (target == null) {
            target = node.get(snakeCaseField);
        }
        if (target == null || target.isNull()) {
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "예측 응답 필드 누락: " + camelCaseField);
        }
        return target.asDouble();
    }

    private Integer readOptionalInt(JsonNode node, String camelCaseField, String snakeCaseField) {
        JsonNode target = node.get(camelCaseField);
        if (target == null) {
            target = node.get(snakeCaseField);
        }
        if (target == null || target.isNull()) {
            return null;
        }
        return target.asInt();
    }

    private Double readOptionalDouble(JsonNode node, String camelCaseField, String snakeCaseField) {
        JsonNode target = node.get(camelCaseField);
        if (target == null) {
            target = node.get(snakeCaseField);
        }
        if (target == null || target.isNull()) {
            return null;
        }
        return target.asDouble();
    }

    @Getter
    @Builder
    public static class MealPayload {
        private Double carbs;
        private Double protein;
        private Double fat;
        private Double fiber;
        private Double kcal;
        private String mealType;
    }

    @Getter
    @Builder
    public static class CurveDeltaPoint {
        private Integer minute;
        private Double delta;
    }

    @Getter
    @Builder
    public static class PredictionResult {
        private Double peakDelta;
        private Integer peakMinute;
        private List<CurveDeltaPoint> curve;
    }
}
