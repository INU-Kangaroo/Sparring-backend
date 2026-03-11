package com.kangaroo.sparring.domain.prediction.client;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.kangaroo.sparring.domain.prediction.dto.res.GlucosePredictionResponse;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class MlServerClient {

    private static final int HORIZON_MINUTES = 120;
    private static final int STEP_MINUTES = 5;
    private static final int[] FORECAST_OFFSETS = {0, 30, 60, 120};

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    @Value("${ml.server.url}")
    private String mlServerUrl;

    public GlucosePredictionResponse.ForecastPoint[] predictGlucose(
            List<Double> glucoseHistory,
            double carbIntake,
            int mealType,
            double age,
            int sex,
            double weight,
            int caffeine,
            int medication,
            int alcohol,
            double intensity
    ) {
        try {
            ObjectNode requestBody = objectMapper.createObjectNode();
            requestBody.put("timestamp", ZonedDateTime.now(ZoneId.of("Asia/Seoul"))
                    .format(DateTimeFormatter.ISO_OFFSET_DATE_TIME));
            requestBody.set("glucose_history", objectMapper.valueToTree(glucoseHistory));
            requestBody.put("carb_intake", carbIntake);
            requestBody.put("meal_type", mealType);
            requestBody.put("steps", 0);
            requestBody.put("intensity", intensity);
            requestBody.put("age", age);
            requestBody.put("sex", sex);
            requestBody.put("weight", weight);
            requestBody.put("alcohol", alcohol);
            requestBody.put("medication", medication);
            requestBody.put("caffeine", caffeine);
            requestBody.put("is_insulin_user", 0);
            requestBody.put("insulin_bolus", 0);
            requestBody.put("insulin_basal", 0);
            requestBody.put("carb_ratio", 0);
            requestBody.put("insulin_sensitivity", 0);
            requestBody.put("horizon_minutes", HORIZON_MINUTES);
            requestBody.put("step_minutes", STEP_MINUTES);
            requestBody.put("debug", false);

            String responseBody = webClientBuilder.build()
                    .post()
                    .uri(mlServerUrl + "/predict")
                    .contentType(MediaType.APPLICATION_JSON)
                    .bodyValue(requestBody.toString())
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();

            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode forecastNode = root.get("forecast");
            if (forecastNode == null || !forecastNode.isArray() || forecastNode.isEmpty()) {
                throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "forecast 응답이 비어 있습니다.");
            }

            // +0/+30/+60/+120분 포인트만 추출
            List<GlucosePredictionResponse.ForecastPoint> points = new ArrayList<>();
            for (int offset : FORECAST_OFFSETS) {
                for (JsonNode point : forecastNode) {
                    if (point.get("offset_minutes").asInt() == offset) {
                        points.add(GlucosePredictionResponse.ForecastPoint.builder()
                                .offsetMinutes(offset)
                                .predictedGlucose(point.get("predicted_glucose").asDouble())
                                .build());
                        break;
                    }
                }
            }

            if (points.isEmpty()) {
                throw new CustomException(ErrorCode.AI_PREDICTION_FAILED, "필수 forecast offset이 누락되었습니다.");
            }
            return points.toArray(new GlucosePredictionResponse.ForecastPoint[0]);

        } catch (Exception e) {
            log.error("ML 서버 혈당 예측 호출 실패", e);
            throw new CustomException(ErrorCode.AI_PREDICTION_FAILED);
        }
    }
}
