package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.healthprofile.entity.HealthProfile;
import com.kangaroo.sparring.domain.recommendation.dto.ml.MealRecommendationMlRequest;
import com.kangaroo.sparring.domain.recommendation.dto.res.MealRecommendationResponse;
import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendation;
import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendationItem;
import com.kangaroo.sparring.domain.recommendation.repository.MealRecommendationRepository;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import com.kangaroo.sparring.domain.user.entity.User;
import com.kangaroo.sparring.domain.user.type.Gender;
import com.kangaroo.sparring.global.exception.CustomException;
import com.kangaroo.sparring.global.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class MealRecommendationServiceTest {

    @Mock
    private RecordReadService recordReadService;
    @Mock
    private MealRecommendationAiClient aiClient;
    @Mock
    private MealRecommendationMlRequestFactory mlRequestFactory;
    @Mock
    private MealRecommendationRepository mealRecommendationRepository;
    @Mock
    private MealRecommendationCachePolicy cachePolicy;
    @Mock
    private Clock kstClock;

    @InjectMocks
    private MealRecommendationService mealRecommendationService;

    @Test
    void refresh_요청시_생성된_요청DTO를_AI클라이언트로_전달한다() {
        User user = User.builder().id(1L).build();
        HealthProfile profile = HealthProfile.builder()
                .gender(Gender.MALE)
                .build();
        MealRecommendationMlRequest mlRequest = sampleMlRequest(user.getId(), "LUNCH", "MALE");

        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-09T00:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(recordReadService.getRecentBloodSugarRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentBloodPressureRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentFoodRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(mlRequestFactory.create(any(), any(), any(), any(), any(), any(), anyBoolean())).thenReturn(mlRequest);
        when(aiClient.recommend(any())).thenReturn(new MealRecommendationAiClient.AiRecommendResult(
                "LUNCH",
                500.0,
                List.of(validCard())
        ));
        when(mealRecommendationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MealRecommendationResponse response = mealRecommendationService.refresh(user, profile, MealTime.LUNCH);

        ArgumentCaptor<MealRecommendationMlRequest> requestCaptor = ArgumentCaptor.forClass(MealRecommendationMlRequest.class);
        verify(aiClient).recommend(requestCaptor.capture());
        assertThat(requestCaptor.getValue()).isEqualTo(mlRequest);
        assertThat(response.getMealType()).isEqualTo("LUNCH");
        assertThat(response.getRecommendations()).hasSize(1);
    }

    @Test
    void refresh_강제변화옵션_true면_factory에_true를_전달한다() {
        User user = User.builder().id(1L).build();
        HealthProfile profile = HealthProfile.builder()
                .gender(Gender.MALE)
                .build();

        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-09T00:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(recordReadService.getRecentBloodSugarRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentBloodPressureRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentFoodRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(mlRequestFactory.create(any(), any(), any(), any(), any(), any(), eq(true)))
                .thenReturn(sampleMlRequest(user.getId(), "LUNCH", "MALE"));
        when(aiClient.recommend(any())).thenReturn(new MealRecommendationAiClient.AiRecommendResult(
                "LUNCH",
                500.0,
                List.of(validCard())
        ));
        when(mealRecommendationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MealRecommendationResponse response = mealRecommendationService.refresh(user, profile, MealTime.LUNCH, true);

        verify(mlRequestFactory).create(any(), any(), any(), any(), any(), any(), eq(true));
        assertThat(response.getRecommendations()).hasSize(1);
    }

    @Test
    void refresh_요청시_성별이_없으면_예외가_발생한다() {
        User user = User.builder().id(1L).build();
        HealthProfile profile = HealthProfile.builder()
                .gender(null)
                .build();

        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-09T00:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(recordReadService.getRecentBloodSugarRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentBloodPressureRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentFoodRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(mlRequestFactory.create(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenThrow(new CustomException(ErrorCode.INVALID_INPUT, "성별 정보가 필요합니다."));
        when(mealRecommendationRepository.findTopByUser_IdAndMealTypeOrderByRecommendedAtDesc(anyLong(), anyString()))
                .thenReturn(Optional.empty());

        assertThatThrownBy(() -> mealRecommendationService.recommend(user, profile, MealTime.LUNCH))
                .isInstanceOf(CustomException.class);

        verify(aiClient, never()).recommend(any());
    }

    @Test
    void refresh_요청시_추천카드가_비어있으면_예외가_발생한다() {
        User user = User.builder().id(1L).build();
        HealthProfile profile = HealthProfile.builder()
                .gender(Gender.FEMALE)
                .build();

        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-09T00:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(recordReadService.getRecentBloodSugarRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentBloodPressureRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentFoodRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(mlRequestFactory.create(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(sampleMlRequest(user.getId(), "LUNCH", "FEMALE"));
        when(aiClient.recommend(any())).thenReturn(new MealRecommendationAiClient.AiRecommendResult(
                "LUNCH",
                500.0,
                List.of()
        ));

        assertThatThrownBy(() -> mealRecommendationService.refresh(user, profile, MealTime.LUNCH))
                .isInstanceOf(CustomException.class);
    }

    @Test
    void recommend_요청시_캐시_변환_실패면_refresh로_재생성한다() {
        User user = User.builder().id(1L).build();
        HealthProfile profile = HealthProfile.builder()
                .gender(Gender.FEMALE)
                .build();

        MealRecommendation brokenCache = brokenCachedRecommendation(user);
        when(mealRecommendationRepository.findTopByUser_IdAndMealTypeOrderByRecommendedAtDesc(1L, "LUNCH"))
                .thenReturn(Optional.of(brokenCache));
        when(cachePolicy.resolve(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(MealRecommendationCachePolicy.InvalidationReason.NONE);

        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-09T00:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(recordReadService.getRecentBloodSugarRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentBloodPressureRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentFoodRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(mlRequestFactory.create(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(sampleMlRequest(user.getId(), "LUNCH", "FEMALE"));
        when(aiClient.recommend(any())).thenReturn(new MealRecommendationAiClient.AiRecommendResult(
                "LUNCH",
                500.0,
                List.of(validCard())
        ));
        when(mealRecommendationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MealRecommendationResponse response = mealRecommendationService.recommend(user, profile, MealTime.LUNCH);

        verify(aiClient, times(1)).recommend(any());
        assertThat(response.getRecommendations()).hasSize(1);
    }

    @Test
    void recommend_요청시_당일_신규로그가_있으면_캐시를_무효화하고_refresh한다() {
        User user = User.builder().id(1L).build();
        HealthProfile profile = HealthProfile.builder()
                .gender(Gender.FEMALE)
                .build();

        MealRecommendation cached = validCachedRecommendation(user);
        when(mealRecommendationRepository.findTopByUser_IdAndMealTypeOrderByRecommendedAtDesc(1L, "LUNCH"))
                .thenReturn(Optional.of(cached));
        when(cachePolicy.resolve(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(MealRecommendationCachePolicy.InvalidationReason.VITALS_INPUT_CHANGED);

        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-09T00:00:00Z"));
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(recordReadService.getRecentBloodSugarRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentBloodPressureRecords(anyLong(), anyInt())).thenReturn(List.of());
        when(recordReadService.getRecentFoodRecords(anyLong(), any(), any())).thenReturn(List.of());
        when(mlRequestFactory.create(any(), any(), any(), any(), any(), any(), anyBoolean()))
                .thenReturn(sampleMlRequest(user.getId(), "LUNCH", "FEMALE"));
        when(aiClient.recommend(any())).thenReturn(new MealRecommendationAiClient.AiRecommendResult(
                "LUNCH",
                500.0,
                List.of(validCard())
        ));
        when(mealRecommendationRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        MealRecommendationResponse response = mealRecommendationService.recommend(user, profile, MealTime.LUNCH);

        verify(aiClient, times(1)).recommend(any());
        assertThat(response.getRecommendations()).hasSize(1);
    }

    @Test
    void recommend_요청시_지나간_끼니는_혈당혈압_변경으로_무효화하지_않는다() {
        User user = User.builder().id(1L).build();
        HealthProfile profile = HealthProfile.builder()
                .gender(Gender.FEMALE)
                .build();

        MealRecommendation cached = validCachedRecommendation(user, "BREAKFAST", LocalDateTime.of(2026, 4, 9, 8, 0));
        when(mealRecommendationRepository.findTopByUser_IdAndMealTypeOrderByRecommendedAtDesc(1L, "BREAKFAST"))
                .thenReturn(Optional.of(cached));

        when(kstClock.instant()).thenReturn(Instant.parse("2026-04-09T03:30:00Z")); // 12:30 KST
        when(kstClock.getZone()).thenReturn(ZoneId.of("Asia/Seoul"));
        when(cachePolicy.resolve(anyLong(), any(), any(), any(), any(), any()))
                .thenReturn(MealRecommendationCachePolicy.InvalidationReason.NONE);

        MealRecommendationResponse response = mealRecommendationService.recommend(user, profile, MealTime.BREAKFAST);

        verify(recordReadService, never()).hasBloodSugarInputChangedSince(anyLong(), any());
        verify(recordReadService, never()).hasBloodPressureInputChangedSince(anyLong(), any());
        verify(aiClient, never()).recommend(any());
        assertThat(response.getRecommendations()).hasSize(1);
    }

    private MealRecommendationResponse.RecommendationCardDto validCard() {
        return MealRecommendationResponse.RecommendationCardDto.builder()
                .rank(1)
                .title("세트1")
                .nutrients(MealRecommendationResponse.NutrientsDto.builder()
                        .kcal(500.0)
                        .carbs(50.0)
                        .protein(30.0)
                        .fat(10.0)
                        .sodium(600.0)
                        .build())
                .reasons(List.of("이유"))
                .menus(List.of(
                        MealRecommendationResponse.MenuItemDto.builder()
                                .id(1L)
                                .name("현미밥")
                                .kcal(200.0)
                                .carbs(40.0)
                                .protein(4.0)
                                .fat(2.0)
                                .sodium(20.0)
                                .build()
                ))
                .build();
    }

    private MealRecommendationMlRequest sampleMlRequest(Long userId, String mealType, String sex) {
        return new MealRecommendationMlRequest(
                "req-test",
                "rec-v4",
                userId,
                mealType,
                4,
                new MealRecommendationMlRequest.HealthProfile(sex, 30, 170.0, 65.0,
                        "MODERATE", false, false),
                null,
                new MealRecommendationMlRequest.Nutrition(
                        new MealRecommendationMlRequest.ConsumedToday(0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
                ),
                new MealRecommendationMlRequest.Preferences(List.of(), List.of(), List.of(), List.of()),
                List.of(),
                null
        );
    }

    private MealRecommendation brokenCachedRecommendation(User user) {
        MealRecommendation rec = MealRecommendation.builder()
                .user(user)
                .mealType("LUNCH")
                .mealTargetKcal(BigDecimal.valueOf(500))
                .recommendedAt(LocalDateTime.of(2026, 4, 9, 10, 0))
                .items(new java.util.ArrayList<>())
                .build();

        MealRecommendationItem item = MealRecommendationItem.builder()
                .mealRecommendation(rec)
                .rankOrder(1)
                .title("깨진카드")
                .totalKcal(BigDecimal.valueOf(500))
                .totalCarbs(BigDecimal.valueOf(50))
                .totalProtein(BigDecimal.valueOf(30))
                .totalFat(BigDecimal.valueOf(10))
                .totalSodium(BigDecimal.valueOf(600))
                .reasonsJson("not-json")
                .menusJson("not-json")
                .build();
        rec.getItems().add(item);
        return rec;
    }

    private MealRecommendation validCachedRecommendation(User user) {
        return validCachedRecommendation(user, "LUNCH", LocalDateTime.of(2026, 4, 9, 10, 0));
    }

    private MealRecommendation validCachedRecommendation(User user, String mealType, LocalDateTime recommendedAt) {
        MealRecommendation rec = MealRecommendation.builder()
                .user(user)
                .mealType(mealType)
                .mealTargetKcal(BigDecimal.valueOf(500))
                .recommendedAt(recommendedAt)
                .items(new java.util.ArrayList<>())
                .build();

        MealRecommendationItem item = MealRecommendationItem.builder()
                .mealRecommendation(rec)
                .rankOrder(1)
                .title("정상카드")
                .totalKcal(BigDecimal.valueOf(500))
                .totalCarbs(BigDecimal.valueOf(50))
                .totalProtein(BigDecimal.valueOf(30))
                .totalFat(BigDecimal.valueOf(10))
                .totalSodium(BigDecimal.valueOf(600))
                .reasonsJson("[\"이유\"]")
                .menusJson("[{\"id\":1,\"name\":\"현미밥\",\"kcal\":200.0,\"carbs\":40.0,\"protein\":4.0,\"fat\":2.0,\"sodium\":20.0}]")
                .build();
        rec.getItems().add(item);
        return rec;
    }
}
