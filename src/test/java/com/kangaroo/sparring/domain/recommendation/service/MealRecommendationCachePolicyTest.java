package com.kangaroo.sparring.domain.recommendation.service;

import com.kangaroo.sparring.domain.common.type.MealTime;
import com.kangaroo.sparring.domain.recommendation.entity.MealRecommendation;
import com.kangaroo.sparring.domain.record.common.RecordReadService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MealRecommendationCachePolicyTest {

    @Mock
    private RecordReadService recordReadService;

    @InjectMocks
    private MealRecommendationCachePolicy cachePolicy;

    @Test
    void 당일이_아닌_캐시는_만료로_판단한다() {
        MealRecommendation cached = recommendationAt(LocalDateTime.of(2026, 4, 8, 10, 0));

        MealRecommendationCachePolicy.InvalidationReason reason = cachePolicy.resolve(
                1L,
                null,
                MealTime.LUNCH,
                LocalDate.of(2026, 4, 9),
                LocalDateTime.of(2026, 4, 9, 12, 0),
                cached
        );

        assertThat(reason).isEqualTo(MealRecommendationCachePolicy.InvalidationReason.EXPIRED_DATE);
    }

    @Test
    void 남은_끼니는_혈당혈압_변경시_무효화한다() {
        MealRecommendation cached = recommendationAt(LocalDateTime.of(2026, 4, 9, 8, 0));
        when(recordReadService.hasFoodInputChangedSince(anyLong(), any(), any())).thenReturn(false);
        when(recordReadService.hasBloodSugarInputChangedSince(1L, cached.getRecommendedAt())).thenReturn(true);

        MealRecommendationCachePolicy.InvalidationReason reason = cachePolicy.resolve(
                1L,
                null,
                MealTime.DINNER,
                LocalDate.of(2026, 4, 9),
                LocalDateTime.of(2026, 4, 9, 12, 0),
                cached
        );

        assertThat(reason).isEqualTo(MealRecommendationCachePolicy.InvalidationReason.VITALS_INPUT_CHANGED);
        verify(recordReadService).hasBloodSugarInputChangedSince(1L, cached.getRecommendedAt());
    }

    @Test
    void 지난_끼니는_혈당혈압_변경으로_무효화하지_않는다() {
        MealRecommendation cached = recommendationAt(LocalDateTime.of(2026, 4, 9, 8, 0));
        when(recordReadService.hasFoodInputChangedSince(anyLong(), any(), any())).thenReturn(false);

        MealRecommendationCachePolicy.InvalidationReason reason = cachePolicy.resolve(
                1L,
                null,
                MealTime.BREAKFAST,
                LocalDate.of(2026, 4, 9),
                LocalDateTime.of(2026, 4, 9, 12, 30),
                cached
        );

        assertThat(reason).isEqualTo(MealRecommendationCachePolicy.InvalidationReason.NONE);
        verify(recordReadService, never()).hasBloodSugarInputChangedSince(anyLong(), any());
        verify(recordReadService, never()).hasBloodPressureInputChangedSince(anyLong(), any());
    }

    private MealRecommendation recommendationAt(LocalDateTime recommendedAt) {
        return MealRecommendation.builder()
                .mealType("LUNCH")
                .recommendedAt(recommendedAt)
                .build();
    }
}
