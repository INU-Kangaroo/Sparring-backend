package com.kangaroo.sparring.domain.prediction.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFunction;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class MlServerClientPathTest {

    private MlServerClient mlServerClient;
    private AtomicReference<String> capturedUrl;

    @BeforeEach
    void setUp() {
        capturedUrl = new AtomicReference<>();

        ExchangeFunction exchangeFunction = request -> {
            capturedUrl.set(request.url().toString());
            return Mono.just(okResponse());
        };

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        mlServerClient = new MlServerClient(builder, new ObjectMapper());

        ReflectionTestUtils.setField(mlServerClient, "mlServerUrl", "http://127.0.0.1:9001");
        ReflectionTestUtils.setField(mlServerClient, "predictPath", "/api/v1/predict-glucose");
    }

    @Test
    void 예측_요청은_설정된_predict_path를_사용한다() {
        mlServerClient.predictGlucose(
                95.0,
                "F",
                MlServerClient.MealPayload.builder()
                        .carbs(42.0)
                        .protein(24.0)
                        .fat(10.0)
                        .fiber(6.0)
                        .kcal(410.0)
                        .mealType("lunch")
                        .build()
        );

        assertThat(capturedUrl.get()).isEqualTo("http://127.0.0.1:9001/api/v1/predict-glucose");
    }

    @Test
    void 구형_forecast_응답도_파싱할_수_있다() {
        ExchangeFunction exchangeFunction = request -> Mono.just(ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body("""
                        {
                          "peakGlucose": 129.0,
                          "peakOffsetMinutes": 75,
                          "forecast": [
                            {"offsetMinutes": 0, "predictedGlucose": 95.0},
                            {"offsetMinutes": 30, "predictedGlucose": 101.1}
                          ]
                        }
                        """)
                .build());

        WebClient.Builder builder = WebClient.builder().exchangeFunction(exchangeFunction);
        MlServerClient client = new MlServerClient(builder, new ObjectMapper());
        ReflectionTestUtils.setField(client, "mlServerUrl", "http://127.0.0.1:9001");
        ReflectionTestUtils.setField(client, "predictPath", "/api/v1/predict-glucose");

        MlServerClient.PredictionResult result = client.predictGlucose(
                95.0,
                "F",
                MlServerClient.MealPayload.builder()
                        .carbs(42.0)
                        .protein(24.0)
                        .fat(10.0)
                        .fiber(6.0)
                        .kcal(410.0)
                        .mealType("lunch")
                        .build()
        );

        assertThat(result.getPeakDelta()).isEqualTo(34.0);
        assertThat(result.getPeakMinute()).isEqualTo(75);
        assertThat(result.getCurve()).hasSize(2);
        assertThat(result.getCurve().get(1).getMinute()).isEqualTo(30);
        assertThat(result.getCurve().get(1).getDelta()).isCloseTo(6.1, org.assertj.core.data.Offset.offset(0.000001));
    }

    private ClientResponse okResponse() {
        String body = """
                {
                  "peakDelta": 34.0,
                  "peakMinute": 75,
                  "curve": [
                    {"minute":0,"delta":0.0},
                    {"minute":30,"delta":6.1}
                  ]
                }
                """;
        return ClientResponse.create(HttpStatus.OK)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .body(body)
                .build();
    }
}
