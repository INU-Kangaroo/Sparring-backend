package com.kangaroo.sparring.domain.insight.today.service;

import com.kangaroo.sparring.domain.insight.today.dto.res.TodayInsightResponse;
import com.kangaroo.sparring.domain.insight.today.service.InsightContextBuilder.InsightContext;
import com.kangaroo.sparring.domain.insight.today.type.InsightType;
import com.kangaroo.sparring.domain.insight.today.type.MealTimeSlot;
import com.kangaroo.sparring.domain.recommendation.service.GeminiApiClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
@Slf4j
public class InsightService {

    private final InsightContextBuilder insightContextBuilder;
    private final InsightPromptSupport insightPromptSupport;
    private final InsightCacheService insightCacheService;
    private final GeminiApiClient geminiApiClient;

    public TodayInsightResponse getTodayInsight(Long userId) {
        LocalTime now = LocalTime.now();
        MealTimeSlot slot = MealTimeSlot.from(now);

        String cacheKey = insightCacheService.buildKey(
                userId,
                LocalDate.now().toString(),
                slot.cacheKey()
        );

        // 캐시 히트
        Optional<String> cached = insightCacheService.get(cacheKey);
        if (cached.isPresent()) {
            log.debug("인사이트 캐시 히트: key={}", cacheKey);
            // 캐시에는 "type|message" 형태로 저장
            return parseFromCache(cached.get());
        }

        // 캐시 미스 → 생성
        InsightContext context = insightContextBuilder.build(userId);
        String prompt = insightPromptSupport.build(context, slot);

        String message;
        try {
            message = geminiApiClient.generateContent(prompt).strip();
        } catch (Exception e) {
            log.warn("Gemini 인사이트 생성 실패, fallback 메시지 사용: userId={}", userId, e);
            message = getFallbackMessage(context.getType());
        }

        // 캐시 저장 (TTL: 다음 슬롯까지)
        long ttl = slot.secondsUntilNext(now);
        String cacheValue = context.getType().name() + "|" + message;
        insightCacheService.set(cacheKey, cacheValue, ttl);

        return TodayInsightResponse.of(context.getType(), message);
    }

    private TodayInsightResponse parseFromCache(String cacheValue) {
        String[] parts = cacheValue.split("\\|", 2);
        if (parts.length == 2) {
            try {
                InsightType type = InsightType.valueOf(parts[0]);
                return TodayInsightResponse.of(type, parts[1]);
            } catch (IllegalArgumentException e) {
                log.warn("인사이트 캐시 타입 파싱 실패: value={}", cacheValue, e);
                return TodayInsightResponse.of(InsightType.NEEDS_MONITORING, parts[1]);
            }
        }
        // 파싱 실패 시 전체를 메시지로
        return TodayInsightResponse.of(InsightType.NEEDS_MONITORING, cacheValue);
    }

    private String getFallbackMessage(InsightType type) {
        return switch (type) {
            case BLOOD_SUGAR_STABLE -> "공복 혈당이 안정적으로 유지되고 있어요! 오늘도 잘 관리해보세요 👏";
            case BLOOD_SUGAR_HIGH -> "혈당이 조금 높은 편이에요. 식사 후 가벼운 산책을 해보는 건 어떨까요? 🚶";
            case BLOOD_SUGAR_LOW -> "저혈당에 주의하세요. 규칙적인 식사가 중요합니다 🍱";
            case BLOOD_PRESSURE_STABLE -> "혈압이 안정적이에요. 오늘도 좋은 컨디션 유지하세요 💪";
            case BLOOD_PRESSURE_HIGH -> "혈압이 다소 높아요. 충분한 수분 섭취와 휴식을 권장드려요 💧";
            case BOTH_STABLE -> "혈당과 혈압 모두 잘 관리되고 있어요. 정말 잘하고 있습니다! 🌟";
            case NEEDS_MONITORING -> "측정 데이터는 잘 쌓이고 있어요. 지금처럼 기록을 이어가면 더 정확한 인사이트를 드릴 수 있어요 📈";
            case NO_DATA -> "오늘 첫 건강 기록을 남겨보세요. 작은 시작이 큰 변화를 만들어요 🌱";
        };
    }
}
