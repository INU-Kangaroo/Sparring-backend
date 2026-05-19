package com.kangaroo.sparring.domain.prediction.service;

import com.kangaroo.sparring.domain.prediction.client.MlServerClient;
import com.kangaroo.sparring.domain.prediction.dto.req.GlucosePredictionRequest;
import com.kangaroo.sparring.domain.prediction.dto.res.GlucosePredictionResponse;
import com.kangaroo.sparring.domain.record.common.BloodSugarRecord;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.type.Gender;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class GlucosePredictionServiceTest {

    @Mock
    private RecordReadService recordReadService;
    @Mock
    private MlServerClient mlServerClient;

    @InjectMocks
    private GlucosePredictionService glucosePredictionService;

    @Test
    void meal_입력으로_혈당_예측을_수행한다() {
        User user = User.builder()
                .id(1L)
                .username("홍길동")
                .email("test@test.com")
                .password("pw")
                .gender(Gender.FEMALE)
                .build();

        GlucosePredictionRequest request = new GlucosePredictionRequest();
        GlucosePredictionRequest.Meal meal = new GlucosePredictionRequest.Meal();
        ReflectionTestUtils.setField(meal, "carbs", 42.0);
        ReflectionTestUtils.setField(meal, "protein", 24.0);
        ReflectionTestUtils.setField(meal, "fat", 10.0);
        ReflectionTestUtils.setField(meal, "fiber", 6.0);
        ReflectionTestUtils.setField(meal, "kcal", 410.0);
        ReflectionTestUtils.setField(meal, "mealType", "lunch");
        ReflectionTestUtils.setField(request, "meal", meal);

        when(recordReadService.getRecentBloodSugarRecords(1L, 3)).thenReturn(List.of(
                new BloodSugarRecord(90, LocalDateTime.of(2026, 4, 9, 7, 0), "공복"),
                new BloodSugarRecord(95, LocalDateTime.of(2026, 4, 9, 8, 0), "공복")
        ));
        when(mlServerClient.predictGlucose(anyDouble(), anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(MlServerClient.PredictionResult.builder()
                        .peakDelta(34.0)
                        .peakMinute(75)
                        .curve(List.of(
                                MlServerClient.CurveDeltaPoint.builder().minute(0).delta(0.0).build(),
                                MlServerClient.CurveDeltaPoint.builder().minute(30).delta(6.1).build()
                        ))
                        .build());

        GlucosePredictionResponse response = glucosePredictionService.predictGlucose(user, request);

        ArgumentCaptor<Double> baselineCaptor = ArgumentCaptor.forClass(Double.class);
        ArgumentCaptor<String> sexCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<MlServerClient.MealPayload> mealCaptor = ArgumentCaptor.forClass(MlServerClient.MealPayload.class);

        verify(mlServerClient).predictGlucose(
                baselineCaptor.capture(),
                sexCaptor.capture(),
                mealCaptor.capture()
        );

        assertThat(baselineCaptor.getValue()).isEqualTo(95.0);
        assertThat(sexCaptor.getValue()).isEqualTo("F");
        assertThat(mealCaptor.getValue().getMealType()).isEqualTo("lunch");
        assertThat(mealCaptor.getValue().getKcal()).isEqualTo(410.0);

        assertThat(response.getPeakGlucose()).isEqualTo(129.0);
        assertThat(response.getPeakMinute()).isEqualTo(75);
        assertThat(response.getCurve()).hasSize(2);
        assertThat(response.getCurve().get(0).getMinute()).isEqualTo(0);
        assertThat(response.getCurve().get(0).getGlucose()).isEqualTo(95.0);
        assertThat(response.getCurve().get(1).getMinute()).isEqualTo(30);
        assertThat(response.getCurve().get(1).getGlucose()).isEqualTo(101.1);
    }
}
